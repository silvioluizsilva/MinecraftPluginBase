package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.PluginBase;

import java.util.Objects;

/**
 * Ponto único de composição dos componentes da aplicação.
 */
public final class ComponentRegistrar {

    private ComponentRegistrar() {
    }

    /**
     * Registra serviços e adaptadores na ordem correta.
     *
     * @param plugin instância principal
     * @param context contexto compartilhado
     */
    public static void registerAll(PluginBase plugin, PluginContext context) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(context, "context");
        ServiceRegistrar.register(plugin, context);
        ListenerRegistrar.register(plugin, context);
    }
}
