package br.net.silvioluizsilva.pluginbase.api;

/**
 * Representa uma tarefa registrada no agendador do servidor.
 */
public interface ScheduledTask {

    /**
     * Cancela execuções futuras da tarefa.
     */
    void cancel();

    /**
     * Informa se a tarefa foi cancelada.
     *
     * @return {@code true} quando cancelada
     */
    boolean isCancelled();

    /**
     * Retorna o identificador atribuído pelo servidor.
     *
     * @return identificador da tarefa
     */
    int taskId();
}
