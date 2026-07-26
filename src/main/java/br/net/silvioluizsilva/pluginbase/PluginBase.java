package br.net.silvioluizsilva.pluginbase;

import br.net.silvioluizsilva.pluginbase.command.CommandRegistrar;
import br.net.silvioluizsilva.pluginbase.bootstrap.PluginContext;
import br.net.silvioluizsilva.pluginbase.bootstrap.ComponentRegistrar;
import br.net.silvioluizsilva.pluginbase.bootstrap.DefaultPluginContext;
import br.net.silvioluizsilva.pluginbase.bootstrap.ApiRegistrar;
import br.net.silvioluizsilva.pluginbase.config.ConfigManager;
import br.net.silvioluizsilva.pluginbase.config.PluginConfig;
import br.net.silvioluizsilva.pluginbase.database.DatabaseManager;
import br.net.silvioluizsilva.pluginbase.language.LanguageManager;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import br.net.silvioluizsilva.pluginbase.service.ServiceRegistry;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;


/**
 * Ponto de entrada do PluginBase.
 *
 * @author Sílvio Luiz da Silva
 * @version 0.0.1
 */
public final class PluginBase extends JavaPlugin {

    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private PluginLogger pluginLogger;
    private ServiceRegistry serviceRegistry;
    private PluginContext pluginContext;

    /**
     * Registra os componentes que dependem do ciclo de vida do Paper.
     */
    @Override
    public void onLoad() {
        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> CommandRegistrar.register(this, event.registrar())
        );
    }

    /**
     * Inicializa configurações, idiomas e infraestrutura do plugin.
     */
    @Override
    public void onEnable() {
        pluginLogger = new PluginLogger(getSLF4JLogger(), new br.net.silvioluizsilva.pluginbase.config.LoggingConfig(false, false));
        saveDefaultConfig();
        saveBundledResource("messages.yml");
        saveBundledResource("languages/pt_BR.yml");
        saveBundledResource("languages/en_US.yml");

        configManager = new ConfigManager(this);
        PluginConfig pluginConfig;
        try {
            pluginConfig = configManager.load();
        } catch (RuntimeException exception) {
            pluginLogger.error("Configuração inválida: {}", exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginLogger.configure(pluginConfig.logging());

        languageManager = new LanguageManager(this);
        languageManager.load(pluginConfig.language());

        databaseManager = new DatabaseManager(this, pluginConfig.database());
        try {
            databaseManager.startAsync();
        } catch (RuntimeException exception) {
            pluginLogger.error("Não foi possível inicializar o banco de dados.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        serviceRegistry = new ServiceRegistry(pluginLogger);
        pluginContext = new DefaultPluginContext(
                configManager,
                databaseManager,
                languageManager,
                pluginLogger,
                serviceRegistry
        );
        try {
            ComponentRegistrar.registerAll(this, pluginContext);
            serviceRegistry.startAll();
            ApiRegistrar.register(this, pluginContext);
        } catch (RuntimeException exception) {
            pluginLogger.error("Não foi possível inicializar os serviços.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginLogger.info("PluginBase {} habilitado.", getPluginMeta().getVersion());
        pluginLogger.debug("Modo de diagnóstico habilitado.");
    }

    /**
     * Encerra os recursos mantidos pelo plugin.
     */
    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (serviceRegistry != null) {
            serviceRegistry.stopAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (pluginLogger != null) {
            pluginLogger.info("PluginBase desabilitado.");
        }
    }

    /**
     * Recarrega as configurações e o idioma ativo.
     */
    public void reloadPluginConfiguration() {
        PluginConfig previous = configManager.current();
        PluginConfig candidate = configManager.reloadCandidate();
        YamlConfiguration candidateLanguage = languageManager.prepare(candidate.language());
        try {
            databaseManager.reconfigureAsync(candidate.database());
            serviceRegistry.reloadAll(candidate);
            pluginLogger.configure(candidate.logging());
            languageManager.activate(candidateLanguage);
            configManager.publish(candidate);
        } catch (RuntimeException exception) {
            restoreConfiguration(previous, exception);
            throw exception;
        }
    }

    private void restoreConfiguration(PluginConfig previous, RuntimeException original) {
        try {
            databaseManager.reconfigureAsync(previous.database());
            serviceRegistry.reloadAll(previous);
            pluginLogger.configure(previous.logging());
            languageManager.load(previous.language());
        } catch (RuntimeException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    /**
     * Retorna o gerenciador de idiomas.
     *
     * @return gerenciador de idiomas ativo
     * @throws IllegalStateException quando o plugin ainda não foi habilitado
     */
    public LanguageManager getLanguageManager() {
        if (languageManager == null) {
            throw new IllegalStateException("O gerenciador de idiomas ainda não foi inicializado.");
        }
        return languageManager;
    }

    /**
     * Retorna o gerenciador de configurações tipadas.
     *
     * @return gerenciador de configurações ativo
     * @throws IllegalStateException quando o plugin ainda não foi habilitado
     */
    public ConfigManager getConfigManager() {
        if (configManager == null) {
            throw new IllegalStateException("O gerenciador de configurações ainda não foi inicializado.");
        }
        return configManager;
    }

    /**
     * Retorna o gerenciador de conexões e transações.
     *
     * @return gerenciador de banco de dados ativo
     * @throws IllegalStateException quando o plugin ainda não foi habilitado
     */
    public DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            throw new IllegalStateException("O gerenciador de banco de dados ainda não foi inicializado.");
        }
        return databaseManager;
    }

    /**
     * Retorna a fachada segura de logs do plugin.
     *
     * @return logger central ativo
     * @throws IllegalStateException quando o plugin ainda não foi habilitado
     */
    public PluginLogger getPluginLogger() {
        if (pluginLogger == null) {
            throw new IllegalStateException("O logger do plugin ainda não foi inicializado.");
        }
        return pluginLogger;
    }

    /**
     * Retorna o contexto compartilhado da aplicação.
     *
     * @return contexto ativo
     * @throws IllegalStateException quando o plugin ainda não foi habilitado
     */
    public PluginContext getPluginContext() {
        if (pluginContext == null) {
            throw new IllegalStateException("O contexto do plugin ainda não foi inicializado.");
        }
        return pluginContext;
    }

    private void saveBundledResource(String path) {
        if (!new java.io.File(getDataFolder(), path).isFile()) {
            saveResource(path, false);
        }
    }
}
