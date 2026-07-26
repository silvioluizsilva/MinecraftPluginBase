package br.net.silvioluizsilva.pluginbase.database.migration;

import br.net.silvioluizsilva.pluginbase.exception.MigrationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide scripts SQL sem quebrar textos, identificadores ou comentários.
 */
public final class SqlScriptParser {

    private SqlScriptParser() {
    }

    /**
     * Divide um script em instruções executáveis.
     *
     * @param script conteúdo SQL
     * @return instruções sem o delimitador final
     */
    public static List<String> parse(String script) {
        if (script == null) {
            throw new MigrationException("O script SQL não pode ser nulo.");
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            char next = index + 1 < script.length() ? script.charAt(index + 1) : '\0';

            if (lineComment) {
                if (character == '\n') {
                    lineComment = false;
                    current.append(' ');
                }
                continue;
            }
            if (blockComment) {
                if (character == '*' && next == '/') {
                    blockComment = false;
                    index++;
                    current.append(' ');
                }
                continue;
            }
            if (!singleQuote && !doubleQuote && !backtick) {
                char afterNext = index + 2 < script.length() ? script.charAt(index + 2) : '\0';
                if (character == '-' && next == '-' && isMySqlCommentSeparator(afterNext)) {
                    lineComment = true;
                    index++;
                    continue;
                }
                if (character == '#') {
                    lineComment = true;
                    continue;
                }
                if (character == '/' && next == '*') {
                    blockComment = true;
                    index++;
                    continue;
                }
            }

            if (character == '\'' && !doubleQuote && !backtick && !isEscaped(script, index)) {
                singleQuote = !singleQuote;
            } else if (character == '"' && !singleQuote && !backtick && !isEscaped(script, index)) {
                doubleQuote = !doubleQuote;
            } else if (character == '`' && !singleQuote && !doubleQuote) {
                backtick = !backtick;
            }

            if (character == ';' && !singleQuote && !doubleQuote && !backtick) {
                addStatement(statements, current);
            } else {
                current.append(character);
            }
        }
        if (singleQuote || doubleQuote || backtick || blockComment) {
            throw new MigrationException("Script SQL incompleto ou com delimitadores não encerrados.");
        }
        addStatement(statements, current);
        return List.copyOf(statements);
    }

    private static boolean isEscaped(String script, int index) {
        int backslashes = 0;
        for (int position = index - 1; position >= 0 && script.charAt(position) == '\\'; position--) {
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    private static boolean isMySqlCommentSeparator(char character) {
        return character == '\0' || Character.isWhitespace(character) || Character.isISOControl(character);
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().strip();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }
}
