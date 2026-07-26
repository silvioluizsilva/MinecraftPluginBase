package br.net.silvioluizsilva.pluginbase.command;

/**
 * Representa uma ação de comando protegida pelo tratamento central de erros.
 */
@FunctionalInterface
public interface CommandAction {

    /**
     * Executa a ação.
     *
     * @return código de resultado Brigadier
     * @throws Exception quando a ação não puder ser concluída
     */
    int execute() throws Exception;
}
