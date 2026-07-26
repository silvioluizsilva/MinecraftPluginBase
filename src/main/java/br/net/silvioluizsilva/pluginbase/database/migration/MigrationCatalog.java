package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.exception.MigrationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Carrega o catálogo explícito de migrações empacotadas.
 */
public final class MigrationCatalog {

    private MigrationCatalog() {
    }

    /**
     * Lê e valida um catálogo no formato {@code versão|descrição|recurso}.
     *
     * @param input conteúdo do catálogo
     * @return migrações ordenadas por versão
     */
    public static List<Migration> load(InputStream input) {
        if (input == null) {
            throw new MigrationException("Catálogo de migrações não encontrado.");
        }
        try (input) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            List<Migration> migrations = new ArrayList<>();
            Set<Integer> versions = new HashSet<>();
            for (String rawLine : content.lines().toList()) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                if (fields.length != 3) {
                    throw new MigrationException("Entrada inválida no catálogo: " + line);
                }
                int version;
                try {
                    version = Integer.parseInt(fields[0].strip());
                } catch (NumberFormatException exception) {
                    throw new MigrationException("Versão inválida no catálogo.", exception);
                }
                Migration migration = new Migration(version, fields[1].strip(), fields[2].strip());
                if (!versions.add(version)) {
                    throw new MigrationException("Versão de migração duplicada: " + version + ".");
                }
                migrations.add(migration);
            }
            migrations.sort(java.util.Comparator.comparingInt(Migration::version));
            return List.copyOf(migrations);
        } catch (IOException exception) {
            throw new MigrationException("Não foi possível ler o catálogo de migrações.", exception);
        }
    }
}
