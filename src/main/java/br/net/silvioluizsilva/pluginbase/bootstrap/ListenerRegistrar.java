package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import org.bukkit.event.Listener;
import br.net.silvioluizsilva.pluginbase.listener.ServerLifecycleListener;

import java.util.List;
import java.util.Objects;

/**
 * Centraliza a declaração e o registro dos listeners do plugin.
 */
public final class ListenerRegistrar {

    private ListenerRegistrar() {
    }

    /**
     * Registra todos os listeners declarados para esta versão.
     *
     * @param plugin instância principal
     * @param context contexto compartilhado
     */
    public static void register(PluginBase plugin, PluginContext context) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(context, "context");
        List<Listener> listeners = createListeners(context);
        listeners.forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));
        context.logger().debug("{} listener(s) registrado(s).", listeners.size());
    }

    private static List<Listener> createListeners(PluginContext context) {
        return List.of(new ServerLifecycleListener(context));
    }
}
