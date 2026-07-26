package br.net.silvioluizsilva.pluginbase.bootstrap;

import br.net.silvioluizsilva.pluginbase.api.DatabaseAccess;
import br.net.silvioluizsilva.pluginbase.api.DatabaseMigration;
import br.net.silvioluizsilva.pluginbase.api.DatabaseWork;
import br.net.silvioluizsilva.pluginbase.api.DatabaseHealth;
import br.net.silvioluizsilva.pluginbase.config.TransactionConfig;
import br.net.silvioluizsilva.pluginbase.database.TimedConnection;
import br.net.silvioluizsilva.pluginbase.exception.DatabaseException;
import org.bukkit.Bukkit;
import br.net.silvioluizsilva.pluginbase.database.migration.ConsumerMigrationRunner;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

/**
 * Implementa o acesso ao banco isolado por namespace de consumidor.
 */
final class DefaultDatabaseAccess implements DatabaseAccess {

    private final PluginContext context;
    private final String namespace;
    private final ConsumerTransactionLimiter transactionLimiter;
    private final BooleanSupplier primaryThread;

    DefaultDatabaseAccess(PluginContext context, Plugin owner) {
        this(context, owner, new ConsumerTransactionLimiter(context.configManager().current().database()
                .transactions().maximumConcurrentPerConsumer()), Bukkit::isPrimaryThread);
    }

    DefaultDatabaseAccess(PluginContext context, Plugin owner, ConsumerTransactionLimiter transactionLimiter) {
        this(context, owner, transactionLimiter, Bukkit::isPrimaryThread);
    }

    DefaultDatabaseAccess(
            PluginContext context,
            Plugin owner,
            ConsumerTransactionLimiter transactionLimiter,
            BooleanSupplier primaryThread
    ) {
        this.context = Objects.requireNonNull(context, "context");
        Objects.requireNonNull(owner, "owner");
        this.namespace = normalize(owner.getName());
        this.transactionLimiter = Objects.requireNonNull(transactionLimiter, "transactionLimiter");
        this.primaryThread = Objects.requireNonNull(primaryThread, "primaryThread");
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public String table(String logicalName) {
        Objects.requireNonNull(logicalName, "logicalName");
        String normalized = logicalName.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9_]{0,30}")) {
            throw new IllegalArgumentException("Nome lógico de tabela inválido: " + logicalName);
        }
        String physicalName = namespace + "_" + normalized;
        if (physicalName.length() > 64) {
            throw new IllegalArgumentException("O nome físico da tabela excede 64 caracteres.");
        }
        return physicalName;
    }

    @Override
    public boolean isConnected() {
        return context.databaseManager().isConnected();
    }

    @Override
    public DatabaseHealth health() {
        return context.databaseManager().health();
    }

    @Override
    public CompletionStage<Void> whenConnected() {
        return context.databaseManager().whenConnected();
    }

    @Override
    public <T> T transaction(DatabaseWork<T> work) {
        Objects.requireNonNull(work, "work");
        if (primaryThread.getAsBoolean()) {
            throw new DatabaseException("Transações consumidoras não podem executar na thread principal.");
        }
        TransactionConfig config = context.configManager().current().database().transactions();
        ConsumerTransactionLimiter.Permit permit = null;
        long startedAt = System.nanoTime();
        try {
            permit = transactionLimiter.acquire(config.concurrencyWaitMs());
            if (permit == null) {
                throw new DatabaseException("Limite de transações simultâneas atingido para " + namespace + ".");
            }
            return context.databaseManager().transaction(connection ->
                    work.execute(TimedConnection.wrap(connection, config.statementTimeoutSeconds())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DatabaseException("A espera pela transação foi interrompida.", exception);
        } finally {
            if (permit != null) {
                permit.close();
            }
            logDuration(config, startedAt);
        }
    }

    private void logDuration(TransactionConfig config, long startedAt) {
        long elapsedMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        if (elapsedMs >= config.criticalWarningMs()) {
            context.logger().error("Transação crítica do consumidor " + namespace + ": " + elapsedMs + " ms.");
        } else if (elapsedMs >= config.slowWarningMs()) {
            context.logger().warn("Transação lenta do consumidor {}: {} ms.", namespace, elapsedMs);
        }
    }

    @Override
    public void migrate(List<DatabaseMigration> migrations) {
        if (primaryThread.getAsBoolean()) {
            throw new DatabaseException("Consumer migrations must not execute on the primary server thread.");
        }
        context.databaseManager().transaction(connection -> {
            new ConsumerMigrationRunner(context.logger()).migrate(connection, namespace, migrations);
            return null;
        });
    }

    private static String normalize(String pluginName) {
        String normalized = pluginName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (!normalized.matches("[a-z][a-z0-9]{2,31}")) {
            throw new IllegalArgumentException("O nome do plugin não produz um namespace válido: " + pluginName);
        }
        return normalized;
    }
}
