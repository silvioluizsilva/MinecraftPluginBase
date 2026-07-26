package br.net.silvioluizsilva.pluginbase.command;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Objects;

/**
 * Constrói o subcomando responsável pela recarga segura.
 */
public final class ReloadCommand {

    private ReloadCommand() {
    }

    /**
     * Cria a árvore do subcomando {@code reload}.
     *
     * @param plugin instância principal
     * @param handler tratador central de falhas
     * @return árvore Brigadier
     */
    public static LiteralArgumentBuilder<CommandSourceStack> create(
            PluginBase plugin,
            CommandExceptionHandler handler
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(handler, "handler");
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission(CommandPermissions.RELOAD))
                .executes(context -> handler.execute(context, () -> {
                    plugin.reloadPluginConfiguration();
                    plugin.getLanguageManager().send(context.getSource().getSender(), "command.reloaded");
                    return 1;
                }));
    }
}
