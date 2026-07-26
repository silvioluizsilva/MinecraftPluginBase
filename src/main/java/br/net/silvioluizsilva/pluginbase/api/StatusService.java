package br.net.silvioluizsilva.pluginbase.api;

import br.net.silvioluizsilva.pluginbase.service.ManagedService;

import java.time.Instant;
import java.util.Optional;

/**
 * Expõe o estado operacional básico da aplicação.
 */
public interface StatusService extends ManagedService {

    /**
     * Informa se o serviço está em execução.
     *
     * @return {@code true} quando iniciado
     */
    boolean isRunning();

    /**
     * Retorna o instante da última inicialização.
     *
     * @return instante, ou vazio antes da inicialização
     */
    Optional<Instant> startedAt();
}
