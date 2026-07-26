package br.net.silvioluizsilva.pluginbase.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica a remoção de segredos em formatos comuns.
 */
final class SensitiveDataSanitizerTest {

    @Test
    void shouldRedactKeyValueSecrets() {
        String result = SensitiveDataSanitizer.sanitize("username=pluginbase password=very-secret token:abc123");

        assertTrue(result.contains("username=pluginbase"));
        assertFalse(result.contains("very-secret"));
        assertFalse(result.contains("abc123"));
    }

    @Test
    void shouldRedactJdbcCredentialsAndBearerToken() {
        String result = SensitiveDataSanitizer.sanitize(
                "jdbc:mysql://admin:secret@localhost/pluginbase Authorization: Bearer ey.secret.token"
        );

        assertFalse(result.contains("admin:secret"));
        assertFalse(result.contains("ey.secret.token"));
        assertTrue(result.contains("[REDACTED]"));
    }

    @Test
    void shouldRedactCharacterAndByteArrays() {
        assertTrue("[REDACTED]".equals(SensitiveDataSanitizer.sanitizeArgument("secret".toCharArray())));
        assertTrue("[REDACTED]".equals(SensitiveDataSanitizer.sanitizeArgument(new byte[]{1, 2, 3})));
    }
}
