package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica os limites seguros da configuração web.
 */
final class WebConfigTest {

    private static final String HASH = "930bbdc51b6aed5c2a5678fd6e28dee7a05e8a4b643cfc0b4427c3efb86c0d94";

    @Test
    void shouldAcceptEnabledLoopbackConfiguration() {
        WebConfig config = new WebConfig(
                true,
                "127.0.0.1",
                8080,
                List.of("https://admin.example.com"),
                65_536,
                HASH
        );

        assertEquals("127.0.0.1", config.bindAddress());
    }

    @Test
    void shouldRejectExternalBindAddress() {
        assertThrows(
                ConfigurationException.class,
                () -> new WebConfig(false, "0.0.0.0", 8080, List.of(), 65_536, "")
        );
    }

    @Test
    void shouldRejectWildcardOrigin() {
        assertThrows(
                ConfigurationException.class,
                () -> new WebConfig(true, "localhost", 8080, List.of("*"), 65_536, HASH)
        );
    }

    @Test
    void shouldRejectEnabledConfigurationWithoutTokenHash() {
        assertThrows(
                ConfigurationException.class,
                () -> new WebConfig(true, "::1", 8080, List.of("http://localhost:3000"), 65_536, "")
        );
    }
}
