package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.api.DatabaseMigration;
import br.net.silvioluizsilva.pluginbase.exception.DatabaseException;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Protects consumer migrations from accidental execution on the primary thread. */
final class DefaultDatabaseAccessThreadTest {

    @Test
    void shouldRejectConsumerMigrationsOnThePrimaryThread() {
        PluginContext context = mock(PluginContext.class);
        Plugin owner = mock(Plugin.class);
        when(owner.getName()).thenReturn("PluginExample");
        DefaultDatabaseAccess access = new DefaultDatabaseAccess(
                context, owner, new ConsumerTransactionLimiter(1), () -> true
        );

        assertThrows(DatabaseException.class, () -> access.migrate(List.of(
                new DatabaseMigration(1, "Initial", "CREATE TABLE pluginexample_visits (id INT)")
        )));
    }
}
