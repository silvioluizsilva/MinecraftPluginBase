package br.net.silvioluizsilva.pluginbase.web.dto;

/**
 * Configurações que podem ser exibidas pela futura interface administrativa.
 *
 * @param language idioma ativo
 * @param databaseEnabled indica se a persistência está habilitada
 * @param debugEnabled indica se o modo de diagnóstico está ativo
 */
public record PublicSettingsResponse(String language, boolean databaseEnabled, boolean debugEnabled) {
}
