package br.net.silvioluizsilva.pluginbase.command;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;
import java.util.Objects;

/**
 * Registra as árvores Brigadier no ciclo de vida do Paper.
 */
public final class CommandRegistrar {

    private CommandRegistrar() {
    }

    /**
     * Registra todos os comandos, descrições e aliases.
     *
     * @param plugin instância principal
     * @param registrar registrador do Paper
     */
    public static void register(PluginBase plugin, Commands registrar) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registrar, "registrar");
        registrar.register(
                PluginBaseCommand.create(plugin),
                "Exibe informações e administra o PluginBase",
                List.of("pbase")
        );
    }
}
