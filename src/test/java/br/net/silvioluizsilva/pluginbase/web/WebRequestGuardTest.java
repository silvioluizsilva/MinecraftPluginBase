package br.net.silvioluizsilva.pluginbase.web;

import br.net.silvioluizsilva.pluginbase.config.WebConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica a ordem das barreiras preliminares de requisição.
 */
final class WebRequestGuardTest {

    private static final String ORIGIN = "https://admin.example.com";
    private static final String HASH = "930bbdc51b6aed5c2a5678fd6e28dee7a05e8a4b643cfc0b4427c3efb86c0d94";

    @Test
    void shouldAllowValidRequest() {
        WebRequestGuard guard = guard();

        assertEquals(
                WebRequestDecision.ALLOWED,
                guard.evaluate(ORIGIN, 1_024, "Bearer secret-token")
        );
    }

    @Test
    void shouldRejectOriginBeforeCredentials() {
        WebRequestGuard guard = guard();

        assertEquals(
                WebRequestDecision.ORIGIN_DENIED,
                guard.evaluate("https://evil.example.com", 1_024, "Bearer secret-token")
        );
    }

    @Test
    void shouldRejectLargePayloadAndInvalidCredential() {
        WebRequestGuard guard = guard();

        assertEquals(
                WebRequestDecision.PAYLOAD_TOO_LARGE,
                guard.evaluate(ORIGIN, 65_537, "Bearer secret-token")
        );
        assertEquals(
                WebRequestDecision.UNAUTHORIZED,
                guard.evaluate(ORIGIN, 1_024, "Bearer wrong-token")
        );
    }

    private static WebRequestGuard guard() {
        return new WebRequestGuard(new WebConfig(
                true,
                "127.0.0.1",
                8080,
                List.of(ORIGIN),
                65_536,
                HASH
        ));
    }
}
