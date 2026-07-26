package br.net.silvioluizsilva.pluginbase.command;

/**
 * Centraliza os nós de permissão usados pelos comandos.
 */
public final class CommandPermissions {

    /** Permite recarregar configurações e serviços. */
    public static final String RELOAD = "pluginbase.command.reload";

    /** Permite consultar o estado operacional detalhado. */
    public static final String STATUS = "pluginbase.command.status";

    /** Permite consultar a saúde do banco. */
    public static final String DATABASE_HEALTH = "pluginbase.command.database.health";

    /** Permite solicitar uma reconexão imediata. */
    public static final String DATABASE_RECONNECT = "pluginbase.command.database.reconnect";

    private CommandPermissions() {
    }
}
