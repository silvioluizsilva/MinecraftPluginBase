package br.net.silvioluizsilva.pluginbase.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Verifica limite, espera e liberação das vagas transacionais. */
final class ConsumerTransactionLimiterTest {

    @Test
    void shouldAllowTwoConcurrentTransactionsAndRejectTheThird() throws Exception {
        ConsumerTransactionLimiter limiter = new ConsumerTransactionLimiter(2);
        ConsumerTransactionLimiter.Permit first = limiter.acquire(0L);
        ConsumerTransactionLimiter.Permit second = limiter.acquire(0L);

        assertNotNull(first);
        assertNotNull(second);
        assertNull(limiter.acquire(1L));

        first.close();
        ConsumerTransactionLimiter.Permit replacement = limiter.acquire(1L);
        assertNotNull(replacement);
        first.close();
        second.close();
        replacement.close();
    }
}
