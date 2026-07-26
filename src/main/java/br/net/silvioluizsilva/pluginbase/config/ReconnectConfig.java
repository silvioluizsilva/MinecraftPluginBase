package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

/** Configuração da reconexão linear com variação aleatória. */
public record ReconnectConfig(int initialDelaySeconds, int incrementSeconds, int maximumDelaySeconds,
                              int jitterPercent) {
    /** Valida a política de reconexão. */
    public ReconnectConfig {
        if (initialDelaySeconds < 1 || incrementSeconds < 1) {
            throw new ConfigurationException("Os intervalos de reconexão devem ser positivos.");
        }
        if (maximumDelaySeconds < initialDelaySeconds || maximumDelaySeconds > 86_400) {
            throw new ConfigurationException("database.reconnect.maximum-delay-seconds é inválido.");
        }
        if (jitterPercent < 0 || jitterPercent > 50) {
            throw new ConfigurationException("database.reconnect.jitter-percent deve estar entre 0 e 50.");
        }
    }
}
