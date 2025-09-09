package org.bunnys.handler.lifecycle;

import org.bunnys.handler.Config;
import org.bunnys.handler.database.DatabaseConfig;
import org.bunnys.handler.database.DatabaseProvider;
import org.bunnys.handler.database.DatabaseType;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>
 * A utility class responsible for managing the lifecycle of database
 * connections within
 * the application. This class provides a centralized, thread-safe, and
 * asynchronous
 * mechanism for initializing, retrieving, and gracefully shutting down database
 * providers.
 * </p>
 *
 * <p>
 * By encapsulating the management of {@link DatabaseProvider} instances, this
 * class
 * ensures that database connections are handled consistently and that resources
 * are
 * released properly upon application shutdown. It leverages
 * {@link ConcurrentMap} to
 * safely manage multiple database providers in a concurrent environment.
 * </p>
 *
 * <p>
 * Key features of this lifecycle manager include:
 * </p>
 * <ul>
 * <li><b>Centralized Initialization:</b> The {@link #initialize(Config)} method
 * serves as the
 * single entry point for setting up database connections based on a global
 * configuration.</li>
 * <li><b>Graceful Shutdown:</b> The {@link #shutdown()} method ensures that all
 * active
 * database connections are closed asynchronously and safely.</li>
 * <li><b>Thread-Safe Access:</b> All methods are designed to be thread-safe,
 * allowing for
 * concurrent access from different parts of the application.</li>
 * <li><b>Provider Retrieval:</b> Provides methods for safely retrieving
 * configured and
 * connected database providers.</li>
 * </ul>
 *
 * @see DatabaseProvider
 * @see DatabaseConfig
 * @see Config
 *
 * @author Bunny
 */
public final class DatabaseLifecycle {
    /**
     * A thread-safe map to store initialized database providers, keyed by their
     * type.
     */
    private static final ConcurrentMap<DatabaseType, DatabaseProvider<?>> providers = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DatabaseLifecycle() {
        // Utility class
    }

    /**
     * <p>
     * Initializes a database connection based on the provided application
     * configuration.
     * This method is idempotent and will log an informational message if database
     * connections are disabled in the configuration or if no database configuration
     * is provided.
     * </p>
     *
     * <p>
     * The connection is established asynchronously, but the method blocks using
     * {@code CompletableFuture.join()} to ensure that the database is fully
     * connected
     * and ready before the application proceeds. This design ensures that
     * subsequent
     * database operations will not fail due to a lack of connection.
     * </p>
     *
     * @param config The application configuration, containing database connection
     *               details.
     * @return A {@link CompletableFuture<Void>} that completes when the database
     *         connection has been successfully established.
     * @throws RuntimeException if the database initialization or connection fails.
     */
    public static CompletableFuture<Void> initialize(Config config) {
        if (!config.connectToDatabase()) {
            Logger.info("Database connections disabled in configuration");
            return CompletableFuture.completedFuture(null);
        }

        DatabaseConfig<?> dbConfig = config.databaseConfig();
        if (dbConfig == null) {
            Logger.warning("No database configuration provided");
            return CompletableFuture.completedFuture(null);
        }

        Logger.info("Initializing database connection: " + dbConfig.type());

        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseProvider<?> provider = dbConfig.createProvider();
                providers.put(dbConfig.type(), provider);

                provider.connect().join(); // Wait for connection

                Logger.success("Database connection established: " + dbConfig.type());
            } catch (Exception e) {
                Logger.error("Failed to initialize database: " + e.getMessage());
                throw new RuntimeException("Database initialization failed", e);
            }
        });
    }

    /**
     * <p>
     * Gracefully shuts down all active database connections managed by this class.
     * This method iterates through all registered providers and calls their
     * {@link DatabaseProvider#disconnect()} method asynchronously. It then waits
     * for all disconnection processes to complete before clearing the internal map.
     * </p>
     *
     * <p>
     * Errors during individual disconnections are logged but do not prevent the
     * shutdown of other connections, ensuring a robust cleanup process.
     * </p>
     *
     * @return A {@link CompletableFuture<Void>} that completes when all database
     *         connections have been closed.
     */
    public static CompletableFuture<Void> shutdown() {
        if (providers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Logger.info("Shutting down database connections...");

        CompletableFuture<?>[] shutdownFutures = providers.values().stream()
                .map(provider -> provider.disconnect().exceptionally(throwable -> {
                    Logger.warning("Error during database shutdown: " + throwable.getMessage());
                    return null;
                }))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(shutdownFutures)
                .thenRun(() -> {
                    providers.clear();
                    Logger.success("All database connections closed");
                });
    }

    /**
     * <p>
     * Retrieves a connected and ready database provider of the specified type.
     * This method is type-safe and will cast the provider to the expected generic
     * type.
     * </p>
     *
     * @param type The {@link DatabaseType} of the provider to retrieve.
     * @param <T>  The expected type of the database client/connection.
     * @return The requested {@link DatabaseProvider} instance.
     * @throws IllegalStateException if no provider is found for the specified type.
     */
    @SuppressWarnings("unchecked")
    public static <T> DatabaseProvider<T> getProvider(DatabaseType type) {
        DatabaseProvider<?> provider = providers.get(type);
        if (provider == null) {
            throw new IllegalStateException("No provider found for database type: " + type);
        }
        return (DatabaseProvider<T>) provider;
    }

    /**
     * <p>
     * A convenience method to retrieve the {@link DatabaseProvider} for MongoDB.
     * This method is a specialized version of {@link #getProvider(DatabaseType)}.
     * </p>
     *
     * @return The {@link DatabaseProvider} for MongoDB.
     * @throws IllegalStateException if the MongoDB provider has not been
     *                               initialized.
     */
    public static DatabaseProvider<com.mongodb.client.MongoDatabase> getMongoProvider() {
        return getProvider(DatabaseType.MONGO);
    }

    /**
     * <p>
     * Checks the health status of a specific database connection. This method will
     * return {@code true} only if a provider for the given type exists and its
     * {@link DatabaseProvider#isHealthy()} method returns {@code true}.
     * </p>
     *
     * @param type The {@link DatabaseType} of the connection to check.
     * @return {@code true} if the provider exists and is healthy, {@code false}
     *         otherwise.
     */
    public static boolean isHealthy(DatabaseType type) {
        DatabaseProvider<?> provider = providers.get(type);
        return provider != null && provider.isHealthy();
    }
}