package br.net.silvioluizsilva.pluginbase.database;

import br.net.silvioluizsilva.pluginbase.config.ReconnectConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Calcula atrasos lineares limitados com variação aleatória. */
public final class ReconnectPolicy {

    private final ReconnectConfig config;
    private final RandomGenerator random;

    /**
     * Cria a política usando a fonte aleatória padrão.
     *
     * @param config configuração validada
     */
    public ReconnectPolicy(ReconnectConfig config) {
        this(config, RandomGenerator.getDefault());
    }

    ReconnectPolicy(ReconnectConfig config, RandomGenerator random) {
        this.config = Objects.requireNonNull(config, "config");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Calcula o atraso da tentativa informada.
     *
     * @param attempt tentativa iniciada em um
     * @return atraso positivo
     */
    public Duration delay(long attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("A tentativa deve ser positiva.");
        }
        long incrementCount = Math.min(attempt - 1, Integer.MAX_VALUE);
        long base = Math.min(config.maximumDelaySeconds(),
                config.initialDelaySeconds() + incrementCount * config.incrementSeconds());
        double variation = config.jitterPercent() / 100.0;
        double factor = 1.0 - variation + random.nextDouble() * variation * 2.0;
        return Duration.ofSeconds(Math.max(1L, Math.round(base * factor)));
    }
}
