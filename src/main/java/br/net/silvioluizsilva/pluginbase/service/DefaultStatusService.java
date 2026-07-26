package br.net.silvioluizsilva.pluginbase.service;

import br.net.silvioluizsilva.pluginbase.api.StatusService;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementação padrão do estado operacional do plugin.
 */
public final class DefaultStatusService implements StatusService {

    private final Clock clock;
    private Instant startedAt;

    /**
     * Cria o serviço usando o relógio do sistema.
     */
    public DefaultStatusService() {
        this(Clock.systemUTC());
    }

    /**
     * Cria o serviço com um relógio controlável.
     *
     * @param clock fonte de tempo
     */
    public DefaultStatusService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Marca o serviço como iniciado.
     */
    @Override
    public synchronized void start() {
        if (startedAt != null) {
            throw new IllegalStateException("O serviço de status já está iniciado.");
        }
        startedAt = clock.instant();
    }

    /**
     * Marca o serviço como encerrado.
     */
    @Override
    public synchronized void stop() {
        startedAt = null;
    }

    /**
     * Informa se o serviço está em execução.
     *
     * @return {@code true} quando iniciado
     */
    @Override
    public synchronized boolean isRunning() {
        return startedAt != null;
    }

    /**
     * Retorna o instante da última inicialização.
     *
     * @return instante, ou vazio antes da inicialização
     */
    @Override
    public synchronized Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }
}
