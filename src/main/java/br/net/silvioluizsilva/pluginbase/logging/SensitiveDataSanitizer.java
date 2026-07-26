package br.net.silvioluizsilva.pluginbase.logging;

import java.util.regex.Pattern;

/**
 * Remove credenciais conhecidas de textos destinados aos logs.
 */
public final class SensitiveDataSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(password|passwd|pwd|token|secret|api[-_]?key|authorization)\\s*([:=])\\s*([^\\s,;&]+)"
    );
    private static final Pattern JDBC_CREDENTIALS = Pattern.compile(
            "(?i)(jdbc:mysql://)([^:@/\\s]+):([^@/\\s]+)@"
    );
    private static final Pattern URL_QUERY_SECRET = Pattern.compile(
            "(?i)([?&](?:password|passwd|pwd|token|secret|api[-_]?key)=)([^&\\s]+)"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)(bearer\\s+)([A-Za-z0-9._~+/-]+=*)"
    );

    private SensitiveDataSanitizer() {
    }

    /**
     * Mascara dados sensíveis presentes em um texto.
     *
     * @param value texto original
     * @return texto seguro para registro, ou {@code null}
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = BEARER_TOKEN.matcher(value).replaceAll("$1" + REDACTED);
        sanitized = JDBC_CREDENTIALS.matcher(sanitized).replaceAll("$1" + REDACTED + ":" + REDACTED + "@");
        sanitized = KEY_VALUE_SECRET.matcher(sanitized).replaceAll("$1$2" + REDACTED);
        sanitized = URL_QUERY_SECRET.matcher(sanitized).replaceAll("$1" + REDACTED);
        return sanitized;
    }

    /**
     * Converte e mascara um argumento antes de enviá-lo ao backend de logs.
     *
     * @param value argumento original
     * @return argumento seguro
     */
    public static Object sanitizeArgument(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof char[] || value instanceof byte[]) {
            return REDACTED;
        }
        return sanitize(String.valueOf(value));
    }
}
