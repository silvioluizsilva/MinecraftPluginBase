package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.config.ConfigManager;
import br.net.silvioluizsilva.pluginbase.database.DatabaseManager;
import br.net.silvioluizsilva.pluginbase.language.LanguageManager;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import br.net.silvioluizsilva.pluginbase.service.ServiceRegistry;

/**
 * Expõe aos componentes internos as dependências compartilhadas da aplicação.
 */
public interface PluginContext {

    /**
     * Retorna as configurações validadas.
     *
     * @return gerenciador de configurações
     */
    ConfigManager configManager();

    /**
     * Retorna a infraestrutura de persistência.
     *
     * @return gerenciador de banco de dados
     */
    DatabaseManager databaseManager();

    /**
     * Retorna o sistema de mensagens traduzidas.
     *
     * @return gerenciador de idiomas
     */
    LanguageManager languageManager();

    /**
     * Retorna o logger seguro.
     *
     * @return logger central
     */
    PluginLogger logger();

    /**
     * Retorna o registro de serviços.
     *
     * @return registro central
     */
    ServiceRegistry services();
}
