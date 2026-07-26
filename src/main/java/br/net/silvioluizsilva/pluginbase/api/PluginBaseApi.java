package br.net.silvioluizsilva.pluginbase.api;

import br.net.silvioluizsilva.pluginbase.service.ManagedService;

import java.util.Optional;
import org.bukkit.plugin.Plugin;

/**
 * Contrato público para integração de outros plugins com o PluginBase.
 *
 * <p>Implementações consumidoras devem depender somente deste pacote e nunca
 * realizar cast para classes internas.</p>
 */
public interface PluginBaseApi {

    /** Versão inicial do contrato público. */
    String API_VERSION = "1.1";

    /**
     * Retorna a versão do contrato da API.
     *
     * @return versão da API
     */
    default String apiVersion() {
        return API_VERSION;
    }

    /**
     * Retorna a versão instalada do plugin provedor.
     *
     * @return versão do PluginBase
     */
    String pluginVersion();

    /**
     * Retorna uma visão da configuração sem credenciais.
     *
     * @return configurações públicas
     */
    PluginBaseSettings settings();

    /**
     * Retorna o serviço de estado operacional.
     *
     * @return serviço de estado
     */
    StatusService status();

    /**
     * Retorna o agendador gerenciado pelo ciclo de vida do plugin.
     *
     * @return serviço de tarefas
     */
    TaskScheduler scheduler();

    /**
     * Retorna o acesso transacional ao banco compartilhado.
     *
     * @param owner plugin consumidor responsável pelo namespace
     * @return acesso controlado ao banco
     */
    DatabaseAccess database(Plugin owner);

    /**
     * Procura um serviço público registrado.
     *
     * @param contract contrato do serviço
     * @param <T> tipo do serviço
     * @return implementação, quando registrada
     */
    <T extends ManagedService> Optional<T> findService(Class<T> contract);
}
