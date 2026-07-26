package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

/** Configuração da verificação periódica da conexão. */
public record HealthCheckConfig(int intervalSeconds, int timeoutSeconds) {
    /** Valida os intervalos operacionais. */
    public HealthCheckConfig {
        if (intervalSeconds < 5 || intervalSeconds > 3600) {
            throw new ConfigurationException("database.health-check.interval-seconds deve estar entre 5 e 3600.");
        }
        if (timeoutSeconds < 1 || timeoutSeconds > 30) {
            throw new ConfigurationException("database.health-check.timeout-seconds deve estar entre 1 e 30.");
        }
    }
}
