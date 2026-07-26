package br.net.silvioluizsilva.pluginbase.logging;

import br.net.silvioluizsilva.pluginbase.config.LoggingConfig;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fachada central de logs com filtragem de informações sensíveis.
 */
public final class PluginLogger {

    private final Logger delegate;
    private volatile LoggingConfig config;

    /**
     * Cria um logger seguro.
     *
     * @param delegate logger fornecido pelo Paper
     * @param config configuração inicial
     */
    public PluginLogger(Logger delegate, LoggingConfig config) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Atualiza as opções do logger após uma recarga válida.
     *
     * @param config nova configuração validada
     */
    public void configure(LoggingConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Registra uma mensagem informativa.
     *
     * @param message mensagem com placeholders SLF4J
     * @param arguments argumentos da mensagem
     */
    public void info(String message, Object... arguments) {
        delegate.info(sanitizeMessage(message), sanitizeArguments(arguments));
    }

    /**
     * Registra um alerta operacional.
     *
     * @param message mensagem com placeholders SLF4J
     * @param arguments argumentos da mensagem
     */
    public void warn(String message, Object... arguments) {
        delegate.warn(sanitizeMessage(message), sanitizeArguments(arguments));
    }

    /**
     * Registra uma mensagem de diagnóstico somente quando o modo debug está ativo.
     *
     * @param message mensagem com placeholders SLF4J
     * @param arguments argumentos da mensagem
     */
    public void debug(String message, Object... arguments) {
        if (config.debug()) {
            delegate.debug(sanitizeMessage(message), sanitizeArguments(arguments));
        }
    }

    /**
     * Registra uma falha sem expor detalhes internos quando stack traces estão desativados.
     *
     * @param message descrição segura da operação
     * @param throwable falha capturada
     */
    public void error(String message, Throwable throwable) {
        String safeMessage = sanitizeMessage(message);
        if (config.includeStackTraces()) {
            delegate.error(safeMessage, throwable);
            return;
        }
        delegate.error("{} Tipo: {}.", safeMessage, throwable.getClass().getSimpleName());
    }

    /**
     * Registra um erro sem exceção associada.
     *
     * @param message mensagem com placeholders SLF4J
     * @param arguments argumentos da mensagem
     */
    public void error(String message, Object... arguments) {
        delegate.error(sanitizeMessage(message), sanitizeArguments(arguments));
    }

    private String sanitizeMessage(String message) {
        return SensitiveDataSanitizer.sanitize(Objects.requireNonNull(message, "message"));
    }

    private Object[] sanitizeArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return new Object[0];
        }
        return Arrays.stream(arguments)
                .map(SensitiveDataSanitizer::sanitizeArgument)
                .toArray(Object[]::new);
    }
}
