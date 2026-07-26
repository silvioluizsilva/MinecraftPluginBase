package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Configuração imutável da conexão MySQL.
 *
 * @param enabled indica se a persistência está ativa
 * @param host endereço do servidor MySQL
 * @param port porta do servidor MySQL
 * @param name nome do banco de dados
 * @param username usuário restrito do plugin
 * @param password senha do usuário
 * @param parameters parâmetros adicionais da conexão
 * @param pool configuração do pool
 */
public record DatabaseConfig(
        boolean enabled,
        String host,
        int port,
        String name,
        String username,
        String password,
        String parameters,
        PoolConfig pool,
        boolean degradedMode,
        HealthCheckConfig healthCheck,
        ReconnectConfig reconnect,
        TransactionConfig transactions
) {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]{1,64}");
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9._:-]{1,253}");

    /**
     * Normaliza e valida os dados da conexão.
     */
    public DatabaseConfig {
        host = requireText(host, "database.host");
        name = requireIdentifier(name, "database.name");
        username = requireIdentifier(username, "database.username");
        password = Objects.requireNonNull(password, "password");
        parameters = requireText(parameters, "database.parameters");
        pool = Objects.requireNonNull(pool, "pool");
        healthCheck = Objects.requireNonNull(healthCheck, "healthCheck");
        reconnect = Objects.requireNonNull(reconnect, "reconnect");
        transactions = Objects.requireNonNull(transactions, "transactions");

        if (!HOST.matcher(host).matches()) {
            throw new ConfigurationException("database.host contém caracteres inválidos.");
        }
        if (port < 1 || port > 65_535) {
            throw new ConfigurationException("database.port deve estar entre 1 e 65535.");
        }
        if (enabled && (password.isBlank() || "change-me".equals(password))) {
            throw new ConfigurationException("Defina uma senha segura antes de habilitar o banco de dados.");
        }
    }

    /**
     * Cria uma configuração usando os padrões operacionais atuais.
     *
     * @param enabled persistência ativa
     * @param host servidor MySQL
     * @param port porta MySQL
     * @param name banco de dados
     * @param username usuário
     * @param password senha
     * @param parameters parâmetros JDBC
     * @param pool configuração do pool
     */
    public DatabaseConfig(boolean enabled, String host, int port, String name, String username,
                          String password, String parameters, PoolConfig pool) {
        this(enabled, host, port, name, username, password, parameters, pool, true,
                new HealthCheckConfig(30, 3), new ReconnectConfig(60, 60, 900, 20),
                new TransactionConfig(30, 1_000L, 5_000L, 2, 2_000L));
    }

    /**
     * Monta a URL JDBC validada.
     *
     * @return URL para o driver MySQL
     */
    public String jdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + name + "?" + parameters;
    }

    private static String requireIdentifier(String value, String path) {
        String text = requireText(value, path);
        if (!IDENTIFIER.matcher(text).matches()) {
            throw new ConfigurationException(path + " deve conter apenas letras, números e sublinhado.");
        }
        return text;
    }

    private static String requireText(String value, String path) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(path + " não pode estar vazio.");
        }
        return value.strip();
    }
}
