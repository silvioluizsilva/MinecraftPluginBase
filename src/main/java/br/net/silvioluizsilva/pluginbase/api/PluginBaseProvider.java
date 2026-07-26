package br.net.silvioluizsilva.pluginbase.api;

import org.bukkit.Bukkit;

import java.util.Optional;

/**
 * Localiza a API publicada no registro de serviços do Paper.
 */
public final class PluginBaseProvider {

    private PluginBaseProvider() {
    }

    /**
     * Procura a API sem lançar exceção quando o PluginBase não está disponível.
     *
     * @return API registrada, quando presente
     */
    public static Optional<PluginBaseApi> find() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(PluginBaseApi.class));
    }

    /**
     * Obtém a API ativa.
     *
     * @return API registrada
     * @throws IllegalStateException quando o PluginBase não está disponível
     */
    public static PluginBaseApi get() {
        return find().orElseThrow(() -> new IllegalStateException(
                "A API do PluginBase não está registrada. Declare depend: [PluginBase]."
        ));
    }
}
