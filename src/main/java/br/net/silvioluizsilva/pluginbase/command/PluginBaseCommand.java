package br.net.silvioluizsilva.pluginbase.command;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Objects;

/**
 * Compõe a árvore raiz do comando PluginBase.
 */
public final class PluginBaseCommand {

    private PluginBaseCommand() {
    }

    /**
     * Cria a árvore completa do comando {@code /pluginbase}.
     *
     * @param plugin instância principal
     * @return nó raiz compilado
     */
    public static LiteralCommandNode<CommandSourceStack> create(PluginBase plugin) {
        Objects.requireNonNull(plugin, "plugin");
        CommandExceptionHandler handler = new CommandExceptionHandler(plugin);
        return Commands.literal("pluginbase")
                .executes(context -> handler.execute(context, () -> {
                    plugin.getLanguageManager().send(
                            context.getSource().getSender(),
                            "command.info",
                            "{version}", plugin.getPluginMeta().getVersion()
                    );
                    return 1;
                }))
                .then(ReloadCommand.create(plugin, handler))
                .then(StatusCommand.create(plugin, handler))
                .then(DatabaseCommand.create(plugin, handler))
                .build();
    }
}
