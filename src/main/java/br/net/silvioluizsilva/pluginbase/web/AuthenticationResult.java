package br.net.silvioluizsilva.pluginbase.web;

/**
 * Resultado da validação de uma credencial Bearer.
 */
public enum AuthenticationResult {
    /** Credencial válida. */
    AUTHENTICATED,
    /** Cabeçalho ausente ou vazio. */
    MISSING,
    /** Formato ou credencial inválida. */
    INVALID
}
