package br.net.silvioluizsilva.pluginbase.database;

import br.net.silvioluizsilva.pluginbase.config.ReconnectConfig;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifica incremento, limite e variação da reconexão. */
final class ReconnectPolicyTest {

    @Test
    void shouldIncreaseBySixtySecondsAndStopAtNineHundred() {
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextDouble()).thenReturn(0.5);
        ReconnectPolicy policy = new ReconnectPolicy(new ReconnectConfig(60, 60, 900, 20), random);

        assertEquals(60L, policy.delay(1).toSeconds());
        assertEquals(120L, policy.delay(2).toSeconds());
        assertEquals(900L, policy.delay(100).toSeconds());
    }

    @Test
    void shouldApplyTwentyPercentJitter() {
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextDouble()).thenReturn(0.0, 0.999999);
        ReconnectPolicy policy = new ReconnectPolicy(new ReconnectConfig(60, 60, 900, 20), random);

        assertEquals(48L, policy.delay(1).toSeconds());
        assertEquals(72L, policy.delay(1).toSeconds());
    }
}
