package br.net.silvioluizsilva.pluginbase.service;

import br.net.silvioluizsilva.pluginbase.config.PluginConfig;
import br.net.silvioluizsilva.pluginbase.exception.ServiceException;
import br.net.silvioluizsilva.pluginbase.logging.PluginLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Registro tipado que controla dependências e ciclo de vida dos serviços.
 */
public final class ServiceRegistry {

    private enum State { NEW, STARTING, RUNNING, STOPPING, STOPPED }

    private final PluginLogger logger;
    private final Map<Class<?>, ManagedService> services = new LinkedHashMap<>();
    private State state = State.NEW;

    /**
     * Cria um registro de serviços.
     *
     * @param logger logger central
     */
    public ServiceRegistry(PluginLogger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Registra uma implementação para um contrato público.
     *
     * @param contract tipo usado para resolução
     * @param service implementação gerenciada
     * @param <T> tipo do contrato
     */
    public synchronized <T extends ManagedService> void register(Class<T> contract, T service) {
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(service, "service");
        if (state != State.NEW) {
            throw new ServiceException("Serviços só podem ser registrados antes da inicialização.");
        }
        if (!contract.isInstance(service)) {
            throw new ServiceException("O serviço não implementa o contrato " + contract.getName() + ".");
        }
        if (services.putIfAbsent(contract, service) != null) {
            throw new ServiceException("Contrato já registrado: " + contract.getName() + ".");
        }
    }

    /**
     * Resolve um serviço pelo contrato registrado.
     *
     * @param contract contrato desejado
     * @param <T> tipo do serviço
     * @return implementação registrada
     */
    public synchronized <T extends ManagedService> T resolve(Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        ManagedService service = services.get(contract);
        if (service == null) {
            throw new ServiceException("Serviço não registrado: " + contract.getName() + ".");
        }
        return contract.cast(service);
    }

    /**
     * Procura um serviço sem lançar exceção quando o contrato não foi registrado.
     *
     * @param contract contrato desejado
     * @param <T> tipo do serviço
     * @return implementação, quando presente
     */
    public synchronized <T extends ManagedService> Optional<T> find(Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        return Optional.ofNullable(services.get(contract)).map(contract::cast);
    }

    /**
     * Inicializa os serviços na ordem de registro.
     */
    public void startAll() {
        List<ManagedService> snapshot;
        synchronized (this) {
        if (state != State.NEW) {
            throw new ServiceException("O registro de serviços já foi inicializado ou encerrado.");
        }
            state = State.STARTING;
            snapshot = new ArrayList<>(services.values());
        }
        List<ManagedService> started = new ArrayList<>();
        try {
            for (ManagedService service : snapshot) {
                service.start();
                started.add(service);
                logger.debug("Serviço iniciado: {}.", service.getClass().getSimpleName());
            }
            setState(State.RUNNING);
        } catch (RuntimeException exception) {
            stopReverse(started);
            setState(State.STOPPED);
            throw new ServiceException("Falha durante a inicialização dos serviços.", exception);
        }
    }

    /**
     * Propaga uma configuração válida aos serviços recarregáveis.
     *
     * @param config nova configuração
     */
    public void reloadAll(PluginConfig config) {
        Objects.requireNonNull(config, "config");
        List<ManagedService> snapshot;
        synchronized (this) {
            requireRunning();
            snapshot = new ArrayList<>(services.values());
        }
        for (ManagedService service : snapshot) {
            if (service instanceof ReloadableService reloadable) {
                reloadable.reload(config);
            }
        }
    }

    /**
     * Encerra os serviços em ordem inversa ao registro.
     */
    public void stopAll() {
        List<ManagedService> snapshot;
        synchronized (this) {
            if (state == State.STOPPED || state == State.NEW) {
                state = State.STOPPED;
                return;
            }
            if (state != State.RUNNING) {
                throw new ServiceException("O registro não pode ser encerrado no estado atual.");
            }
            state = State.STOPPING;
            snapshot = new ArrayList<>(services.values());
        }
        stopReverse(snapshot);
        setState(State.STOPPED);
    }

    /**
     * Retorna os contratos registrados sem permitir alterações.
     *
     * @return conjunto de contratos
     */
    public synchronized Set<Class<?>> registeredTypes() {
        return Set.copyOf(services.keySet());
    }

    private void stopReverse(List<ManagedService> orderedServices) {
        Collections.reverse(orderedServices);
        for (ManagedService service : orderedServices) {
            try {
                service.stop();
            } catch (RuntimeException exception) {
                logger.error("Falha ao encerrar o serviço " + service.getClass().getSimpleName() + ".", exception);
            }
        }
    }

    private void requireRunning() {
        if (state != State.RUNNING) {
            throw new ServiceException("Os serviços não estão em execução.");
        }
    }

    private synchronized void setState(State newState) {
        state = newState;
    }
}
