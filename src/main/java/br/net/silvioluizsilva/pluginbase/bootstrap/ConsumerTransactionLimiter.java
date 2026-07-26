package br.net.silvioluizsilva.pluginbase.bootstrap;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Limita de forma justa a concorrência transacional de um consumidor. */
final class ConsumerTransactionLimiter {

    private final Semaphore semaphore;

    ConsumerTransactionLimiter(int maximumConcurrent) {
        if (maximumConcurrent < 1) {
            throw new IllegalArgumentException("O limite deve ser positivo.");
        }
        semaphore = new Semaphore(maximumConcurrent, true);
    }

    Permit acquire(long waitMs) throws InterruptedException {
        return semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS) ? new Permit(semaphore) : null;
    }

    /** Libera exatamente uma vez a vaga adquirida. */
    static final class Permit implements AutoCloseable {
        private final Semaphore semaphore;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
