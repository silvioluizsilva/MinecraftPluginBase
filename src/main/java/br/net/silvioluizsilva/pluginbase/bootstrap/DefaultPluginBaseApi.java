package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.api.PluginBaseApi;
import br.net.silvioluizsilva.pluginbase.api.PluginBaseSettings;
import br.net.silvioluizsilva.pluginbase.api.StatusService;
import br.net.silvioluizsilva.pluginbase.api.TaskScheduler;
import br.net.silvioluizsilva.pluginbase.api.DatabaseAccess;
import br.net.silvioluizsilva.pluginbase.config.PluginConfig;
import br.net.silvioluizsilva.pluginbase.service.ManagedService;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.plugin.Plugin;

/**
 * Implementação interna do contrato público do PluginBase.
 */
final class DefaultPluginBaseApi implements PluginBaseApi {

    private final String pluginVersion;
    private final PluginContext context;
    private final ConcurrentMap<String, DatabaseAccess> databaseAccesses = new ConcurrentHashMap<>();

    DefaultPluginBaseApi(String pluginVersion, PluginContext context) {
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion");
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override
    public String pluginVersion() {
        return pluginVersion;
    }

    @Override
    public PluginBaseSettings settings() {
        PluginConfig config = context.configManager().current();
        return new PluginBaseSettings(
                config.language(),
                config.database().enabled(),
                context.databaseManager().isConnected(),
                config.logging().debug()
        );
    }

    @Override
    public StatusService status() {
        return context.services().resolve(StatusService.class);
    }

    @Override
    public TaskScheduler scheduler() {
        return context.services().resolve(TaskScheduler.class);
    }

    @Override
    public DatabaseAccess database(Plugin owner) {
        Objects.requireNonNull(owner, "owner");
        return databaseAccesses.computeIfAbsent(owner.getName(), ignored -> new DefaultDatabaseAccess(context, owner));
    }

    @Override
    public <T extends ManagedService> Optional<T> findService(Class<T> contract) {
        return context.services().find(contract);
    }
}
