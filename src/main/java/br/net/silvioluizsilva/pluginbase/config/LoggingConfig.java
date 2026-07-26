package br.net.silvioluizsilva.pluginbase.config;

/**
 * Configuração imutável de logs.
 *
 * @param debug indica se mensagens de diagnóstico estão habilitadas
 * @param includeStackTraces indica se exceções completas aparecem no console
 */
public record LoggingConfig(boolean debug, boolean includeStackTraces) {
}
