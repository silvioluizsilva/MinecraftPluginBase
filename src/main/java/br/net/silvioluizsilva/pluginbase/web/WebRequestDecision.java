package br.net.silvioluizsilva.pluginbase.web;

/**
 * Resultado da política aplicada antes de um futuro controlador web.
 */
public enum WebRequestDecision {
    /** Requisição aceita pela política preliminar. */
    ALLOWED,
    /** Origem ausente ou não autorizada. */
    ORIGIN_DENIED,
    /** Corpo excede o limite configurado. */
    PAYLOAD_TOO_LARGE,
    /** Credencial ausente ou inválida. */
    UNAUTHORIZED
}
