package br.net.silvioluizsilva.pluginbase.command;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.api.StatusService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Constrói o comando de diagnóstico operacional.
 */
public final class StatusCommand {

    private static final List<String> COMPONENTS = List.of("database", "services");

    private StatusCommand() {
    }

    /**
     * Cria a árvore do subcomando {@code status}.
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
        return Commands.literal("status")
                .requires(source -> source.getSender().hasPermission(CommandPermissions.STATUS))
                .executes(context -> handler.execute(context, () -> sendGeneralStatus(plugin, context.getSource())))
                .then(Commands.argument("component", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            CommandSuggestions.matching(builder.getRemaining(), COMPONENTS).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> handler.execute(context, () -> sendComponentStatus(
                                plugin,
                                context.getSource(),
                                StringArgumentType.getString(context, "component")
                        ))));
    }

    private static int sendGeneralStatus(PluginBase plugin, CommandSourceStack source) {
        StatusService status = plugin.getPluginContext().services().resolve(StatusService.class);
        long uptime = status.startedAt()
                .map(started -> Duration.between(started, Instant.now()).toSeconds())
                .orElse(0L);
        plugin.getLanguageManager().send(
                source.getSender(),
                "command.status.general",
                "{uptime}", Long.toString(Math.max(0L, uptime)),
                "{services}", Integer.toString(plugin.getPluginContext().services().registeredTypes().size())
        );
        return 1;
    }

    private static int sendComponentStatus(PluginBase plugin, CommandSourceStack source, String component) {
        return switch (component.toLowerCase(java.util.Locale.ROOT)) {
            case "database" -> {
                String stateKey = "database.state." + plugin.getDatabaseManager().health().state()
                        .name().toLowerCase(java.util.Locale.ROOT);
                String state = plugin.getLanguageManager().text(stateKey);
                plugin.getLanguageManager().send(source.getSender(), "command.status.database", "{state}", state);
                yield 1;
            }
            case "services" -> {
                int count = plugin.getPluginContext().services().registeredTypes().size();
                plugin.getLanguageManager().send(
                        source.getSender(), "command.status.services", "{count}", Integer.toString(count)
                );
                yield 1;
            }
            default -> {
                plugin.getLanguageManager().send(source.getSender(), "command.status.unknown", "{component}", component);
                yield 0;
            }
        };
    }
}
