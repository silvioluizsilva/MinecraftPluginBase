package br.net.silvioluizsilva.pluginbase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste mínimo de integridade da versão inicial.
 */
final class ProjectStructureTest {

    @Test
    void basePackageMustRemainStable() {
        assertEquals("br.net.silvioluizsilva.pluginbase", PluginBase.class.getPackageName());
    }
}
