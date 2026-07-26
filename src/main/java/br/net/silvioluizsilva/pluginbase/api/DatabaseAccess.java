package br.net.silvioluizsilva.pluginbase.api;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Expõe operações controladas no banco compartilhado pelo PluginBase.
 */
public interface DatabaseAccess {

    /**
     * Retorna o namespace exclusivo do plugin consumidor.
     *
     * @return identificador normalizado
     */
    String namespace();

    /**
     * Gera um nome físico de tabela dentro do namespace do consumidor.
     *
     * @param logicalName nome lógico sem prefixo
     * @return nome de tabela validado
     */
    String table(String logicalName);

    /**
     * Informa se o pool de conexões está ativo.
     *
     * @return {@code true} quando há um pool ativo
     */
    boolean isConnected();

    /**
     * Retorna a saúde atual do banco compartilhado.
     *
     * @return estado e marcos operacionais
     */
    DatabaseHealth health();

    /**
     * Returns a stage completed when the database becomes available.
     *
     * <p>Continuations run away from the primary thread and must not access
     * the Bukkit API.</p>
     *
     * @return asynchronous availability signal
     */
    CompletionStage<Void> whenConnected();

    /**
     * Executa uma operação dentro de uma transação gerenciada.
     *
     * @param work operação JDBC
     * @param <T> tipo do resultado
     * @return resultado da operação
     */
    <T> T transaction(DatabaseWork<T> work);

    /**
     * Aplica migrações idempotentes mantendo histórico e checksum por consumidor.
     *
     * @param migrations migrações em ordem crescente
     */
    void migrate(List<DatabaseMigration> migrations);
}
