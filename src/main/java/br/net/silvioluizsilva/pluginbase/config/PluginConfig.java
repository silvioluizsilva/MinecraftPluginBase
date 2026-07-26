package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Visão tipada e imutável de toda a configuração do plugin.
 *
 * @param language código do idioma ativo
 * @param database configuração de persistência
 * @param logging configuração de logs
 * @param web configuração reservada para a interface web
 */
public record PluginConfig(String language, DatabaseConfig database, LoggingConfig logging, WebConfig web) {

    private static final Pattern LOCALE = Pattern.compile("[a-z]{2}_[A-Z]{2}");

    /**
     * Valida e normaliza a configuração principal.
     */
    public PluginConfig {
        if (language == null || !LOCALE.matcher(language).matches()) {
            throw new ConfigurationException("language deve seguir o formato pt_BR.");
        }
        String[] parts = language.split("_", 2);
        language = parts[0].toLowerCase(Locale.ROOT) + "_" + parts[1].toUpperCase(Locale.ROOT);
        database = Objects.requireNonNull(database, "database");
        logging = Objects.requireNonNull(logging, "logging");
        web = Objects.requireNonNull(web, "web");
    }
}
