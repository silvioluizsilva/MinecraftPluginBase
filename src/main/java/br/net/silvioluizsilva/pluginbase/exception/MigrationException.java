package br.net.silvioluizsilva.pluginbase.exception;

/**
 * Indica que a evolução do esquema não pôde ser concluída com segurança.
 */
public final class MigrationException extends DatabaseException {

    /**
     * Cria uma exceção de migração.
     *
     * @param message descrição segura da falha
     */
    public MigrationException(String message) {
        super(message);
    }

    /**
     * Cria uma exceção de migração com a causa original.
     *
     * @param message descrição segura da falha
     * @param cause causa original
     */
    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
