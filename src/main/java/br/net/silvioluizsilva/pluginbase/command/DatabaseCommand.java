package br.net.silvioluizsilva.pluginbase.command;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import br.net.silvioluizsilva.pluginbase.api.DatabaseHealth;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Constrói os comandos administrativos do banco. */
public final class DatabaseCommand {

    private DatabaseCommand() {
    }

    /**
     * Cria a árvore {@code /pluginbase database}.
     *
     * @param plugin plugin principal
     * @param handler tratador de falhas
     * @return árvore Brigadier
     */
    public static LiteralArgumentBuilder<CommandSourceStack> create(
            PluginBase plugin,
            CommandExceptionHandler handler
    ) {
        Objects.requireNonNull(plugin, "plugin");
        return Commands.literal("database")
                .then(Commands.literal("health")
                        .requires(source -> source.getSender().hasPermission(CommandPermissions.DATABASE_HEALTH))
                        .executes(context -> handler.execute(context,
                                () -> sendHealth(plugin, context.getSource()))))
                .then(Commands.literal("reconnect")
                        .requires(source -> source.getSender().hasPermission(CommandPermissions.DATABASE_RECONNECT))
                        .executes(context -> handler.execute(context,
                                () -> reconnect(plugin, context.getSource()))));
    }

    private static int sendHealth(PluginBase plugin, CommandSourceStack source) {
        DatabaseHealth health = plugin.getDatabaseManager().health();
        Instant now = Instant.now();
        String state = plugin.getLanguageManager().text(
                "database.state." + health.state().name().toLowerCase(Locale.ROOT)
        );
        long nextSeconds = health.nextReconnectAt()
                .map(next -> Math.max(0L, Duration.between(now, next).toSeconds())).orElse(0L);
        plugin.getLanguageManager().send(source.getSender(), "command.database.health",
                "{state}", state,
                "{attempt}", Long.toString(health.reconnectAttempt()),
                "{next}", Long.toString(nextSeconds),
                "{unavailable}", Long.toString(health.unavailableFor(now).toSeconds()),
                "{last-connected}", health.lastConnectedAt().map(Instant::toString).orElse("-"),
                "{last-failure}", health.lastFailureAt().map(Instant::toString).orElse("-"));
        return 1;
    }

    private static int reconnect(PluginBase plugin, CommandSourceStack source) {
        boolean accepted = plugin.getDatabaseManager().requestReconnect();
        plugin.getLanguageManager().send(source.getSender(), accepted
                ? "command.database.reconnect-accepted" : "command.database.reconnect-rejected");
        return accepted ? 1 : 0;
    }
}
