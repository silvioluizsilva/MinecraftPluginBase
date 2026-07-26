package br.net.silvioluizsilva.pluginbase.scheduler;

import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica a adaptação do controle de tarefas Bukkit.
 */
final class BukkitScheduledTaskTest {

    @Test
    void shouldDelegateCancellationAndState() {
        BukkitTask delegate = mock(BukkitTask.class);
        when(delegate.getTaskId()).thenReturn(42);
        when(delegate.isCancelled()).thenReturn(true);
        BukkitScheduledTask task = new BukkitScheduledTask(delegate);

        task.cancel();

        verify(delegate).cancel();
        assertTrue(task.isCancelled());
        assertEquals(42, task.taskId());
    }
}
