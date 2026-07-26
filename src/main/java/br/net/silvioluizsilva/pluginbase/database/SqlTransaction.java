package br.net.silvioluizsilva.pluginbase.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Representa uma operação atômica executada com uma única conexão.
 *
 * @param <T> tipo do resultado
 */
@FunctionalInterface
public interface SqlTransaction<T> {

    /**
     * Executa as operações da transação.
     *
     * @param connection conexão com commit manual
     * @return resultado da operação
     * @throws SQLException quando uma operação SQL falhar
     */
    T execute(Connection connection) throws SQLException;
}
