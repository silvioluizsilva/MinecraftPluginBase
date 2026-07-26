package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.api.PluginBaseApi;
import org.bukkit.plugin.ServicePriority;

import java.util.Objects;

/**
 * Publica a API somente depois da inicialização completa da aplicação.
 */
public final class ApiRegistrar {

    private ApiRegistrar() {
    }

    /**
     * Registra a API no ServicesManager do Paper.
     *
     * @param plugin instância principal
     * @param context contexto inicializado
     */
    public static void register(PluginBase plugin, PluginContext context) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(context, "context");
        PluginBaseApi api = new DefaultPluginBaseApi(plugin.getPluginMeta().getVersion(), context);
        plugin.getServer().getServicesManager().register(
                PluginBaseApi.class,
                api,
                plugin,
                ServicePriority.Normal
        );
    }
}
