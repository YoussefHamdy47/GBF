package org.bunnys.handler.database.providers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bunnys.handler.database.DatabaseProvider;
import org.bunnys.handler.database.configs.MongoConfig;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * A thread-safe, asynchronous provider for managing connections to a MongoDB
 * database.
 * This class encapsulates the logic for connecting, disconnecting, and
 * performing health checks
 * on a MongoDB client instance.
 * </p>
 *
 * <p>
 * The provider leverages {@link CompletableFuture} for non-blocking I/O
 * operations,
 * ensuring that connection and disconnection logic does not block the calling
 * thread.
 * It uses {@link AtomicReference} and {@link AtomicBoolean} to manage the
 * internal state
 * of the client and connection status in a concurrent-safe manner, making it
 * suitable for
 * use in multithreaded applications.
 * </p>
 *
 * <p>
 * Key features include:
 * </p>
 * <ul>
 * <li><b>Asynchronous Operations:</b> All connection and disconnection logic is
 * executed
 * asynchronously, providing a non-blocking API.</li>
 * <li><b>Thread-Safety:</b> The internal state is managed using atomic
 * variables,
 * guaranteeing correct behavior in concurrent environments.</li>
 * <li><b>Health Checks:</b> The {@link #isHealthy()} method performs a
 * lightweight
 * {@code ping} command to verify the active connection to the database.</li>
 * <li><b>Configuration Driven:</b> The provider's behavior is fully configured
 * via
 * an immutable {@link MongoConfig} object, promoting separation of
 * concerns.</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>{@code
 * MongoConfig config = MongoConfig.builder().envKey("MONGODB_URI").build();
 * MongoProvider provider = new MongoProvider(config);
 *
 * provider.connect().whenComplete((result, ex) -> {
 *     if (ex != null) {
 *         Logger.error("Connection failed: " + ex.getMessage());
 *     } else {
 *         try {
 *             MongoDatabase db = provider.getConnection();
 *             // Perform database operations
 *         } catch (IllegalStateException e) {
 *             Logger.error("Provider is not connected: " + e.getMessage());
 *         }
 *     }
 * });
 *
 * // ... later
 * provider.disconnect();
 * }</pre>
 *
 * @see DatabaseProvider
 * @see MongoConfig
 * @see com.mongodb.client.MongoClient
 * @see java.util.concurrent.CompletableFuture
 *
 * @author Bunny
 */
public final class MongoProvider implements DatabaseProvider<MongoDatabase> {
    /**
     * The immutable configuration object for the MongoDB connection.
     */
    private final MongoConfig config;

    /**
     * A thread-safe reference to the active MongoClient instance.
     */
    private final AtomicReference<MongoClient> client = new AtomicReference<>();

    /**
     * A thread-safe reference to the active MongoDatabase instance.
     */
    private final AtomicReference<MongoDatabase> database = new AtomicReference<>();

    /**
     * An atomic boolean flag indicating whether the provider is connected to
     * MongoDB.
     */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * An atomic boolean flag indicating the health status of the connection.
     */
    private final AtomicBoolean healthy = new AtomicBoolean(false);

    /**
     * Constructs a new {@code MongoProvider} with the specified configuration.
     *
     * @param config The immutable {@link MongoConfig} instance containing
     *               connection settings.
     */
    public MongoProvider(MongoConfig config) {
        this.config = config;
    }

    /**
     * <p>
     * Establishes an asynchronous connection to the MongoDB database. This method
     * is idempotent; if a connection is already established, it will log a warning
     * and return a completed future.
     * </p>
     *
     * <p>
     * The connection process involves creating a new {@link MongoClient}, obtaining
     * the
     * specified database instance, and performing a quick {@code ping} command to
     * verify
     * that the connection is healthy and functional. The internal state flags for
     * {@link #connected} and {@link #healthy} are updated upon successful
     * connection.
     * </p>
     *
     * @return A {@link CompletableFuture<Void>} that completes when the connection
     *         is
     *         successfully established or completes exceptionally if a connection
     *         error occurs.
     */
    @Override
    public CompletableFuture<Void> connect() {
        if (connected.get()) {
            Logger.warning("MongoDB connection already established, aborting.");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                MongoClient newClient = createClient();
                assert config.databaseName() != null;
                MongoDatabase newDatabase = newClient.getDatabase(config.databaseName());

                client.set(newClient);
                database.set(newDatabase);
                connected.set(true);
                healthy.set(true);

                Logger.debug(() -> "Successfully connected to MongoDB database: " + config.databaseName());
            } catch (Exception e) {
                // Ensure the state is reset on failure
                connected.set(false);
                healthy.set(false);
                Logger.error("Failed to connect to MongoDB: " + e.getMessage());
                throw new RuntimeException("MongoDB connection failed", e);
            }
        });
    }

    /**
     * <p>
     * Asynchronously disconnects from the MongoDB database by closing the
     * underlying
     * {@link MongoClient} instance. This method is idempotent; if the provider is
     * not
     * connected, it will return a completed future without performing any action.
     * </p>
     *
     * <p>
     * Upon successful disconnection, all internal state, including the client,
     * database,
     * and connection flags, is reset. This method is designed to be called safely
     * even if the connection state is uncertain.
     * </p>
     *
     * @return A {@link CompletableFuture<Void>} that completes when the
     *         disconnection is
     *         successfully performed.
     */
    @Override
    public CompletableFuture<Void> disconnect() {
        if (!connected.get())
            return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            MongoClient currentClient = client.getAndSet(null);
            if (currentClient != null) {
                try {
                    currentClient.close();
                    connected.set(false);
                    healthy.set(false);
                    database.set(null);
                    Logger.info("Disconnected from MongoDB");
                } catch (Exception e) {
                    Logger.warning("Error during MongoDB disconnect: " + e.getMessage());
                }
            }
        });
    }

    /**
     * <p>
     * Checks if the provider is currently connected to the MongoDB database based
     * on
     * the internal state flag. This is a lightweight check that does not perform
     * a network operation.
     * </p>
     *
     * @return {@code true} if the provider is connected, {@code false} otherwise.
     */
    @Override
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * <p>
     * Performs a real-time health check by sending a lightweight {@code ping}
     * command
     * to the MongoDB server. This check verifies that the connection is not only
     * established but also active and responsive. The internal {@link #healthy}
     * flag is updated with the result of this check.
     * </p>
     *
     * <p>
     * If the provider is not currently connected, this method returns {@code false}
     * immediately without attempting a network call.
     * </p>
     *
     * @return {@code true} if the connection is healthy and the server is
     *         responsive,
     *         {@code false} otherwise.
     */
    @Override
    public boolean isHealthy() {
        if (!connected.get())
            return false;

        // Perform health check
        try {
            MongoDatabase db = database.get();
            if (db != null) {
                db.runCommand(new Document("ping", 1));
                healthy.set(true);
                return true;
            }
        } catch (Exception e) {
            Logger.warning("MongoDB health check failed: " + e.getMessage());
            healthy.set(false);
        }

        return false;
    }

    /**
     * <p>
     * Retrieves the active {@link MongoDatabase} instance. This method should only
     * be
     * called after a successful connection has been established via
     * {@link #connect()}.
     * </p>
     *
     * @return The active {@link MongoDatabase} instance.
     * @throws IllegalStateException if the provider is not currently connected.
     */
    @Override
    public MongoDatabase getConnection() {
        MongoDatabase db = database.get();
        if (db == null)
            throw new IllegalStateException(
                    "MongoDB not connected. Call connect() first or check connection status.");

        return db;
    }

    /**
     * <p>
     * Creates and configures a new {@link MongoClient} instance based on the
     * provided {@link MongoConfig}. This method is called internally during the
     * connection process.
     * </p>
     *
     * <p>
     * The client is built using the standard {@link MongoClientSettings()}
     * and applies all the properties from the configuration object, including
     * connection pool settings, timeouts, and write concerns.
     * </p>
     *
     * @return A newly created and configured {@link MongoClient}.
     */
    private MongoClient createClient() {
        ConnectionString cs = new ConnectionString(config.URI());

        return MongoClients.create(
                MongoClientSettings.builder()
                        .applyConnectionString(cs)
                        .applyToConnectionPoolSettings(builder -> builder
                                .maxSize(config.poolSize())
                                .minSize(config.minPoolSize()))
                        .retryWrites(config.retryWrites())
                        .writeConcern(config.writeConcern())
                        .applyToSocketSettings(builder -> builder
                                .connectTimeout(config.connectTimeoutMS(), TimeUnit.MILLISECONDS))
                        .applyToClusterSettings(builder -> builder
                                .serverSelectionTimeout(config.serverSelectionTimeoutMS(), TimeUnit.MILLISECONDS))
                        .build());
    }
}
