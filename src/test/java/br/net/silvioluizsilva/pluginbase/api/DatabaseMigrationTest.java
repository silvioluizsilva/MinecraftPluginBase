package br.net.silvioluizsilva.pluginbase.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica o contrato público das migrações consumidoras.
 */
final class DatabaseMigrationTest {

    @Test
    void shouldCreateValidMigration() {
        DatabaseMigration migration = new DatabaseMigration(1, "Initial", "CREATE TABLE sample (id INT)");

        assertEquals(1, migration.version());
    }

    @Test
    void shouldRejectInvalidMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseMigration(0, "Initial", "SELECT 1"));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseMigration(1, " ", "SELECT 1"));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseMigration(1, "Initial", " "));
    }
}
