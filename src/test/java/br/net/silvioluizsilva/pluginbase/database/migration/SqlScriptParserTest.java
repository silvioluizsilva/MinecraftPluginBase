package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.exception.MigrationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica a divisão segura de scripts de migração.
 */
final class SqlScriptParserTest {

    @Test
    void shouldPreserveSemicolonsInsideValuesAndIdentifiers() {
        String script = """
                -- comentário inicial
                INSERT INTO `sample;table` (value) VALUES ('first;value');
                # outro comentário
                UPDATE sample SET value = "second;value";
                """;

        List<String> statements = SqlScriptParser.parse(script);

        assertEquals(2, statements.size());
        assertTrue(statements.getFirst().contains("'first;value'"));
        assertTrue(statements.getLast().contains("\"second;value\""));
    }

    @Test
    void shouldRemoveBlockComments() {
        List<String> statements = SqlScriptParser.parse("SELECT /* internal; comment */ 1; SELECT 2;");

        assertEquals(List.of("SELECT   1", "SELECT 2"), statements);
    }

    @Test
    void shouldRejectUnclosedLiteral() {
        assertThrows(MigrationException.class, () -> SqlScriptParser.parse("SELECT 'incomplete;"));
    }

    @Test
    void shouldPreserveDoubleMinusWithoutRequiredCommentWhitespace() {
        List<String> statements = SqlScriptParser.parse("SELECT 5--2; -- valid comment\nSELECT 3;");

        assertEquals(List.of("SELECT 5--2", "SELECT 3"), statements);
    }
}
