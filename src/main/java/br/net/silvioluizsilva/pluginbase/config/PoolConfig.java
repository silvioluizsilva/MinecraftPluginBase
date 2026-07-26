package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

/**
 * Configuração imutável do pool de conexões.
 *
 * @param maximumSize quantidade máxima de conexões
 * @param minimumIdle quantidade mínima de conexões ociosas
 * @param connectionTimeoutMs tempo máximo de espera por conexão
 */
public record PoolConfig(int maximumSize, int minimumIdle, long connectionTimeoutMs) {

    /**
     * Valida os limites operacionais do pool.
     */
    public PoolConfig {
        if (maximumSize < 1 || maximumSize > 100) {
            throw new ConfigurationException("database.pool.maximum-size deve estar entre 1 e 100.");
        }
        if (minimumIdle < 0 || minimumIdle > maximumSize) {
            throw new ConfigurationException("database.pool.minimum-idle deve estar entre 0 e maximum-size.");
        }
        if (connectionTimeoutMs < 250 || connectionTimeoutMs > 60_000) {
            throw new ConfigurationException("database.pool.connection-timeout-ms deve estar entre 250 e 60000.");
        }
    }
}
