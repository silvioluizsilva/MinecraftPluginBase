package br.net.silvioluizsilva.pluginbase.web;

import br.net.silvioluizsilva.pluginbase.config.WebConfig;

import java.util.Objects;

/**
 * Aplica limites básicos antes que uma requisição alcance regras de negócio.
 */
public final class WebRequestGuard {

    private final WebConfig config;
    private final TokenAuthenticator authenticator;

    /**
     * Cria uma política para uma configuração web habilitada.
     *
     * @param config configuração validada
     */
    public WebRequestGuard(WebConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        if (!config.enabled()) {
            throw new IllegalArgumentException("A política web exige configuração habilitada.");
        }
        this.authenticator = new TokenAuthenticator(config.tokenSha256());
    }

    /**
     * Avalia origem, tamanho declarado e autenticação, nesta ordem.
     *
     * @param origin valor exato do cabeçalho Origin
     * @param contentLength tamanho declarado do corpo
     * @param authorizationHeader cabeçalho Authorization
     * @return decisão preliminar
     */
    public WebRequestDecision evaluate(String origin, long contentLength, String authorizationHeader) {
        if (origin == null || !config.allowedOrigins().contains(origin)) {
            return WebRequestDecision.ORIGIN_DENIED;
        }
        if (contentLength < 0 || contentLength > config.maximumRequestBodyBytes()) {
            return WebRequestDecision.PAYLOAD_TOO_LARGE;
        }
        if (authenticator.authenticate(authorizationHeader) != AuthenticationResult.AUTHENTICATED) {
            return WebRequestDecision.UNAUTHORIZED;
        }
        return WebRequestDecision.ALLOWED;
    }
}
