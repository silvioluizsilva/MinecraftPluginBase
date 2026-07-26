package br.net.silvioluizsilva.pluginbase.database;

import br.net.silvioluizsilva.pluginbase.api.DatabaseState;
import br.net.silvioluizsilva.pluginbase.config.DatabaseConfig;
import br.net.silvioluizsilva.pluginbase.config.LoggingConfig;
import br.net.silvioluizsilva.pluginbase.config.PoolConfig;
import br.net.silvioluizsilva.pluginbase.exception.DatabaseException;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifica as transições determinísticas de inicialização e encerramento. */
final class DatabaseManagerStateTest {

    @Test
    void shouldRemainOperationalWhenDatabaseIsDisabled() {
        DatabaseManager manager = new DatabaseManager(null, config(false), (plugin, config) -> {
            throw new AssertionError("O pool não deve ser aberto.");
        }, logger());

        manager.start();
        assertEquals(DatabaseState.DISABLED, manager.health().state());
        manager.close();
        assertEquals(DatabaseState.STOPPED, manager.health().state());
    }

    @Test
    void shouldEnterDegradedModeWhenInitialConnectionFails() throws InterruptedException {
        DatabaseManager manager = new DatabaseManager(null, config(true), (plugin, config) -> {
            throw new DatabaseExceptionForTest();
        }, logger());

        manager.startAsync();
        awaitState(manager, DatabaseState.DEGRADED);
        assertEquals(DatabaseState.DEGRADED, manager.health().state());
        manager.close();
    }

    @Test
    void shouldBecomeConnectedWhenPoolIsPrepared() throws Exception {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        when(dataSource.isClosed()).thenReturn(false);
        DatabaseManager manager = new DatabaseManager(null, config(true),
                (plugin, config) -> dataSource, logger());

        manager.startAsync().get(5L, TimeUnit.SECONDS);
        assertEquals(DatabaseState.CONNECTED, manager.health().state());
        manager.close();
    }

    @Test
    void shouldOpenTheInitialPoolAwayFromTheCallingThread() throws Exception {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        when(dataSource.isClosed()).thenReturn(false);
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> poolThread = new AtomicReference<>();
        DatabaseManager manager = new DatabaseManager(null, config(true), (plugin, databaseConfig) -> {
            poolThread.set(Thread.currentThread());
            return dataSource;
        }, logger(), () -> false);

        manager.startAsync().get(5L, TimeUnit.SECONDS);

        assertNotEquals(caller, poolThread.get());
        manager.close();
    }

    @Test
    void shouldRejectJdbcOnThePrimaryThread() {
        HikariDataSource dataSource = mock(HikariDataSource.class);
        when(dataSource.isClosed()).thenReturn(false);
        DatabaseManager manager = new DatabaseManager(null, config(true), (plugin, databaseConfig) -> dataSource,
                logger(), () -> true);

        manager.start();

        assertThrows(DatabaseException.class, manager::getConnection);
        assertThrows(DatabaseException.class, () -> manager.transaction(connection -> null));
        manager.close();
    }

    private static PluginLogger logger() {
        return new PluginLogger(mock(Logger.class), new LoggingConfig(false, false));
    }

    private static void awaitState(DatabaseManager manager, DatabaseState expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5L);
        while (manager.health().state() != expected && System.nanoTime() < deadline) {
            Thread.sleep(10L);
        }
        assertEquals(expected, manager.health().state());
    }

    private static DatabaseConfig config(boolean enabled) {
        return new DatabaseConfig(enabled, "127.0.0.1", 3306, "pluginbase", "pluginbase",
                enabled ? "secure-password" : "change-me", "useSSL=true", new PoolConfig(2, 1, 1_000L));
    }

    private static final class DatabaseExceptionForTest extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
