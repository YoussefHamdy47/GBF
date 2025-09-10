package org.bunnys.handler.lifecycle;

import org.bunnys.handler.database.DatabaseConfig;
import org.bunnys.handler.database.DatabaseProvider;
import org.bunnys.handler.database.DatabaseType;
import org.bunnys.handler.database.configs.MongoConfig;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public final class DatabaseLifecycle {
    private static final ConcurrentMap<DatabaseType, DatabaseProvider<?>> providers = new ConcurrentHashMap<>();

    @Autowired
    private MongoConfig config;

    @PostConstruct
    public void init() {
        initialize(config).join();
    }

    public static CompletableFuture<Void> initialize(DatabaseConfig<?> config) {
        if (config == null) {
            Logger.warning("No database configuration provided");
            return CompletableFuture.completedFuture(null);
        }

        Logger.debug(() -> "Initializing database connection: " + config.type());

        return CompletableFuture.runAsync(() -> {
            try {
                DatabaseProvider<?> provider = config.createProvider();
                providers.put(config.type(), provider);

                provider.connect().join();
                Logger.debug(() -> "Database connection established: " + config.type());
            } catch (Exception e) {
                Logger.error("Failed to initialize database: " + e.getMessage());
                throw new RuntimeException("Database initialization failed", e);
            }
        });
    }

    public static CompletableFuture<Void> shutdown() {
        if (providers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Logger.info("[Database] Disconnecting...");

        CompletableFuture<?>[] shutdownFutures = providers.values().stream()
                .map(provider -> provider.disconnect().exceptionally(throwable -> {
                    Logger.error("[Database] Error during shutdown: " + throwable.getMessage(), throwable);
                    return null;
                }))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(shutdownFutures)
                .thenRun(() -> {
                    providers.clear();
                    Logger.info("[Database] Disconnected");
                });
    }

    @SuppressWarnings("unchecked")
    public static <T> DatabaseProvider<T> getProvider(DatabaseType type) {
        DatabaseProvider<?> provider = providers.get(type);
        if (provider == null) {
            throw new IllegalStateException("No provider found for database type: " + type);
        }
        return (DatabaseProvider<T>) provider;
    }

    public static DatabaseProvider<com.mongodb.client.MongoDatabase> getMongoProvider() {
        return getProvider(DatabaseType.MONGO);
    }

    public static boolean isHealthy(DatabaseType type) {
        DatabaseProvider<?> provider = providers.get(type);
        return provider != null && provider.isHealthy();
    }
}