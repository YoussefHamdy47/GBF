package org.bunnys.handler;

import net.dv8tion.jda.api.sharding.ShardManager;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.database.providers.MongoProvider;
import org.bunnys.handler.events.EventRegistry;
import org.bunnys.handler.lifecycle.*;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * The main entry point for the BunnyNexus application.
 *
 * <p>
 * This class orchestrates the initialization and lifecycle management of the
 * bot, including connecting to Discord via a {@link ShardManager}, loading
 * commands
 * and events, and managing database connections. It acts as the central hub for
 * accessing core components like the {@link CommandRegistry} and
 * {@link MongoProvider}.
 * </p>
 *
 * <p>
 * The design of this class encapsulates the complexity of the bot's startup
 * and provides a clean, public API for managing its state, such as updating
 * the token, reconnecting, and graceful shutdown. All core lifecycle events
 * are delegated to dedicated handler classes in the
 * {@code org.bunnys.handler.lifecycle} package.
 * </p>
 *
 * @author bunny
 */
@SuppressWarnings("unused")
public class BunnyNexus {
    private final Config config;
    private volatile ShardManager shardManager;
    private final ApplicationContext applicationContext;
    private final AutowireCapableBeanFactory beanFactory;

    /**
     * The registry for all bot commands
     * <p>
     * This is a core component that manages the lifecycle and execution of slash
     * commands.
     * </p>
     */
    private final CommandRegistry commandRegistry = new CommandRegistry();

    /**
     * The registry for all bot events
     */
    private EventRegistry eventRegistry = new EventRegistry(this);

    /**
     * The provider for MongoDB database access.
     * <p>
     * This is initialized only if the bot is configured to connect to a database.
     * </p>
     */
    private MongoProvider mongoProvider;

    /**
     * Constructs a new instance of {@code BunnyNexus}.
     *
     * <p>
     * Initializes the bot with the provided configuration. If the configuration
     * specifies {@code autoLogin}, the bot will immediately attempt to log in to
     * Discord.
     * </p>
     *
     * @param config             The configuration object containing bot settings.
     *                           Cannot be
     *                           {@code null}.
     * @param applicationContext The Spring ApplicationContext for dependency
     *                           injection.
     * @param beanFactory        The AutowireCapableBeanFactory for wiring
     *                           dependencies.
     * @throws IllegalArgumentException If the provided {@code config} is
     *                                  {@code null}.
     */
    public BunnyNexus(Config config, ApplicationContext applicationContext, AutowireCapableBeanFactory beanFactory) {
        if (config == null)
            throw new IllegalArgumentException("Config cannot be null");

        this.config = config;
        this.applicationContext = applicationContext;
        this.beanFactory = beanFactory;

        if (this.config.autoLogin())
            this.login();
    }

    /**
     * Initializes all necessary components for the bot to operate.
     *
     * <p>
     * This private method is responsible for the entire bot startup sequence,
     * which includes:
     * </p>
     * <ul>
     * <li>Attaching the logger and logging startup information.</li>
     * <li>Initializing the {@link ShardManager} to connect to Discord.</li>
     * <li>Loading and registering event and command handlers.</li>
     * <li>Initializing the database connection if configured.</li>
     * </ul>
     */
    public void login() {
        LoggerLifecycle.attachLogger(config);
        StartupLogger.logStartupInfo(config);

        this.shardManager = ShardManagerInitializer.initShardManager(config);

        EventLifecycle.loadAndRegisterEvents(config, this, shardManager);
        CommandLifecycle.loadAndRegisterCommands(config, this, commandRegistry, applicationContext, beanFactory);

        if (this.config.connectToDatabase()) {
            DatabaseLifecycle.initialize(this.config.databaseConfig()).join();
            this.mongoProvider = (MongoProvider) DatabaseLifecycle.getMongoProvider();
        }
    }

    /* -------------------- Public API -------------------- */

    /**
     * Updates the bot's authentication token in the configuration.
     *
     * <p>
     * This method only updates the token value and does not automatically trigger a
     * reconnection.
     * To apply the new token, call the {@link #reconnect(String)} method.
     * </p>
     *
     * @param newToken The new bot token. Cannot be {@code null} or blank.
     * @throws IllegalArgumentException If the new token is {@code null} or an empty
     *                                  string.
     */
    public void updateToken(String newToken) {
        if (newToken == null || newToken.isBlank())
            throw new IllegalArgumentException("New token cannot be null or blank");
        this.config.token(newToken);
    }

    /**
     * Sets the event registry, used for shutdown
     */
    public void setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    /**
     * Initiates a full reconnection of the bot to Discord.
     *
     * <p>
     * This method shuts down the existing {@link ShardManager}, updates the token,
     * and re-initializes a new {@link ShardManager} to connect with the new token.
     * It is a safe and atomic way to change the bot's credentials and reconnect.
     * </p>
     *
     * @param newToken The new token to use for reconnection.
     */
    public void reconnect(String newToken) {
        ShardManagerLifecycle.reconnect(this, newToken);
    }

    /**
     * Reconnects the bot to the database, gracefully handling existing connections.
     *
     * <p>
     * This method first checks if the database connection is enabled. If so, it
     * logs the action, gracefully shuts down the current connection, and then
     * re-initializes a new connection, re-assigning the {@link MongoProvider}.
     * </p>
     */
    public void reconnectDatabase() {
        if (!this.config.connectToDatabase()) {
            Logger.warning("Database connection is disabled. Skipping reconnection.");
            return;
        }

        Logger.info("Reconnecting to database...");

        DatabaseLifecycle.shutdown().join();

        DatabaseLifecycle.initialize(this.config.databaseConfig()).join();
        this.mongoProvider = (MongoProvider) DatabaseLifecycle.getMongoProvider();
    }

    /**
     * Initiates a graceful shutdown of the bot.
     *
     * <p>
     * This method handles the orderly shutdown of the {@link ShardManager} and
     * releases all associated resources. This is the preferred method for exiting
     * the application to prevent resource leaks and incomplete state writes.
     * </p>
     */
    public void shutdown() {
        ShardManagerLifecycle.shutdown(this);
    }

    /* -------------------- Getters -------------------- */

    /**
     * Gets the active {@link ShardManager} instance.
     *
     * @return The {@link ShardManager} instance, or {@code null} if the bot has not
     *         logged in yet.
     */
    public ShardManager getShardManager() {
        return shardManager;
    }

    /**
     * Gets the configuration object used to initialize the bot.
     *
     * @return The {@link Config} instance.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Gets the command registry instance.
     *
     * @return The {@link CommandRegistry} instance that manages all bot commands.
     */
    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    /**
     * Gets the event registry instance.
     *
     * @return The {@link EventRegistry} instance that manages all bot events.
     */
    public EventRegistry eventRegistry() {
        return eventRegistry;
    }

    /**
     * Gets the MongoDB database provider.
     *
     * @return The {@link MongoProvider} instance, or {@code null} if the database
     *         is not connected.
     */
    public MongoProvider getMongoProvider() {
        return mongoProvider;
    }

    /**
     * Gets the Spring ApplicationContext.
     *
     * @return The {@link ApplicationContext} instance.
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Gets the AutowireCapableBeanFactory.
     *
     * @return The {@link AutowireCapableBeanFactory} instance.
     */
    public AutowireCapableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    /* -------------------- Internal use -------------------- */

    /**
     * Sets the {@link ShardManager} instance.
     *
     * <p>
     * This method is intended for internal use by lifecycle handlers during
     * initialization
     * and reconnection processes. External use is discouraged.
     * </p>
     *
     * @param sm The new {@link ShardManager} instance.
     */
    public void setShardManager(ShardManager sm) {
        this.shardManager = sm;
    }
}