package br.net.silvioluizsilva.pluginbase.scheduler;

import br.net.silvioluizsilva.pluginbase.api.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * Adapta uma tarefa Bukkit ao contrato público do plugin.
 */
public final class BukkitScheduledTask implements ScheduledTask {

    private final BukkitTask delegate;

    /**
     * Cria um controle de tarefa.
     *
     * @param delegate tarefa criada pelo servidor
     */
    public BukkitScheduledTask(BukkitTask delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Cancela execuções futuras.
     */
    @Override
    public void cancel() {
        delegate.cancel();
    }

    /**
     * Informa se a tarefa foi cancelada.
     *
     * @return {@code true} quando cancelada
     */
    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    /**
     * Retorna o identificador da tarefa.
     *
     * @return identificador Bukkit
     */
    @Override
    public int taskId() {
        return delegate.getTaskId();
    }
}
