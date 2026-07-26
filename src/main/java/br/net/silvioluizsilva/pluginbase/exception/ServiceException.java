package br.net.silvioluizsilva.pluginbase.exception;

/**
 * Indica uma falha no registro ou no ciclo de vida dos serviços.
 */
public final class ServiceException extends RuntimeException {

    /**
     * Cria uma exceção de serviço.
     *
     * @param message descrição da falha
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Cria uma exceção de serviço com a causa original.
     *
     * @param message descrição da falha
     * @param cause causa original
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
