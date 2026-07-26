package br.net.silvioluizsilva.pluginbase.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica a filtragem usada pelas sugestões Brigadier.
 */
final class CommandSuggestionsTest {

    @Test
    void shouldFilterIgnoringCaseAndSortResults() {
        List<String> result = CommandSuggestions.matching(
                "D",
                List.of("debug", "services", "database")
        );

        assertEquals(List.of("database", "debug"), result);
    }

    @Test
    void shouldReturnAllOptionsForEmptyPrefix() {
        List<String> result = CommandSuggestions.matching("", List.of("services", "database"));

        assertEquals(List.of("database", "services"), result);
    }

    @Test
    void shouldReturnEmptyListWithoutMatches() {
        assertEquals(List.of(), CommandSuggestions.matching("unknown", List.of("database", "services")));
    }
}
