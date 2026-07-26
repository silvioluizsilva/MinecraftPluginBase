package br.net.silvioluizsilva.pluginbase.exception;

/**
 * Indica uma falha controlada na infraestrutura de persistência.
 */
public class DatabaseException extends RuntimeException {

    /**
     * Cria uma exceção de banco de dados.
     *
     * @param message descrição segura da falha
     * @param cause causa original
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Cria uma exceção de banco de dados sem causa associada.
     *
     * @param message descrição segura da falha
     */
    public DatabaseException(String message) {
        super(message);
    }
}
