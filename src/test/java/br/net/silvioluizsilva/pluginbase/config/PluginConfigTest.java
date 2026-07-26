package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica as invariantes dos modelos de configuração.
 */
final class PluginConfigTest {

    @Test
    void shouldRejectInvalidLocale() {
        DatabaseConfig database = new DatabaseConfig(
                false,
                "localhost",
                3306,
                "pluginbase",
                "pluginbase",
                "",
                "useSSL=true",
                new PoolConfig(10, 2, 10_000)
        );

        assertThrows(
                ConfigurationException.class,
                () -> new PluginConfig(
                        "portuguese",
                        database,
                        new LoggingConfig(false, false),
                        disabledWebConfig()
                )
        );
    }

    @Test
    void shouldRejectInvalidDatabaseIdentifier() {
        assertThrows(
                ConfigurationException.class,
                () -> new DatabaseConfig(
                        false,
                        "localhost",
                        3306,
                        "pluginbase;DROP_DATABASE",
                        "pluginbase",
                        "",
                        "useSSL=true",
                        new PoolConfig(10, 2, 10_000)
                )
        );
    }

    private static WebConfig disabledWebConfig() {
        return new WebConfig(false, "127.0.0.1", 8080, java.util.List.of(), 65_536L, "");
    }
}
