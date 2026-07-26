package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

/** Configuração dos limites transacionais de consumidores. */
public record TransactionConfig(int statementTimeoutSeconds, long slowWarningMs, long criticalWarningMs,
                                int maximumConcurrentPerConsumer, long concurrencyWaitMs) {
    /** Valida os limites transacionais. */
    public TransactionConfig {
        if (statementTimeoutSeconds < 1 || statementTimeoutSeconds > 300) {
            throw new ConfigurationException(
                    "database.transactions.statement-timeout-seconds deve estar entre 1 e 300."
            );
        }
        if (slowWarningMs < 1 || criticalWarningMs < slowWarningMs) {
            throw new ConfigurationException("Os limites de duração das transações são inválidos.");
        }
        if (maximumConcurrentPerConsumer < 1 || maximumConcurrentPerConsumer > 32) {
            throw new ConfigurationException("A concorrência por consumidor deve estar entre 1 e 32.");
        }
        if (concurrencyWaitMs < 0 || concurrencyWaitMs > 60_000) {
            throw new ConfigurationException("database.transactions.concurrency-wait-ms é inválido.");
        }
    }
}
