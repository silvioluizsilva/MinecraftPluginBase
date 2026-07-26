package br.net.silvioluizsilva.pluginbase.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Valida tokens Bearer usando somente um hash SHA-256 armazenado.
 */
public final class TokenAuthenticator {

    private static final int MAXIMUM_HEADER_LENGTH = 1_024;
    private final byte[] expectedHash;

    /**
     * Cria um autenticador a partir de um hash hexadecimal validado.
     *
     * @param tokenSha256 hash SHA-256 esperado
     */
    public TokenAuthenticator(String tokenSha256) {
        String normalized = Objects.requireNonNull(tokenSha256, "tokenSha256")
                .strip()
                .toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("O hash do token deve ser um SHA-256 hexadecimal.");
        }
        this.expectedHash = HexFormat.of().parseHex(normalized);
    }

    /**
     * Valida um cabeçalho Authorization sem comparar o token em texto puro.
     *
     * @param authorizationHeader conteúdo integral do cabeçalho
     * @return resultado da autenticação
     */
    public AuthenticationResult authenticate(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return AuthenticationResult.MISSING;
        }
        if (authorizationHeader.length() > MAXIMUM_HEADER_LENGTH
                || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return AuthenticationResult.INVALID;
        }
        String token = authorizationHeader.substring(7).strip();
        if (token.isEmpty()) {
            return AuthenticationResult.INVALID;
        }
        byte[] actualHash = sha256(token);
        return MessageDigest.isEqual(expectedHash, actualHash)
                ? AuthenticationResult.AUTHENTICATED
                : AuthenticationResult.INVALID;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", exception);
        }
    }
}
