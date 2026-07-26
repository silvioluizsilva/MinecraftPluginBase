package br.net.silvioluizsilva.pluginbase.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Representa uma unidade de trabalho JDBC executada pelo PluginBase.
 *
 * @param <T> tipo do resultado
 */
@FunctionalInterface
public interface DatabaseWork<T> {

    /**
     * Executa a unidade de trabalho usando a conexão transacional fornecida.
     *
     * @param connection conexão pertencente ao PluginBase
     * @return resultado da operação
     * @throws SQLException quando a operação JDBC falhar
     */
    T execute(Connection connection) throws SQLException;
}
