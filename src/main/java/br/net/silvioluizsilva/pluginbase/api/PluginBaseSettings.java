package br.net.silvioluizsilva.pluginbase.api;

/**
 * Visão pública e segura da configuração ativa.
 *
 * @param language idioma ativo
 * @param databaseEnabled indica se o banco foi habilitado
 * @param databaseAvailable indica se o pool está conectado
 * @param debugEnabled indica se logs de diagnóstico estão ativos
 */
public record PluginBaseSettings(
        String language,
        boolean databaseEnabled,
        boolean databaseAvailable,
        boolean debugEnabled
) {
}
