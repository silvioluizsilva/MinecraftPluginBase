package br.net.silvioluizsilva.pluginbase.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;

/** Aplica timeout automaticamente às instruções criadas por um consumidor. */
public final class TimedConnection {

    private TimedConnection() {
    }

    /**
     * Cria uma fachada que preserva a propriedade da conexão e configura timeouts.
     *
     * @param delegate conexão transacional real
     * @param timeoutSeconds timeout das instruções
     * @return conexão protegida
     */
    public static Connection wrap(Connection delegate, int timeoutSeconds) {
        Objects.requireNonNull(delegate, "delegate");
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("commit".equals(method.getName()) || "rollback".equals(method.getName())
                            || "setAutoCommit".equals(method.getName()) || "unwrap".equals(method.getName())) {
                        throw new SQLFeatureNotSupportedException(
                                "O ciclo transacional pertence ao PluginBase."
                        );
                    }
                    if ("isWrapperFor".equals(method.getName())) {
                        return false;
                    }
                    try {
                        Object result = method.invoke(delegate, arguments);
                        if (result instanceof Statement statement
                                && (method.getName().startsWith("create")
                                || method.getName().startsWith("prepare"))) {
                            statement.setQueryTimeout(timeoutSeconds);
                        }
                        return result;
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }
}
