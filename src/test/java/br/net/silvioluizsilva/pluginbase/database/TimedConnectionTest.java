package br.net.silvioluizsilva.pluginbase.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLFeatureNotSupportedException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifica a proteção e o timeout da conexão consumidora. */
final class TimedConnectionTest {

    @Test
    void shouldApplyTimeoutAndPreserveConnectionOwnership() throws Exception {
        Connection delegate = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(delegate.prepareStatement("SELECT 1")).thenReturn(statement);
        Connection connection = TimedConnection.wrap(delegate, 30);

        connection.prepareStatement("SELECT 1");
        connection.close();

        verify(statement).setQueryTimeout(30);
        verify(delegate, never()).close();
        assertThrows(SQLFeatureNotSupportedException.class, connection::commit);
        assertThrows(SQLFeatureNotSupportedException.class, connection::rollback);
    }
}
