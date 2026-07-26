package br.net.silvioluizsilva.pluginbase.command;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Oferece filtragem determinística para sugestões Brigadier.
 */
public final class CommandSuggestions {

    private CommandSuggestions() {
    }

    /**
     * Filtra opções pelo texto já digitado, ignorando maiúsculas.
     *
     * @param remaining texto parcial
     * @param options opções disponíveis
     * @return opções correspondentes em ordem alfabética
     */
    public static List<String> matching(String remaining, Collection<String> options) {
        Objects.requireNonNull(remaining, "remaining");
        Objects.requireNonNull(options, "options");
        String prefix = remaining.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(Objects::nonNull)
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .toList();
    }
}
