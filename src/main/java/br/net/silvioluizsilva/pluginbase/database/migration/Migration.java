package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.exception.MigrationException;

/**
 * Descreve uma migração versionada empacotada no plugin.
 *
 * @param version versão incremental positiva
 * @param description descrição curta
 * @param resourcePath caminho do script no JAR
 */
public record Migration(int version, String description, String resourcePath) {

    /**
     * Valida os metadados da migração.
     */
    public Migration {
        if (version < 1) {
            throw new MigrationException("A versão da migração deve ser positiva.");
        }
        if (description == null || description.isBlank()) {
            throw new MigrationException("A descrição da migração não pode estar vazia.");
        }
        if (resourcePath == null || !resourcePath.matches("sql/[0-9]{3}_[a-z0-9_]+\\.sql")) {
            throw new MigrationException("Caminho inválido para a migração " + version + ".");
        }
    }
}
