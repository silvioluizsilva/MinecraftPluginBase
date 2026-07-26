package br.net.silvioluizsilva.pluginbase.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.plugin.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protege elementos básicos do contrato público contra mudanças acidentais.
 */
final class PluginBaseApiContractTest {

    @Test
    void shouldExposeStableInitialApiVersion() {
        assertEquals("1.1", PluginBaseApi.API_VERSION);
        assertTrue(PluginBaseApi.class.isInterface());
    }

    @Test
    void publicSettingsMustNotExposeCredentials() {
        Set<String> components = Arrays.stream(PluginBaseSettings.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());

        assertEquals(Set.of("language", "databaseenabled", "databaseavailable", "debugenabled"), components);
        assertFalse(components.contains("password"));
        assertFalse(components.contains("username"));
        assertFalse(components.contains("jdbcurl"));
    }

    @Test
    void shouldKeepDatabaseApiElevenContract() throws Exception {
        assertEquals(DatabaseAccess.class, PluginBaseApi.class.getMethod("database", Plugin.class).getReturnType());
        assertEquals(DatabaseHealth.class, DatabaseAccess.class.getMethod("health").getReturnType());
        assertEquals(java.util.concurrent.CompletionStage.class,
                DatabaseAccess.class.getMethod("whenConnected").getReturnType());
        assertEquals(String.class, DatabaseAccess.class.getMethod("table", String.class).getReturnType());
        assertEquals(void.class, DatabaseAccess.class.getMethod("migrate", java.util.List.class).getReturnType());
    }
}
