package br.net.silvioluizsilva.pluginbase.language;

import br.net.silvioluizsilva.pluginbase.PluginBase;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;

/**
 * Carrega e formata mensagens traduzidas do plugin.
 */
public final class LanguageManager {

    private final PluginBase plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages = new YamlConfiguration();

    /**
     * Cria o gerenciador de idiomas.
     *
     * @param plugin instância principal do plugin
     */
    public LanguageManager(PluginBase plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Carrega o arquivo do idioma configurado.
     *
     * @param locale código de idioma previamente validado
     */
    public void load(String locale) {
        messages = prepare(locale);
    }

    /**
     * Carrega um idioma candidato sem alterar o idioma ativo.
     *
     * @param locale código de idioma previamente validado
     * @return mensagens candidatas
     */
    public YamlConfiguration prepare(String locale) {
        File languageFile = new File(plugin.getDataFolder(), "languages/" + locale + ".yml");
        if (!languageFile.isFile()) {
            plugin.getPluginLogger().warn("Idioma '{}' não encontrado; usando pt_BR.", locale);
            languageFile = new File(plugin.getDataFolder(), "languages/pt_BR.yml");
        }
        return YamlConfiguration.loadConfiguration(languageFile);
    }

    /**
     * Publica mensagens previamente carregadas.
     *
     * @param candidate mensagens que passarão a ser ativas
     */
    public void activate(YamlConfiguration candidate) {
        messages = Objects.requireNonNull(candidate, "candidate");
    }

    /**
     * Envia uma mensagem traduzida a um destinatário.
     *
     * @param sender destinatário da mensagem
     * @param key chave no arquivo de idioma
     * @param replacements pares de nome do marcador e valor literal
     * @throws IllegalArgumentException quando os pares de substituição estão incompletos
     */
    public void send(CommandSender sender, String key, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("As substituições devem ser informadas em pares.");
        }

        TagResolver.Builder resolver = TagResolver.builder();
        for (int index = 0; index < replacements.length; index += 2) {
            resolver.resolver(Placeholder.unparsed(normalizePlaceholder(replacements[index]), replacements[index + 1]));
        }
        String message = messages.getString(key, "<red>Mensagem ausente: " + key + "</red>");
        String prefix = messages.getString("prefix", "");
        sender.sendMessage(miniMessage.deserialize(normalizeTemplate(prefix + message), resolver.build()));
    }

    /**
     * Retorna uma mensagem simples do idioma ativo sem formatação MiniMessage.
     *
     * @param key chave no arquivo de idioma
     * @return texto traduzido
     */
    public String text(String key) {
        return messages.getString(key, key);
    }

    private static String normalizePlaceholder(String placeholder) {
        String normalized = placeholder;
        if (normalized.startsWith("{") && normalized.endsWith("}") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (!normalized.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("Nome de marcador inválido: " + placeholder);
        }
        return normalized;
    }

    private static String normalizeTemplate(String template) {
        return template.replaceAll("\\{([a-zA-Z0-9_-]+)}", "<$1>");
    }
}
