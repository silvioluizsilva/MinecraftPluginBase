package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.api.DatabaseMigration;
import br.net.silvioluizsilva.pluginbase.exception.MigrationException;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executa migrações de consumidores com namespace, bloqueio e histórico central.
 */
public final class ConsumerMigrationRunner {

    private static final int LOCK_TIMEOUT_SECONDS = 30;
    private static final List<Pattern> TABLE_REFERENCES = List.of(
            Pattern.compile("(?i)\\b(?:CREATE|ALTER|DROP|TRUNCATE|RENAME)\\s+TABLE"
                    + "(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+`?([a-zA-Z0-9_]+)`?"),
            Pattern.compile("(?i)\\b(?:INSERT\\s+INTO|DELETE\\s+FROM|FROM|JOIN)\\s+`?([a-zA-Z0-9_]+)`?"),
            Pattern.compile("(?i)^\\s*UPDATE\\s+`?([a-zA-Z0-9_]+)`?"),
            Pattern.compile("(?i)\\bCREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+`?[a-zA-Z0-9_]+`?"
                    + "\\s+ON\\s+`?([a-zA-Z0-9_]+)`?"),
            Pattern.compile("(?i)\\bRENAME\\s+TABLE\\s+`?[a-zA-Z0-9_]+`?\\s+TO\\s+`?([a-zA-Z0-9_]+)`?")
    );
    private static final String CREATE_HISTORY = """
            CREATE TABLE IF NOT EXISTS pluginbase_consumer_schema_history (
                namespace VARCHAR(32) NOT NULL,
                version INT UNSIGNED NOT NULL,
                description VARCHAR(255) NOT NULL,
                checksum CHAR(64) NOT NULL,
                installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                execution_time_ms BIGINT UNSIGNED NOT NULL,
                PRIMARY KEY (namespace, version)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private final PluginLogger logger;

    /**
     * Cria o executor para consumidores.
     *
     * @param logger logger central seguro
     */
    public ConsumerMigrationRunner(PluginLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Aplica as migrações do namespace informado.
     *
     * @param connection conexão transacional
     * @param namespace namespace validado
     * @param migrations catálogo do consumidor
     */
    public void migrate(Connection connection, String namespace, List<DatabaseMigration> migrations) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(namespace, "namespace");
        List<DatabaseMigration> catalog = validateCatalog(namespace, migrations);
        String lockName = "pluginbase_consumer_" + namespace;
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute(CREATE_HISTORY);
            }
            acquireLock(connection, lockName);
            try {
                preventDowngrade(connection, namespace, catalog);
                for (DatabaseMigration migration : catalog) {
                    applyIfPending(connection, namespace, migration);
                }
            } finally {
                releaseLock(connection, lockName);
            }
        } catch (SQLException exception) {
            throw new MigrationException("Falha nas migrações do consumidor " + namespace + ".", exception);
        }
    }

    private List<DatabaseMigration> validateCatalog(String namespace, List<DatabaseMigration> migrations) {
        Objects.requireNonNull(migrations, "migrations");
        List<DatabaseMigration> catalog = List.copyOf(migrations);
        Set<Integer> versions = new HashSet<>();
        int previous = 0;
        for (DatabaseMigration migration : catalog) {
            Objects.requireNonNull(migration, "migration");
            if (!versions.add(migration.version()) || migration.version() <= previous) {
                throw new MigrationException("As migrações do consumidor devem ter versões únicas e crescentes.");
            }
            validateNamespaceReferences(namespace, migration.script());
            previous = migration.version();
        }
        return catalog;
    }

    private void validateNamespaceReferences(String namespace, String script) {
        for (String statement : SqlScriptParser.parse(script)) {
            for (Pattern pattern : TABLE_REFERENCES) {
                Matcher matcher = pattern.matcher(statement);
                while (matcher.find()) {
                    String table = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
                    if (!table.startsWith(namespace + "_")) {
                        throw new MigrationException(
                                "A migração tentou acessar uma tabela fora do namespace " + namespace + "."
                        );
                    }
                }
            }
        }
    }

    private void acquireLock(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new MigrationException("Não foi possível bloquear as migrações do consumidor.");
                }
            }
        }
    }

    private void releaseLock(Connection connection, String lockName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    logger.warn("O bloqueio de migração já não pertencia ao namespace {}.", lockName);
                }
            }
        } catch (SQLException exception) {
            logger.warn("Não foi possível liberar o bloqueio de migração do namespace {}.", lockName);
        }
    }

    private void preventDowngrade(
            Connection connection,
            String namespace,
            List<DatabaseMigration> migrations
    ) throws SQLException {
        int latest = migrations.isEmpty() ? 0 : migrations.getLast().version();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(version), 0) FROM pluginbase_consumer_schema_history WHERE namespace = ?"
        )) {
            statement.setString(1, namespace);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) > latest) {
                    throw new MigrationException("O esquema consumidor foi criado por uma versão mais recente.");
                }
            }
        }
    }

    private void applyIfPending(Connection connection, String namespace, DatabaseMigration migration)
            throws SQLException {
        String checksum = checksum(migration.script());
        String installed = findChecksum(connection, namespace, migration.version());
        if (installed != null) {
            if (!MessageDigest.isEqual(installed.getBytes(StandardCharsets.US_ASCII),
                    checksum.getBytes(StandardCharsets.US_ASCII))) {
                throw new MigrationException("A migração consumidora " + migration.version() + " foi alterada.");
            }
            return;
        }
        long startedAt = System.nanoTime();
        for (String sql : SqlScriptParser.parse(migration.script())) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
        long elapsed = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pluginbase_consumer_schema_history
                    (namespace, version, description, checksum, execution_time_ms)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, namespace);
            statement.setInt(2, migration.version());
            statement.setString(3, migration.description());
            statement.setString(4, checksum);
            statement.setLong(5, elapsed);
            statement.executeUpdate();
        }
        logger.info("Migração {} aplicada para o consumidor {}.", migration.version(), namespace);
    }

    private String findChecksum(Connection connection, String namespace, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT checksum FROM pluginbase_consumer_schema_history
                WHERE namespace = ? AND version = ?
                """)) {
            statement.setString(1, namespace);
            statement.setInt(2, version);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static String checksum(String script) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(script.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", exception);
        }
    }
}
