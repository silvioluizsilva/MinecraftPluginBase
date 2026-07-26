package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.api.StatusService;
import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.api.TaskScheduler;
import br.net.silvioluizsilva.pluginbase.service.DefaultStatusService;
import br.net.silvioluizsilva.pluginbase.scheduler.PaperTaskScheduler;

import java.util.Objects;

/**
 * Declara os serviços que compõem a aplicação.
 */
public final class ServiceRegistrar {

    private ServiceRegistrar() {
    }

    /**
     * Registra todos os serviços na ordem de dependência.
     *
     * @param plugin instância principal
     * @param context contexto compartilhado
     */
    public static void register(PluginBase plugin, PluginContext context) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(context, "context");
        context.services().register(StatusService.class, new DefaultStatusService());
        context.services().register(TaskScheduler.class, new PaperTaskScheduler(plugin, context.logger()));
    }
}
