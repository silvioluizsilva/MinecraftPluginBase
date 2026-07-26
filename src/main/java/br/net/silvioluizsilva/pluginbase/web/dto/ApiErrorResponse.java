package br.net.silvioluizsilva.pluginbase.web.dto;

/**
 * Resposta de erro sem detalhes internos ou stack traces.
 *
 * @param code código estável e legível por máquina
 * @param message mensagem segura
 * @param requestId identificador para correlação nos logs
 */
public record ApiErrorResponse(String code, String message, String requestId) {
}
