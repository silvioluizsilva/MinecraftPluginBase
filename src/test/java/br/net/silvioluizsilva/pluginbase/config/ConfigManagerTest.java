package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;
import br.net.silvioluizsilva.pluginbase.PluginBase;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica a leitura e a validação da configuração tipada.
 */
final class ConfigManagerTest {

    @Test
    void shouldParseValidDefaults() {
        YamlConfiguration yaml = validConfiguration();

        PluginConfig config = ConfigManager.parse(yaml);

        assertEquals("pt_BR", config.language());
        assertFalse(config.database().enabled());
        assertEquals(10, config.database().pool().maximumSize());
        assertEquals(30, config.database().healthCheck().intervalSeconds());
        assertEquals(900, config.database().reconnect().maximumDelaySeconds());
        assertEquals(20, config.database().reconnect().jitterPercent());
        assertEquals(30, config.database().transactions().statementTimeoutSeconds());
        assertEquals(2, config.database().transactions().maximumConcurrentPerConsumer());
        assertEquals("jdbc:mysql://127.0.0.1:3306/pluginbase?useSSL=true", config.database().jdbcUrl());
        assertFalse(config.web().enabled());
    }

    @Test
    void shouldRejectUnsafeDefaultPasswordWhenDatabaseIsEnabled() {
        YamlConfiguration yaml = validConfiguration();
        yaml.set("database.enabled", true);

        assertThrows(ConfigurationException.class, () -> ConfigManager.parse(yaml));
    }

    @Test
    void shouldRejectMinimumIdleAboveMaximumSize() {
        YamlConfiguration yaml = validConfiguration();
        yaml.set("database.pool.minimum-idle", 11);

        assertThrows(ConfigurationException.class, () -> ConfigManager.parse(yaml));
    }

    @Test
    void shouldRestoreBukkitConfigurationAfterInvalidReload() {
        YamlConfiguration previous = validConfiguration();
        YamlConfiguration invalid = validConfiguration();
        invalid.set("database.pool.minimum-idle", 11);
        PluginBase plugin = mock(PluginBase.class);
        when(plugin.getConfig()).thenReturn(previous, invalid, invalid);
        doNothing().when(plugin).reloadConfig();
        ConfigManager manager = new ConfigManager(plugin);

        assertThrows(ConfigurationException.class, manager::reloadCandidate);

        assertEquals(2, invalid.getInt("database.pool.minimum-idle"));
        assertEquals("pt_BR", invalid.getString("language"));
    }

    private static YamlConfiguration validConfiguration() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("language", "pt_BR");
        yaml.set("database.enabled", false);
        yaml.set("database.host", "127.0.0.1");
        yaml.set("database.port", 3306);
        yaml.set("database.name", "pluginbase");
        yaml.set("database.username", "pluginbase");
        yaml.set("database.password", "change-me");
        yaml.set("database.parameters", "useSSL=true");
        yaml.set("database.pool.maximum-size", 10);
        yaml.set("database.pool.minimum-idle", 2);
        yaml.set("database.pool.connection-timeout-ms", 10_000L);
        yaml.set("logging.debug", false);
        yaml.set("logging.include-stack-traces", false);
        return yaml;
    }
}
