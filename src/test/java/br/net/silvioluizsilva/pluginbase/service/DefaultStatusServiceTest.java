package br.net.silvioluizsilva.pluginbase.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica o serviço de estado operacional.
 */
final class DefaultStatusServiceTest {

    @Test
    void shouldExposeLifecycleState() {
        Instant now = Instant.parse("2026-07-22T12:00:00Z");
        DefaultStatusService service = new DefaultStatusService(Clock.fixed(now, ZoneOffset.UTC));

        assertFalse(service.isRunning());
        service.start();
        assertTrue(service.isRunning());
        assertEquals(now, service.startedAt().orElseThrow());
        service.stop();
        assertFalse(service.isRunning());
    }
}
