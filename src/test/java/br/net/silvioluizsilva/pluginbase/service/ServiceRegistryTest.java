package br.net.silvioluizsilva.pluginbase.service;

import br.net.silvioluizsilva.pluginbase.exception.ServiceException;
import br.net.silvioluizsilva.pluginbase.config.LoggingConfig;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifica registro, ordem e recuperação do ciclo de vida dos serviços.
 */
final class ServiceRegistryTest {

    @Test
    void shouldResolveRegisteredService() {
        ServiceRegistry registry = registry();
        AlphaService service = new AlphaService(new ArrayList<>());

        registry.register(AlphaService.class, service);

        assertSame(service, registry.resolve(AlphaService.class));
    }

    @Test
    void shouldStartInRegistrationOrderAndStopInReverseOrder() {
        List<String> events = new ArrayList<>();
        ServiceRegistry registry = registry();
        registry.register(AlphaService.class, new AlphaService(events));
        registry.register(BetaService.class, new BetaService(events));

        registry.startAll();
        registry.stopAll();

        assertEquals(List.of("alpha:start", "beta:start", "beta:stop", "alpha:stop"), events);
    }

    @Test
    void shouldRollbackPreviouslyStartedServicesWhenStartupFails() {
        List<String> events = new ArrayList<>();
        ServiceRegistry registry = registry();
        registry.register(AlphaService.class, new AlphaService(events));
        registry.register(FailingService.class, new FailingService(events));

        assertThrows(ServiceException.class, registry::startAll);
        assertEquals(List.of("alpha:start", "failing:start", "alpha:stop"), events);
    }

    @Test
    void shouldRejectDuplicateContract() {
        ServiceRegistry registry = registry();
        registry.register(AlphaService.class, new AlphaService(new ArrayList<>()));

        assertThrows(
                ServiceException.class,
                () -> registry.register(AlphaService.class, new AlphaService(new ArrayList<>()))
        );
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownContract() {
        ServiceRegistry registry = registry();

        assertTrue(registry.find(AlphaService.class).isEmpty());
    }

    private static ServiceRegistry registry() {
        return new ServiceRegistry(new PluginLogger(mock(Logger.class), new LoggingConfig(false, false)));
    }

    private static class AlphaService implements ManagedService {
        private final List<String> events;

        private AlphaService(List<String> events) {
            this.events = events;
        }

        @Override
        public void start() {
            events.add("alpha:start");
        }

        @Override
        public void stop() {
            events.add("alpha:stop");
        }
    }

    private static final class BetaService implements ManagedService {
        private final List<String> events;

        private BetaService(List<String> events) {
            this.events = events;
        }

        @Override
        public void start() {
            events.add("beta:start");
        }

        @Override
        public void stop() {
            events.add("beta:stop");
        }
    }

    private static final class FailingService implements ManagedService {
        private final List<String> events;

        private FailingService(List<String> events) {
            this.events = events;
        }

        @Override
        public void start() {
            events.add("failing:start");
            throw new IllegalStateException("Expected test failure");
        }

        @Override
        public void stop() {
            events.add("failing:stop");
        }
    }
}
