package br.net.silvioluizsilva.pluginbase.scheduler;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.api.ScheduledTask;
import br.net.silvioluizsilva.pluginbase.api.TaskScheduler;
import br.net.silvioluizsilva.pluginbase.exception.ServiceException;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementa agendamento Paper com rastreamento e tratamento de falhas.
 */
public final class PaperTaskScheduler implements TaskScheduler {

    private final PluginBase plugin;
    private final PluginLogger logger;
    private final BukkitScheduler scheduler;
    private final ConcurrentMap<Integer, BukkitTask> tasks = new ConcurrentHashMap<>();
    private volatile boolean running;

    /**
     * Cria o agendador do plugin.
     *
     * @param plugin instância principal
     * @param logger logger seguro
     */
    public PaperTaskScheduler(PluginBase plugin, PluginLogger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.scheduler = plugin.getServer().getScheduler();
    }

    /**
     * Habilita o recebimento de novas tarefas.
     */
    @Override
    public synchronized void start() {
        if (running) {
            throw new ServiceException("O agendador já está em execução.");
        }
        running = true;
    }

    /**
     * Cancela as tarefas e encerra o agendador.
     */
    @Override
    public synchronized void stop() {
        running = false;
        cancelAll();
    }

    /**
     * Agenda uma ação síncrona para o próximo ciclo.
     *
     * @param action ação a executar
     * @return controle da tarefa
     */
    @Override
    public ScheduledTask runSync(Runnable action) {
        return runSync(plugin, action);
    }

    @Override
    public ScheduledTask runSync(Plugin owner, Runnable action) {
        return schedule(owner, false, 0L, 0L, false, action);
    }

    /**
     * Agenda uma ação síncrona com atraso.
     *
     * @param delayTicks atraso em ticks
     * @param action ação a executar
     * @return controle da tarefa
     */
    @Override
    public ScheduledTask runSyncLater(long delayTicks, Runnable action) {
        return runSyncLater(plugin, delayTicks, action);
    }

    @Override
    public ScheduledTask runSyncLater(Plugin owner, long delayTicks, Runnable action) {
        validateDelay(delayTicks);
        return schedule(owner, false, delayTicks, 0L, false, action);
    }

    /**
     * Agenda uma ação síncrona repetitiva.
     *
     * @param delayTicks atraso inicial em ticks
     * @param periodTicks intervalo em ticks
     * @param action ação a executar
     * @return controle da tarefa
     */
    @Override
    public ScheduledTask runSyncRepeating(long delayTicks, long periodTicks, Runnable action) {
        return runSyncRepeating(plugin, delayTicks, periodTicks, action);
    }

    @Override
    public ScheduledTask runSyncRepeating(Plugin owner, long delayTicks, long periodTicks, Runnable action) {
        validateDelay(delayTicks);
        if (periodTicks < 1L) {
            throw new IllegalArgumentException("O período deve ser de pelo menos um tick.");
        }
        return schedule(owner, false, delayTicks, periodTicks, true, action);
    }

    /**
     * Agenda uma ação assíncrona que não deve acessar a API Bukkit.
     *
     * @param action ação a executar
     * @return controle da tarefa
     */
    @Override
    public ScheduledTask runAsync(Runnable action) {
        return runAsync(plugin, action);
    }

    @Override
    public ScheduledTask runAsync(Plugin owner, Runnable action) {
        return schedule(owner, true, 0L, 0L, false, action);
    }

    /**
     * Cancela todas as tarefas rastreadas.
     */
    @Override
    public synchronized void cancelAll() {
        tasks.values().forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });
        tasks.clear();
        scheduler.cancelTasks(plugin);
    }

    @Override
    public synchronized void cancelAll(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        tasks.values().removeIf(task -> {
            if (task.getOwner() != owner) {
                return false;
            }
            if (!task.isCancelled()) {
                task.cancel();
            }
            return true;
        });
    }

    private synchronized ScheduledTask schedule(
            Plugin owner,
            boolean async,
            long delay,
            long period,
            boolean repeating,
            Runnable action
    ) {
        requireRunning();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(action, "action");
        ManagedRunnable managed = new ManagedRunnable(action, !repeating);
        BukkitTask task;
        if (repeating) {
            task = scheduler.runTaskTimer(owner, managed, delay, period);
        } else if (async) {
            task = scheduler.runTaskAsynchronously(owner, managed);
        } else if (delay > 0L) {
            task = scheduler.runTaskLater(owner, managed, delay);
        } else {
            task = scheduler.runTask(owner, managed);
        }
        managed.bind(task);
        tasks.put(task.getTaskId(), task);
        managed.removeIfAlreadyCompleted();
        return new BukkitScheduledTask(task);
    }

    private void requireRunning() {
        if (!running) {
            throw new ServiceException("O agendador não está em execução.");
        }
    }

    private static void validateDelay(long delayTicks) {
        if (delayTicks < 0L) {
            throw new IllegalArgumentException("O atraso não pode ser negativo.");
        }
    }

    private final class ManagedRunnable implements Runnable {
        private final Runnable action;
        private final boolean oneShot;
        private volatile BukkitTask task;
        private volatile boolean completed;

        private ManagedRunnable(Runnable action, boolean oneShot) {
            this.action = action;
            this.oneShot = oneShot;
        }

        @Override
        public void run() {
            try {
                action.run();
            } catch (RuntimeException exception) {
                logger.error("Falha durante a execução de uma tarefa agendada.", exception);
            } finally {
                if (oneShot) {
                    completed = true;
                    BukkitTask current = task;
                    if (current != null) {
                        tasks.remove(current.getTaskId());
                    }
                }
            }
        }

        private void bind(BukkitTask task) {
            this.task = task;
        }

        private void removeIfAlreadyCompleted() {
            if (completed && task != null) {
                tasks.remove(task.getTaskId());
            }
        }
    }
}
