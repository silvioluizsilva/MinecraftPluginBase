package br.net.silvioluizsilva.pluginbase.exception;

/**
 * Indica que uma configuração não atende aos requisitos do plugin.
 */
public final class ConfigurationException extends RuntimeException {

    /**
     * Cria uma exceção de configuração.
     *
     * @param message descrição segura do problema
     */
    public ConfigurationException(String message) {
        super(message);
    }
}
