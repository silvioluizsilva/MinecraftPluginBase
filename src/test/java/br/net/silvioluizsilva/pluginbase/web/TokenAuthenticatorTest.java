package br.net.silvioluizsilva.pluginbase.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica autenticação sem armazenamento do token original.
 */
final class TokenAuthenticatorTest {

    private static final String HASH = "930bbdc51b6aed5c2a5678fd6e28dee7a05e8a4b643cfc0b4427c3efb86c0d94";

    @Test
    void shouldAuthenticateMatchingBearerToken() {
        TokenAuthenticator authenticator = new TokenAuthenticator(HASH);

        assertEquals(AuthenticationResult.AUTHENTICATED, authenticator.authenticate("Bearer secret-token"));
    }

    @Test
    void shouldRejectMissingOrInvalidToken() {
        TokenAuthenticator authenticator = new TokenAuthenticator(HASH);

        assertEquals(AuthenticationResult.MISSING, authenticator.authenticate(null));
        assertEquals(AuthenticationResult.INVALID, authenticator.authenticate("Basic secret-token"));
        assertEquals(AuthenticationResult.INVALID, authenticator.authenticate("Bearer wrong-token"));
    }
}
