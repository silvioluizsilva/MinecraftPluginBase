package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

/**
 * Carrega e publica configurações somente depois de validá-las integralmente.
 */
public final class ConfigManager {

    private final PluginBase plugin;
    private PluginConfig current;

    /**
     * Cria o gerenciador de configurações.
     *
     * @param plugin instância principal do plugin
     */
    public ConfigManager(PluginBase plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Carrega e valida a configuração atualmente mantida pelo Bukkit.
     *
     * @return nova configuração tipada
     */
    public PluginConfig load() {
        PluginConfig candidate = parse(plugin.getConfig());
        current = candidate;
        return candidate;
    }

    /**
     * Recarrega o arquivo e mantém a configuração tipada anterior se a nova for inválida.
     *
     * @return nova configuração validada
     */
    public PluginConfig reload() {
        PluginConfig candidate = reloadCandidate();
        publish(candidate);
        return candidate;
    }

    /**
     * Recarrega e valida o arquivo sem alterar a configuração tipada ativa.
     *
     * @return configuração candidata validada
     */
    public PluginConfig reloadCandidate() {
        plugin.reloadConfig();
        return parse(plugin.getConfig());
    }

    /**
     * Publica uma configuração previamente validada.
     *
     * @param config configuração que passará a ser ativa
     */
    public void publish(PluginConfig config) {
        current = Objects.requireNonNull(config, "config");
    }

    /**
     * Retorna a última configuração validada.
     *
     * @return configuração ativa
     * @throws IllegalStateException quando nenhuma configuração foi carregada
     */
    public PluginConfig current() {
        if (current == null) {
            throw new IllegalStateException("A configuração ainda não foi carregada.");
        }
        return current;
    }

    /**
     * Converte uma configuração YAML em modelos tipados.
     *
     * @param source configuração de origem
     * @return configuração tipada validada
     */
    public static PluginConfig parse(FileConfiguration source) {
        Objects.requireNonNull(source, "source");
        PoolConfig pool = new PoolConfig(
                source.getInt("database.pool.maximum-size", 10),
                source.getInt("database.pool.minimum-idle", 2),
                source.getLong("database.pool.connection-timeout-ms", 10_000L)
        );
        DatabaseConfig database = new DatabaseConfig(
                source.getBoolean("database.enabled", false),
                stringValue(source, "database.host", "127.0.0.1"),
                source.getInt("database.port", 3306),
                stringValue(source, "database.name", "pluginbase"),
                stringValue(source, "database.username", "pluginbase"),
                stringValue(source, "database.password", ""),
                stringValue(source, "database.parameters", "useUnicode=true&characterEncoding=UTF-8&useSSL=true&serverTimezone=UTC"),
                pool,
                source.getBoolean("database.degraded-mode", true),
                new HealthCheckConfig(
                        source.getInt("database.health-check.interval-seconds", 30),
                        source.getInt("database.health-check.timeout-seconds", 3)
                ),
                new ReconnectConfig(
                        source.getInt("database.reconnect.initial-delay-seconds", 60),
                        source.getInt("database.reconnect.increment-seconds", 60),
                        source.getInt("database.reconnect.maximum-delay-seconds", 900),
                        source.getInt("database.reconnect.jitter-percent", 20)
                ),
                new TransactionConfig(
                        source.getInt("database.transactions.statement-timeout-seconds", 30),
                        source.getLong("database.transactions.slow-warning-ms", 1_000L),
                        source.getLong("database.transactions.critical-warning-ms", 5_000L),
                        source.getInt("database.transactions.maximum-concurrent-per-consumer", 2),
                        source.getLong("database.transactions.concurrency-wait-ms", 2_000L)
                )
        );
        return new PluginConfig(
                stringValue(source, "language", "pt_BR"),
                database,
                new LoggingConfig(
                        source.getBoolean("logging.debug", false),
                        source.getBoolean("logging.include-stack-traces", false)
                ),
                new WebConfig(
                        source.getBoolean("web.enabled", false),
                        stringValue(source, "web.bind-address", "127.0.0.1"),
                        source.getInt("web.port", 8080),
                        source.getStringList("web.allowed-origins"),
                        source.getLong("web.maximum-request-body-bytes", 65_536L),
                        stringValue(source, "web.authentication.token-sha256", "")
                )
        );
    }

    private static String stringValue(FileConfiguration source, String path, String defaultValue) {
        return Objects.requireNonNullElse(source.getString(path, defaultValue), defaultValue);
    }
}
