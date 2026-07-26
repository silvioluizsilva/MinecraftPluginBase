package br.net.silvioluizsilva.pluginbase.service;

import br.net.silvioluizsilva.pluginbase.config.PluginConfig;

/**
 * Contrato opcional para serviços que aceitam recarga em execução.
 */
public interface ReloadableService extends ManagedService {

    /**
     * Aplica uma nova configuração já validada.
     *
     * @param config configuração ativa
     */
    void reload(PluginConfig config);
}
