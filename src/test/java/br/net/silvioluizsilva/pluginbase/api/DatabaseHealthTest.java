package br.net.silvioluizsilva.pluginbase.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifica a duração pública da indisponibilidade. */
final class DatabaseHealthTest {

    @Test
    void shouldCalculateCurrentUnavailability() {
        Instant since = Instant.parse("2026-07-22T12:00:00Z");
        DatabaseHealth health = new DatabaseHealth(DatabaseState.DEGRADED, 3L, Optional.empty(),
                Optional.of(since), Optional.empty(), Optional.of(since));

        assertEquals(Duration.ofSeconds(90), health.unavailableFor(since.plusSeconds(90)));
    }
}
