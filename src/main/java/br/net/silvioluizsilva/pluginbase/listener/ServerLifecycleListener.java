package br.net.silvioluizsilva.pluginbase.listener;

import br.net.silvioluizsilva.pluginbase.bootstrap.PluginContext;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

import java.util.Objects;

/**
 * Observa eventos gerais do servidor sem conter regras de negócio.
 */
public final class ServerLifecycleListener implements Listener {

    private final PluginContext context;

    /**
     * Cria o listener de ciclo do servidor.
     *
     * @param context contexto compartilhado
     */
    public ServerLifecycleListener(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * Registra em debug quando o carregamento do servidor termina.
     *
     * @param event evento emitido pelo Paper
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        context.logger().debug("Servidor carregado. Tipo: {}.", event.getType());
    }
}
