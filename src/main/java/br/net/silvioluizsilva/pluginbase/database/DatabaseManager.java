package br.net.silvioluizsilva.pluginbase.database;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.api.DatabaseHealth;
import br.net.silvioluizsilva.pluginbase.api.DatabaseState;
import br.net.silvioluizsilva.pluginbase.config.DatabaseConfig;
import br.net.silvioluizsilva.pluginbase.database.migration.MigrationRunner;
import br.net.silvioluizsilva.pluginbase.exception.DatabaseException;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Mantém o pool MySQL, a saúde operacional e a reconexão automática. */
public final class DatabaseManager implements AutoCloseable {

    private final PluginBase plugin;
    private final PluginLogger logger;
    private final PoolFactory poolFactory;
    private final BooleanSupplier primaryThread;
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PluginBase-Database-Monitor");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean reconnecting = new AtomicBoolean();
    private volatile DatabaseConfig databaseConfig;
    private volatile HikariDataSource dataSource;
    private volatile DatabaseState state = DatabaseState.STOPPED;
    private volatile Instant lastConnectedAt;
    private volatile Instant lastFailureAt;
    private volatile Instant nextReconnectAt;
    private volatile Instant unavailableSince;
    private volatile long reconnectAttempt;
    private volatile boolean closed;
    private volatile CompletableFuture<Void> connectionFuture = new CompletableFuture<>();
    private ScheduledFuture<?> reconnectFuture;
    private ScheduledFuture<?> healthFuture;

    /**
     * Cria o gerenciador de banco.
     *
     * @param plugin plugin principal
     * @param databaseConfig configuração validada
     */
    public DatabaseManager(PluginBase plugin, DatabaseConfig databaseConfig) {
        this(plugin, databaseConfig, DatabaseManager::openDataSource, plugin.getPluginLogger(), Bukkit::isPrimaryThread);
    }

    DatabaseManager(PluginBase plugin, DatabaseConfig databaseConfig, PoolFactory poolFactory, PluginLogger logger) {
        this(plugin, databaseConfig, poolFactory, logger, Bukkit::isPrimaryThread);
    }

    DatabaseManager(
            PluginBase plugin,
            DatabaseConfig databaseConfig,
            PoolFactory poolFactory,
            PluginLogger logger,
            BooleanSupplier primaryThread
    ) {
        this.plugin = plugin;
        this.databaseConfig = Objects.requireNonNull(databaseConfig, "databaseConfig");
        this.poolFactory = Objects.requireNonNull(poolFactory, "poolFactory");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.primaryThread = Objects.requireNonNull(primaryThread, "primaryThread");
    }

    /**
     * Starts connection and migration on the database monitor thread.
     *
     * @return a future that completes when the first connection succeeds
     */
    public synchronized CompletableFuture<Void> startAsync() {
        closed = false;
        cancelScheduledTasks();
        connectionFuture = new CompletableFuture<>();
        if (!databaseConfig.enabled()) {
            transition(DatabaseState.DISABLED);
            connectionFuture.complete(null);
            return connectionFuture.copy();
        }
        transition(DatabaseState.INITIALIZING);
        monitor.execute(this::performInitialConnection);
        scheduleHealthCheck();
        return connectionFuture.copy();
    }

    public synchronized void start() {
        startAsync();
    }

    /**
     * Schedules a database reconfiguration without blocking the caller thread.
     *
     * @param candidate validated replacement configuration
     * @return a future completed after the new connection cycle is scheduled
     */
    public CompletableFuture<Void> reconfigureAsync(DatabaseConfig candidate) {
        Objects.requireNonNull(candidate, "candidate");
        CompletableFuture<Void> scheduled = new CompletableFuture<>();
        monitor.execute(() -> {
            synchronized (this) {
                if (closed) {
                    scheduled.completeExceptionally(new IllegalStateException("Database manager is stopped."));
                    return;
                }
                if (candidate.equals(databaseConfig) && state != DatabaseState.STOPPED) {
                    scheduled.complete(null);
                    return;
                }
                closePool();
                databaseConfig = candidate;
                startAsync();
                scheduled.complete(null);
            }
        });
        return scheduled;
    }

    public synchronized void reconfigure(DatabaseConfig candidate) {
        reconfigureAsync(candidate);
    }

    /**
     * Solicita uma tentativa imediata sem permitir concorrência.
     *
     * @return {@code true} quando a tentativa foi agendada
     */
    public synchronized boolean requestReconnect() {
        if (closed || !databaseConfig.enabled() || state == DatabaseState.INITIALIZING
                || state == DatabaseState.CONNECTING || state == DatabaseState.CONNECTED || reconnecting.get()) {
            return false;
        }
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
        }
        nextReconnectAt = Instant.now();
        reconnectFuture = monitor.schedule(this::performReconnect, 0L, TimeUnit.SECONDS);
        return true;
    }

    /** Returns a future completed after the current connection cycle is ready. */
    public CompletableFuture<Void> whenConnected() {
        return connectionFuture.copy();
    }

    /**
     * Retorna uma conexão ativa.
     *
     * @return conexão do pool
     * @throws SQLException quando o pool não fornecer conexão
     */
    public Connection getConnection() throws SQLException {
        requireNonPrimaryThread();
        HikariDataSource current = dataSource;
        if (current == null || current.isClosed() || state != DatabaseState.CONNECTED) {
            throw new IllegalStateException("O banco de dados está indisponível no estado " + state + ".");
        }
        return current.getConnection();
    }

    /** @return {@code true} quando o pool está conectado */
    public boolean isConnected() {
        HikariDataSource current = dataSource;
        return state == DatabaseState.CONNECTED && current != null && !current.isClosed();
    }

    /** @return visão pública da saúde atual */
    public DatabaseHealth health() {
        return new DatabaseHealth(state, reconnectAttempt, Optional.ofNullable(lastConnectedAt),
                Optional.ofNullable(lastFailureAt), Optional.ofNullable(nextReconnectAt),
                Optional.ofNullable(unavailableSince));
    }

    /**
     * Executa uma unidade de trabalho com commit e rollback automáticos.
     *
     * @param transaction operação JDBC
     * @param <T> tipo do resultado
     * @return resultado produzido
     */
    public <T> T transaction(SqlTransaction<T> transaction) {
        Objects.requireNonNull(transaction, "transaction");
        requireNonPrimaryThread();
        try (Connection connection = getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = transaction.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                handleConnectionFailure(exception);
                throw new DatabaseException("A transação de banco de dados foi revertida.", exception);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException | IllegalStateException exception) {
            throw new DatabaseException("Não foi possível executar a transação de banco de dados.", exception);
        }
    }

    /** Encerra monitor, tentativas e pool. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        transition(DatabaseState.STOPPING);
        closed = true;
        cancelScheduledTasks();
        closePool();
        connectionFuture.completeExceptionally(new IllegalStateException("Database manager is stopped."));
        monitor.shutdownNow();
        transition(DatabaseState.STOPPED);
    }

    private HikariDataSource createDataSource(DatabaseConfig candidate) {
        return poolFactory.open(plugin, candidate);
    }

    private static HikariDataSource openDataSource(PluginBase plugin, DatabaseConfig candidate) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(candidate.jdbcUrl());
        config.setUsername(candidate.username());
        config.setPassword(candidate.password());
        config.setMaximumPoolSize(candidate.pool().maximumSize());
        config.setMinimumIdle(candidate.pool().minimumIdle());
        config.setConnectionTimeout(candidate.pool().connectionTimeoutMs());
        config.setPoolName("PluginBase-Pool");
        config.setConnectionTestQuery("SELECT 1");
        HikariDataSource candidateDataSource = new HikariDataSource(config);
        try (Connection connection = candidateDataSource.getConnection()) {
            new MigrationRunner(plugin).migrate(connection);
        } catch (SQLException | RuntimeException exception) {
            candidateDataSource.close();
            throw new DatabaseException("Não foi possível preparar o banco de dados.", exception);
        }
        return candidateDataSource;
    }

    @FunctionalInterface
    interface PoolFactory {
        HikariDataSource open(PluginBase plugin, DatabaseConfig config);
    }

    private synchronized void install(HikariDataSource replacement) {
        if (closed) {
            replacement.close();
            return;
        }
        HikariDataSource previous = dataSource;
        dataSource = replacement;
        if (previous != null && previous != replacement) {
            previous.close();
        }
        lastConnectedAt = Instant.now();
        nextReconnectAt = null;
        unavailableSince = null;
        reconnectAttempt = 0L;
        transition(DatabaseState.CONNECTED);
        logger.info("Conexão com o banco de dados estabelecida.");
    }

    private void performInitialConnection() {
        if (closed) {
            connectionFuture.completeExceptionally(new IllegalStateException("Database manager is stopped."));
            return;
        }
        transition(DatabaseState.CONNECTING);
        try {
            install(createDataSource(databaseConfig));
            connectionFuture.complete(null);
        } catch (RuntimeException exception) {
            registerFailure(exception, true);
            if (!databaseConfig.degradedMode() || isNonRecoverable(exception)) {
                transition(DatabaseState.FAILED);
                connectionFuture.completeExceptionally(exception);
                return;
            }
            transition(DatabaseState.DEGRADED);
            scheduleReconnect();
        }
    }

    private void performReconnect() {
        synchronized (this) {
            reconnectFuture = null;
        }
        if (closed || !reconnecting.compareAndSet(false, true)) {
            return;
        }
        long attempt = ++reconnectAttempt;
        transition(DatabaseState.RECONNECTING);
        nextReconnectAt = null;
        Duration unavailable = unavailableDuration();
        try {
            install(createDataSource(databaseConfig));
            connectionFuture.complete(null);
            logger.info("Banco recuperado na tentativa {} após {} segundos.",
                    attempt, Math.max(0L, unavailable.toSeconds()));
        } catch (RuntimeException exception) {
            registerFailure(exception, false);
            transition(isNonRecoverable(exception) ? DatabaseState.FAILED : DatabaseState.DEGRADED);
        } finally {
            reconnecting.set(false);
        }
        if (state == DatabaseState.DEGRADED) {
            scheduleReconnect();
        }
    }

    private synchronized void scheduleReconnect() {
        if (closed || reconnecting.get() || state == DatabaseState.CONNECTED) {
            return;
        }
        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            return;
        }
        long nextAttempt = reconnectAttempt + 1L;
        Duration delay = new ReconnectPolicy(databaseConfig.reconnect()).delay(nextAttempt);
        nextReconnectAt = Instant.now().plus(delay);
        reconnectFuture = monitor.schedule(this::performReconnect, delay.toSeconds(), TimeUnit.SECONDS);
        logger.warn("Reconexão {} em {} segundos; indisponível há {} segundos.",
                nextAttempt, delay.toSeconds(), unavailableDuration().toSeconds());
    }

    private synchronized void scheduleHealthCheck() {
        int interval = databaseConfig.healthCheck().intervalSeconds();
        healthFuture = monitor.scheduleWithFixedDelay(this::checkHealth, interval, interval, TimeUnit.SECONDS);
    }

    private void checkHealth() {
        if (!isConnected()) {
            return;
        }
        try (Connection connection = getConnection()) {
            if (!connection.isValid(databaseConfig.healthCheck().timeoutSeconds())) {
                throw new SQLException("A validação da conexão retornou falso.");
            }
        } catch (SQLException | RuntimeException exception) {
            synchronized (this) {
                registerFailure(exception, false);
                closePool();
                transition(DatabaseState.DEGRADED);
                scheduleReconnect();
            }
        }
    }

    private void registerFailure(Exception exception, boolean initial) {
        lastFailureAt = Instant.now();
        if (unavailableSince == null) {
            unavailableSince = lastFailureAt;
        }
        if (initial) {
            logger.error("MySQL indisponível; iniciando em modo degradado.", exception);
        } else {
            logger.debug("Tentativa de reconexão {} falhou.", reconnectAttempt);
        }
    }

    private Duration unavailableDuration() {
        Instant since = unavailableSince;
        return since == null ? Duration.ZERO : Duration.between(since, Instant.now());
    }

    private void handleConnectionFailure(Exception exception) {
        if (!hasSqlState(exception, "08", false)) {
            return;
        }
        synchronized (this) {
            registerFailure(exception, false);
            closePool();
            transition(DatabaseState.DEGRADED);
            scheduleReconnect();
        }
    }

    private static boolean isNonRecoverable(Throwable throwable) {
        return hasSqlState(throwable, "28", false) || hasSqlState(throwable, "42", true);
    }

    private static boolean hasSqlState(Throwable throwable, String prefix, boolean requireKnownCode) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String stateCode = sqlException.getSQLState();
                boolean stateMatches = stateCode != null && stateCode.startsWith(prefix);
                boolean knownCode = sqlException.getErrorCode() == 1045 || sqlException.getErrorCode() == 1049;
                if (stateMatches && (!requireKnownCode || knownCode)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private synchronized void closePool() {
        HikariDataSource current = dataSource;
        dataSource = null;
        if (current != null) {
            current.close();
        }
    }

    private synchronized void cancelScheduledTasks() {
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        if (healthFuture != null) {
            healthFuture.cancel(false);
            healthFuture = null;
        }
    }

    private void transition(DatabaseState target) {
        DatabaseState previous = state;
        state = target;
        if (previous != target) {
            logger.info("Estado do banco alterado: {} -> {}; tentativa {}; indisponível há {} segundos.",
                    previous, target, reconnectAttempt, Math.max(0L, unavailableDuration().toSeconds()));
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private void requireNonPrimaryThread() {
        if (primaryThread.getAsBoolean()) {
            throw new DatabaseException("JDBC must not execute on the primary server thread.");
        }
    }
}
