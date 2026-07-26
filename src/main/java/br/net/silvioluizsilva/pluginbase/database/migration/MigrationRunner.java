package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.exception.MigrationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Aplica migrações pendentes com bloqueio, histórico e verificação de integridade.
 */
public final class MigrationRunner {

    private static final String LOCK_NAME = "pluginbase_schema_migrations";
    private static final int LOCK_TIMEOUT_SECONDS = 30;
    private static final String CREATE_HISTORY = """
            CREATE TABLE IF NOT EXISTS pluginbase_schema_history (
                version INT UNSIGNED NOT NULL,
                description VARCHAR(255) NOT NULL,
                checksum CHAR(64) NOT NULL,
                installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                execution_time_ms BIGINT UNSIGNED NOT NULL,
                PRIMARY KEY (version)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private final PluginBase plugin;

    /**
     * Cria o executor de migrações.
     *
     * @param plugin instância principal do plugin
     */
    public MigrationRunner(PluginBase plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Aplica todas as migrações ainda não registradas.
     *
     * @param connection conexão exclusiva durante a migração
     */
    public void migrate(Connection connection) {
        Objects.requireNonNull(connection, "connection");
        try {
            createHistoryTable(connection);
            acquireLock(connection);
            try {
                List<Migration> migrations = loadCatalog();
                preventDowngrade(connection, migrations);
                for (Migration migration : migrations) {
                    applyIfPending(connection, migration);
                }
            } finally {
                releaseLock(connection);
            }
        } catch (SQLException exception) {
            throw new MigrationException("Falha ao atualizar o esquema do banco de dados.", exception);
        }
    }

    private List<Migration> loadCatalog() {
        return MigrationCatalog.load(plugin.getResource("sql/migrations.list"));
    }

    private void createHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_HISTORY);
        }
    }

    private void acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)");) {
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new MigrationException("Não foi possível obter o bloqueio de migrações.");
                }
            }
        }
    }

    private void releaseLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    plugin.getPluginLogger().warn("O bloqueio de migrações já não pertencia a esta conexão.");
                }
            }
        } catch (SQLException exception) {
            plugin.getPluginLogger().warn("Não foi possível liberar explicitamente o bloqueio de migrações.");
        }
    }

    private void preventDowngrade(Connection connection, List<Migration> migrations) throws SQLException {
        int latestAvailable = migrations.isEmpty() ? 0 : migrations.getLast().version();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM pluginbase_schema_history")) {
            if (result.next() && result.getInt(1) > latestAvailable) {
                throw new MigrationException("O banco foi criado por uma versão mais recente do plugin.");
            }
        }
    }

    private void applyIfPending(Connection connection, Migration migration) throws SQLException {
        String script = readResource(migration.resourcePath());
        String checksum = checksum(script);
        String installedChecksum = findChecksum(connection, migration.version());
        if (installedChecksum != null) {
            if (!MessageDigest.isEqual(
                    installedChecksum.getBytes(StandardCharsets.US_ASCII),
                    checksum.getBytes(StandardCharsets.US_ASCII))) {
                throw new MigrationException("A migração " + migration.version() + " foi alterada após sua aplicação.");
            }
            return;
        }

        long startedAt = System.nanoTime();
        for (String sql : SqlScriptParser.parse(script)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
        long elapsedMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        recordMigration(connection, migration, checksum, elapsedMs);
        plugin.getPluginLogger().info("Migração {} aplicada: {}.", migration.version(), migration.description());
    }

    private String findChecksum(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT checksum FROM pluginbase_schema_history WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private void recordMigration(
            Connection connection,
            Migration migration,
            String checksum,
            long elapsedMs
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pluginbase_schema_history
                    (version, description, checksum, execution_time_ms)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, checksum);
            statement.setLong(4, elapsedMs);
            statement.executeUpdate();
        }
    }

    private String readResource(String path) {
        try (InputStream input = plugin.getResource(path)) {
            if (input == null) {
                throw new MigrationException("Script de migração ausente: " + path + ".");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new MigrationException("Não foi possível ler a migração " + path + ".", exception);
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
