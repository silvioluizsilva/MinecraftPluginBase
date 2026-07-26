package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.exception.MigrationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica ordenação e integridade do catálogo de migrações.
 */
final class MigrationCatalogTest {

    @Test
    void shouldLoadCatalogInVersionOrder() {
        String catalog = """
                # catalog
                2|Second change|sql/002_second.sql
                1|Initial schema|sql/001_initial.sql
                """;

        List<Migration> migrations = MigrationCatalog.load(stream(catalog));

        assertEquals(List.of(1, 2), migrations.stream().map(Migration::version).toList());
    }

    @Test
    void shouldRejectDuplicateVersions() {
        String catalog = """
                1|Initial schema|sql/001_initial.sql
                1|Repeated schema|sql/001_repeated.sql
                """;

        assertThrows(MigrationException.class, () -> MigrationCatalog.load(stream(catalog)));
    }

    @Test
    void shouldRejectUnsafeResourcePath() {
        assertThrows(
                MigrationException.class,
                () -> MigrationCatalog.load(stream("1|Invalid|../outside.sql"))
        );
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
