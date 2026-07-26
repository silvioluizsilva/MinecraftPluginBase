package br.net.silvioluizsilva.pluginbase.config;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Configuração imutável do futuro módulo web.
 *
 * @param enabled indica se a integração web foi autorizada
 * @param bindAddress interface local reservada
 * @param port porta reservada
 * @param allowedOrigins origens exatas permitidas
 * @param maximumRequestBodyBytes tamanho máximo do corpo da requisição
 * @param tokenSha256 hash hexadecimal SHA-256 do token administrativo
 */
public record WebConfig(
        boolean enabled,
        String bindAddress,
        int port,
        List<String> allowedOrigins,
        long maximumRequestBodyBytes,
        String tokenSha256
) {

    private static final Set<String> LOOPBACK_ADDRESSES = Set.of("127.0.0.1", "::1", "localhost");

    /**
     * Normaliza e valida os limites de segurança do módulo web.
     */
    public WebConfig {
        bindAddress = Objects.requireNonNull(bindAddress, "bindAddress").strip().toLowerCase(Locale.ROOT);
        allowedOrigins = List.copyOf(Objects.requireNonNull(allowedOrigins, "allowedOrigins"));
        tokenSha256 = Objects.requireNonNull(tokenSha256, "tokenSha256").strip().toLowerCase(Locale.ROOT);

        if (!LOOPBACK_ADDRESSES.contains(bindAddress)) {
            throw new ConfigurationException("web.bind-address deve apontar para a interface local.");
        }
        if (port < 1_024 || port > 65_535) {
            throw new ConfigurationException("web.port deve estar entre 1024 e 65535.");
        }
        if (maximumRequestBodyBytes < 1_024 || maximumRequestBodyBytes > 1_048_576) {
            throw new ConfigurationException("web.maximum-request-body-bytes deve estar entre 1024 e 1048576.");
        }
        allowedOrigins.forEach(WebConfig::validateOrigin);
        if (enabled && allowedOrigins.isEmpty()) {
            throw new ConfigurationException("Defina ao menos uma origem antes de habilitar o módulo web.");
        }
        if (enabled && !tokenSha256.matches("[a-f0-9]{64}")) {
            throw new ConfigurationException("web.authentication.token-sha256 deve conter um SHA-256 válido.");
        }
        if (!tokenSha256.isEmpty() && !tokenSha256.matches("[a-f0-9]{64}")) {
            throw new ConfigurationException("web.authentication.token-sha256 possui formato inválido.");
        }
    }

    private static void validateOrigin(String origin) {
        if (origin == null || origin.isBlank() || origin.contains("*")) {
            throw new ConfigurationException("web.allowed-origins não aceita valores vazios ou curingas.");
        }
        try {
            URI uri = URI.create(origin);
            boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                throw new ConfigurationException("Origem web inválida: " + origin + ".");
            }
        } catch (IllegalArgumentException exception) {
            throw new ConfigurationException("Origem web inválida: " + origin + ".");
        }
    }
}
