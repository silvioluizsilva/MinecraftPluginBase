package br.net.silvioluizsilva.pluginbase.command;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Objects;

/**
 * Converte falhas de comandos em mensagens seguras e localizadas.
 */
public final class CommandExceptionHandler {

    private final PluginBase plugin;

    /**
     * Cria o tratador central.
     *
     * @param plugin instância principal
     */
    public CommandExceptionHandler(PluginBase plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Executa uma ação e trata suas falhas sem expor detalhes internos ao jogador.
     *
     * @param context contexto Brigadier
     * @param action ação protegida
     * @return código de resultado
     */
    public int execute(CommandContext<CommandSourceStack> context, CommandAction action) {
        try {
            return action.execute();
        } catch (ConfigurationException exception) {
            plugin.getPluginLogger().warn("Recarga rejeitada: {}", exception.getMessage());
            plugin.getLanguageManager().send(
                    context.getSource().getSender(),
                    "command.reload-failed",
                    "{reason}", exception.getMessage()
            );
        } catch (Exception exception) {
            plugin.getPluginLogger().error("Falha inesperada ao executar um comando.", exception);
            plugin.getLanguageManager().send(context.getSource().getSender(), "command.internal-error");
        }
        return 0;
    }
}
