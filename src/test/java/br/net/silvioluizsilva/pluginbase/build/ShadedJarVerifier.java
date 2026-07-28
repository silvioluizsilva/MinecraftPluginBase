package br.net.silvioluizsilva.pluginbase.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Verifica o conteúdo essencial do JAR sombreado gerado pelo Maven.
 */
public final class ShadedJarVerifier {
    private static final List<String> REQUIRED_ENTRIES = List.of(
            "br/net/silvioluizsilva/pluginbase/api/PluginBaseApi.class",
            "br/net/silvioluizsilva/pluginbase/api/DatabaseHealth.class",
            "br/net/silvioluizsilva/pluginbase/libs/mysql/cj/jdbc/Driver.class",
            "META-INF/services/java.sql.Driver",
            "plugin.yml");
    private static final String JDBC_SERVICE = "META-INF/services/java.sql.Driver";
    private static final String RELOCATED_DRIVER =
            "br.net.silvioluizsilva.pluginbase.libs.mysql.cj.jdbc.Driver";

    private ShadedJarVerifier() {
    }

    /**
     * Executa a verificação do JAR informado no primeiro argumento.
     *
     * @param arguments caminho do JAR sombreado
     * @throws IOException se o JAR não puder ser lido
     */
    public static void main(final String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Informe exatamente o caminho do JAR sombreado.");
        }

        final Path jarPath = Path.of(arguments[0]);
        if (!Files.isRegularFile(jarPath)) {
            throw new IllegalStateException("JAR não encontrado: " + jarPath);
        }

        try (ZipFile archive = new ZipFile(jarPath.toFile())) {
            verifyRequiredEntries(archive);
            verifyRelocations(archive);
            verifyJdbcService(archive);
        }

        System.out.println("JAR validado: " + jarPath.toAbsolutePath());
    }

    private static void verifyRequiredEntries(final ZipFile archive) {
        for (final String requiredEntry : REQUIRED_ENTRIES) {
            if (archive.getEntry(requiredEntry) == null) {
                throw new IllegalStateException("Entrada obrigatória ausente no JAR: " + requiredEntry);
            }
        }
    }

    private static void verifyRelocations(final ZipFile archive) {
        final boolean hasUnrelocatedSlf4j = archive.stream()
                .map(ZipEntry::getName)
                .anyMatch(name -> name.startsWith("org/slf4j/"));
        if (hasUnrelocatedSlf4j) {
            throw new IllegalStateException("O JAR não deve empacotar SLF4J.");
        }

        final boolean hasUnrelocatedProtobuf = archive.stream()
                .map(ZipEntry::getName)
                .anyMatch(name -> name.startsWith("com/google/protobuf/"));
        if (hasUnrelocatedProtobuf) {
            throw new IllegalStateException("Protobuf não relocada foi encontrada.");
        }
    }

    private static void verifyJdbcService(final ZipFile archive) throws IOException {
        final ZipEntry serviceEntry = archive.getEntry(JDBC_SERVICE);
        try (InputStream stream = archive.getInputStream(serviceEntry)) {
            final String declaredDriver = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!RELOCATED_DRIVER.equals(declaredDriver)) {
                throw new IllegalStateException("Descritor JDBC inválido: " + declaredDriver);
            }
        }
    }
}