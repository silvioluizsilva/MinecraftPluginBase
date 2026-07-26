package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.config.ConfigManager;
import br.net.silvioluizsilva.pluginbase.database.DatabaseManager;
import br.net.silvioluizsilva.pluginbase.language.LanguageManager;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import br.net.silvioluizsilva.pluginbase.service.ServiceRegistry;

import java.util.Objects;

/**
 * Implementação imutável do contexto compartilhado do plugin.
 *
 * @param configManager gerenciador de configurações
 * @param databaseManager gerenciador do banco
 * @param languageManager gerenciador de idiomas
 * @param logger logger seguro
 * @param services registro de serviços
 */
public record DefaultPluginContext(
        ConfigManager configManager,
        DatabaseManager databaseManager,
        LanguageManager languageManager,
        PluginLogger logger,
        ServiceRegistry services
) implements PluginContext {

    /**
     * Garante que o contexto nunca seja criado com dependências ausentes.
     */
    public DefaultPluginContext {
        configManager = Objects.requireNonNull(configManager, "configManager");
        databaseManager = Objects.requireNonNull(databaseManager, "databaseManager");
        languageManager = Objects.requireNonNull(languageManager, "languageManager");
        logger = Objects.requireNonNull(logger, "logger");
        services = Objects.requireNonNull(services, "services");
    }
}
