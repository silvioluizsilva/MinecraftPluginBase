package br.net.silvioluizsilva.pluginbase.api;

/**
 * Define uma migração SQL pertencente a um plugin consumidor.
 *
 * @param version versão incremental positiva
 * @param description descrição curta
 * @param script conteúdo SQL idempotente
 */
public record DatabaseMigration(int version, String description, String script) {

    /**
     * Valida os dados públicos da migração.
     */
    public DatabaseMigration {
        if (version < 1) {
            throw new IllegalArgumentException("A versão da migração deve ser positiva.");
        }
        if (description == null || description.isBlank() || description.length() > 255) {
            throw new IllegalArgumentException("A descrição da migração é inválida.");
        }
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("O script da migração não pode estar vazio.");
        }
    }
}
