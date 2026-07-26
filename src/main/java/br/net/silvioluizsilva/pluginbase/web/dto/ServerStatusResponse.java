package br.net.silvioluizsilva.pluginbase.web.dto;

import java.time.Instant;

/**
 * Representa o estado público do servidor.
 *
 * @param status estado operacional
 * @param pluginVersion versão instalada
 * @param apiVersion versão do contrato web
 * @param timestamp instante UTC da resposta
 */
public record ServerStatusResponse(
        String status,
        String pluginVersion,
        String apiVersion,
        Instant timestamp
) {
}
