package br.net.silvioluizsilva.pluginbase.logging;

import br.net.silvioluizsilva.pluginbase.config.LoggingConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifica níveis e proteção de argumentos do logger central.
 */
final class PluginLoggerTest {

    @Test
    void shouldSuppressDebugWhenDisabled() {
        Logger delegate = mock(Logger.class);
        PluginLogger logger = new PluginLogger(delegate, new LoggingConfig(false, false));

        logger.debug("Password={}", "secret");

        verify(delegate, never()).debug(any(String.class), any(Object[].class));
    }

    @Test
    void shouldPublishSanitizedArguments() {
        Logger delegate = mock(Logger.class);
        PluginLogger logger = new PluginLogger(delegate, new LoggingConfig(true, false));
        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);

        logger.info("Conexão: {}", "password=secret-value");

        verify(delegate).info(eq("Conexão: {}"), arguments.capture());
        assertEquals(1, arguments.getValue().length);
        assertFalse(String.valueOf(arguments.getValue()[0]).contains("secret-value"));
    }

    @Test
    void shouldHideStackTraceWhenDisabled() {
        Logger delegate = mock(Logger.class);
        PluginLogger logger = new PluginLogger(delegate, new LoggingConfig(false, false));
        IllegalStateException failure = new IllegalStateException("password=secret-value");

        logger.error("Operação recusada.", failure);

        verify(delegate, never()).error(any(String.class), eq(failure));
        verify(delegate).error(eq("{} Tipo: {}."), eq("Operação recusada."), eq("IllegalStateException"));
    }
}
