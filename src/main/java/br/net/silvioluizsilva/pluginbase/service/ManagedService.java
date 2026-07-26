package br.net.silvioluizsilva.pluginbase.service;

/**
 * Contrato de ciclo de vida para serviços da aplicação.
 */
public interface ManagedService {

    /**
     * Inicializa o serviço e seus recursos.
     */
    void start();

    /**
     * Encerra o serviço e libera seus recursos.
     */
    void stop();
}
