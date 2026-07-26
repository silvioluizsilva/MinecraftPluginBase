package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.api.DatabaseMigration;
import br.net.silvioluizsilva.pluginbase.config.LoggingConfig;
import br.net.silvioluizsilva.pluginbase.exception.MigrationException;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * Verifica as barreiras de namespace do catálogo consumidor.
 */
final class ConsumerMigrationRunnerTest {

    @Test
    void shouldRejectTableOutsideConsumerNamespaceBeforeUsingConnection() {
        ConsumerMigrationRunner runner = new ConsumerMigrationRunner(
                new PluginLogger(mock(Logger.class), new LoggingConfig(false, false))
        );
        DatabaseMigration migration = new DatabaseMigration(
                1,
                "Invalid cross-plugin access",
                "ALTER TABLE anotherplugin_players ADD COLUMN value INT"
        );

        assertThrows(
                MigrationException.class,
                () -> runner.migrate(mock(Connection.class), "pluginexample", List.of(migration))
        );
    }

    @Test
    void shouldRejectDuplicateOrUnorderedVersionsBeforeUsingConnection() {
        ConsumerMigrationRunner runner = new ConsumerMigrationRunner(
                new PluginLogger(mock(Logger.class), new LoggingConfig(false, false))
        );
        DatabaseMigration first = new DatabaseMigration(2, "Second", "CREATE TABLE pluginexample_second (id INT)");
        DatabaseMigration older = new DatabaseMigration(1, "First", "CREATE TABLE pluginexample_first (id INT)");

        assertThrows(
                MigrationException.class,
                () -> runner.migrate(mock(Connection.class), "pluginexample", List.of(first, older))
        );
    }

    @Test
    void shouldNotInterpretOnUpdateClauseAsATableReference() throws Exception {
        ConsumerMigrationRunner runner = new ConsumerMigrationRunner(
                new PluginLogger(mock(Logger.class), new LoggingConfig(false, false))
        );
        DatabaseMigration migration = new DatabaseMigration(1, "Valid", """
                CREATE TABLE pluginexample_players (
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
        Connection connection = mock(Connection.class);
        java.sql.Statement statement = mock(java.sql.Statement.class);
        java.sql.PreparedStatement lock = mock(java.sql.PreparedStatement.class);
        java.sql.ResultSet lockResult = mock(java.sql.ResultSet.class);
        org.mockito.Mockito.when(connection.createStatement()).thenReturn(statement);
        org.mockito.Mockito.when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(lock);
        org.mockito.Mockito.when(lock.executeQuery()).thenReturn(lockResult);
        org.mockito.Mockito.when(lockResult.next()).thenReturn(true, true, false);
        org.mockito.Mockito.when(lockResult.getInt(1)).thenReturn(1);

        assertDoesNotThrow(() -> runner.migrate(connection, "pluginexample", List.of(migration)));
        org.mockito.Mockito.verify(connection, org.mockito.Mockito.atLeastOnce()).createStatement();
    }
}
