package br.net.silvioluizsilva.pluginbase.api;

import br.net.silvioluizsilva.pluginbase.service.ManagedService;
import org.bukkit.plugin.Plugin;

/**
 * Agenda tarefas sem expor diretamente a infraestrutura do servidor.
 */
public interface TaskScheduler extends ManagedService {

    /**
     * Executa uma tarefa na thread principal no próximo ciclo.
     *
     * @param action ação segura para a thread principal
     * @return controle da tarefa
     */
    ScheduledTask runSync(Runnable action);

    /**
     * Executa uma tarefa pertencente ao plugin consumidor no próximo ciclo.
     *
     * @param owner plugin responsável pelo ciclo de vida da tarefa
     * @param action ação segura para a thread principal
     * @return controle da tarefa
     */
    ScheduledTask runSync(Plugin owner, Runnable action);

    /**
     * Executa uma tarefa na thread principal após um atraso.
     *
     * @param delayTicks atraso em ticks
     * @param action ação segura para a thread principal
     * @return controle da tarefa
     */
    ScheduledTask runSyncLater(long delayTicks, Runnable action);

    /**
     * Executa uma tarefa pertencente ao plugin consumidor após um atraso.
     *
     * @param owner plugin responsável pelo ciclo de vida da tarefa
     * @param delayTicks atraso em ticks
     * @param action ação segura para a thread principal
     * @return controle da tarefa
     */
    ScheduledTask runSyncLater(Plugin owner, long delayTicks, Runnable action);

    /**
     * Executa periodicamente uma tarefa na thread principal.
     *
     * @param delayTicks atraso inicial em ticks
     * @param periodTicks intervalo mínimo de um tick
     * @param action ação segura para a thread principal
     * @return controle da tarefa
     */
    ScheduledTask runSyncRepeating(long delayTicks, long periodTicks, Runnable action);

    /**
     * Executa periodicamente uma tarefa pertencente ao plugin consumidor.
     *
     * @param owner plugin responsável pelo ciclo de vida da tarefa
     * @param delayTicks atraso inicial em ticks
     * @param periodTicks intervalo mínimo de um tick
     * @param action ação segura para a thread principal
     * @return controle da tarefa
     */
    ScheduledTask runSyncRepeating(Plugin owner, long delayTicks, long periodTicks, Runnable action);

    /**
     * Executa uma operação independente da API Bukkit fora da thread principal.
     *
     * @param action ação assíncrona
     * @return controle da tarefa
     */
    ScheduledTask runAsync(Runnable action);

    /**
     * Executa uma operação assíncrona pertencente ao plugin consumidor.
     *
     * @param owner plugin responsável pelo ciclo de vida da tarefa
     * @param action ação assíncrona
     * @return controle da tarefa
     */
    ScheduledTask runAsync(Plugin owner, Runnable action);

    /**
     * Cancela todas as tarefas pertencentes ao plugin.
     */
    void cancelAll();

    /**
     * Cancela somente as tarefas rastreadas para um plugin consumidor.
     *
     * @param owner plugin proprietário das tarefas
     */
    void cancelAll(Plugin owner);
}
