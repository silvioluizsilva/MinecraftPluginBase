package br.net.silvioluizsilva.pluginbase.language;

import br.net.silvioluizsilva.pluginbase.exception.ConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class LanguageManagerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsEmptyCatalog() throws IOException {
        Path catalog = temporaryDirectory.resolve("empty.yml");
        Files.writeString(catalog, "\n");

        assertThrows(ConfigurationException.class, () -> LanguageManager.loadStrict(catalog.toFile()));
    }

    @Test
    void rejectsInvalidYamlCatalog() throws IOException {
        Path catalog = temporaryDirectory.resolve("invalid.yml");
        Files.writeString(catalog, "message: [unterminated\n");

        assertThrows(ConfigurationException.class, () -> LanguageManager.loadStrict(catalog.toFile()));
    }

    @Test
    void fillsMissingKeysFromPortugueseFallback() {
        YamlConfiguration candidate = new YamlConfiguration();
        candidate.set("command.reloaded", "Reloaded");
        YamlConfiguration fallback = new YamlConfiguration();
        fallback.set("command.reloaded", "Recarregado");
        fallback.set("command.denied", "Sem permissão");

        LanguageManager.applyFallback(candidate, fallback);

        assertEquals("Reloaded", candidate.getString("command.reloaded"));
        assertEquals("Sem permissão", candidate.getString("command.denied"));
    }

    @Test
    void bundledCatalogsHaveMatchingKeys() {
        YamlConfiguration portuguese = bundledCatalog("pt_BR");
        YamlConfiguration english = bundledCatalog("en_US");

        assertEquals(portuguese.getKeys(true), english.getKeys(true));
    }

    private static YamlConfiguration bundledCatalog(String locale) {
        try (var stream = LanguageManagerTest.class.getResourceAsStream("/languages/" + locale + ".yml")) {
            if (stream == null) {
                throw new AssertionError("Catálogo ausente: " + locale);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new AssertionError("Não foi possível ler o catálogo: " + locale, exception);
        }
    }
}
