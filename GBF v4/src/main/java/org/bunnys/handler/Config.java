package org.bunnys.handler;

import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bunnys.handler.database.DatabaseConfig;
import org.bunnys.handler.database.configs.MongoConfig;
import org.bunnys.handler.events.defaults.DefaultEvents;
import org.bunnys.handler.utils.handler.EnvLoader;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.EnumSet;
import java.util.Objects;

/**
 * <p>
 * A comprehensive, final, and immutable configuration object for the BunnyNexus
 * bot.
 * This class serves as a single source of truth for all application settings,
 * promoting a robust, decoupled, and maintainable architecture. It is built
 * using the <a href="https://en.wikipedia.org/wiki/Builder_pattern">Builder
 * pattern</a>
 * to ensure that all instances are valid and consistent upon creation.
 * </p>
 *
 * <p>
 * The configuration supports a variety of settings, including bot token
 * management
 * (with environment variable fallback), database integration, event and command
 * management, and JDA-specific settings like gateway intents and shard count.
 * This design pattern makes the application's startup configuration clear,
 * flexible, and less prone to errors.
 * </p>
 *
 * <p>
 * Developers can easily configure the bot by calling the static
 * {@code builder()}
 * method and chaining the desired configuration options. This approach allows
 * for
 * a highly readable and self-documenting setup process.
 * </p>
 *
 * @author Bunny
 * @see net.dv8tion.jda.api.JDABuilder
 * @see org.bunnys.handler.database.DatabaseConfig
 * @see org.bunnys.handler.database.configs.MongoConfig
 * @see org.bunnys.handler.utils.handler.EnvLoader
 */
@SuppressWarnings({"unused", "rawtypes"})
public class Config {
    /**
     * The version string of the bot, used for logging and identification.
     */
    private final String version;
    /**
     * The default command prefix used by the bot.
     */
    private final String prefix;
    /**
     * An array of user IDs or names recognized as bot developers.
     */
    private final String[] developers;
    /**
     * An array of guild IDs for servers designated for testing purposes.
     */
    private final String[] testServers;
    /**
     * A flag indicating whether the bot should automatically log in upon startup.
     */
    private final boolean autoLogin;
    /**
     * A flag to enable or disable debug logging.
     */
    private final boolean debug;
    /**
     * The number of shards the bot should use. A value of 0 indicates
     * single-sharded mode.
     */
    private final int shardCount;
    /**
     * The package name where event listeners are located.
     */
    private final String eventsPackage;
    /**
     * A set of default events that have been disabled.
     */
    private final EnumSet<DefaultEvents> disabledDefaults;
    /**
     * The package name where command classes are located.
     */
    private final String commandsPackage;
    /**
     * The set of gateway intents required for the bot's functionality.
     */
    private final EnumSet<GatewayIntent> intents;
    /**
     * The database configuration object. Can be null if database connections are
     * disabled.
     */
    private final DatabaseConfig databaseConfig;
    /**
     * A flag to control whether the application should attempt to connect to a
     * database.
     */
    private final boolean connectToDatabase;
    /**
     * The bot's authentication token. Marked as volatile to allow for safe,
     * thread-safe updates at runtime.
     */
    private volatile String token;

    /**
     * Private constructor to enforce the use of the Builder pattern. This ensures
     * that a {@code Config} object can only be created with a complete set of
     * valid parameters provided by the builder.
     *
     * @param builder The {@link Builder} instance containing all configured
     *                properties.
     */
    private Config(Builder builder) {
        this.token = this.resolveToken(builder.token);
        this.version = Objects.requireNonNull(builder.version, "Version must not be null");
        this.developers = builder.developers != null ? builder.developers.clone() : new String[0];
        this.testServers = builder.testServers != null ? builder.testServers.clone() : new String[0];
        this.prefix = builder.prefix;
        this.eventsPackage = builder.eventsPackage;
        this.commandsPackage = builder.commandsPackage;
        this.disabledDefaults = builder.disabledDefaultEvents != null
                ? EnumSet.copyOf(builder.disabledDefaultEvents)
                : EnumSet.noneOf(DefaultEvents.class);
        this.shardCount = builder.shardCount;
        this.debug = builder.debug;
        this.autoLogin = builder.autoLogin;
        this.intents = builder.intents != null
                ? EnumSet.copyOf(builder.intents)
                : EnumSet.noneOf(GatewayIntent.class);
        this.connectToDatabase = builder.connectToDatabase;
        this.databaseConfig = builder.databaseConfig;
    }

    // --- Getters ---

    /**
     * Retrieves the version of the bot.
     * 
     * @return The version string.
     */
    public String version() {
        return this.version;
    }

    /**
     * Retrieves the configured command prefix.
     * 
     * @return The prefix string.
     */
    public String prefix() {
        return this.prefix;
    }

    /**
     * Checks if debug mode is enabled.
     * 
     * @return {@code true} if debug mode is on, otherwise {@code false}.
     */
    public boolean debug() {
        return this.debug;
    }

    /**
     * Retrieves the configured shard count.
     * 
     * @return The number of shards.
     */
    public int shardCount() {
        return this.shardCount;
    }

    /**
     * Retrieves the bot's authentication token.
     * 
     * @return The token string.
     */
    public String token() {
        return this.token;
    }

    /**
     * Retrieves the package name for event listeners.
     * 
     * @return The events package name.
     */
    public String eventsPackage() {
        return this.eventsPackage;
    }

    /**
     * Retrieves the package name for command classes.
     * 
     * @return The commands package name.
     */
    public String commandsPackage() {
        return this.commandsPackage;
    }

    /**
     * Checks if the application is configured to connect to a database.
     * 
     * @return {@code true} if database connection is enabled, otherwise
     *         {@code false}.
     */
    public boolean connectToDatabase() {
        return this.connectToDatabase;
    }

    /**
     * Retrieves a copy of the developers array. The returned array is a clone
     * to maintain the immutability of the internal state.
     * 
     * @return An array of developer IDs.
     */
    public String[] developers() {
        return this.developers.clone();
    }

    /**
     * Retrieves a copy of the test servers array. The returned array is a clone
     * to maintain the immutability of the internal state.
     * 
     * @return An array of test server IDs.
     */
    public String[] testServers() {
        return this.testServers.clone();
    }

    /**
     * Retrieves a copy of the disabled default events set.
     * 
     * @return An {@link EnumSet} of disabled default events.
     */
    public EnumSet<DefaultEvents> disabledDefaults() {
        return EnumSet.copyOf(this.disabledDefaults);
    }

    /**
     * Retrieves a copy of the configured gateway intents.
     * 
     * @return An {@link EnumSet} of gateway intents.
     */
    public EnumSet<GatewayIntent> intents() {
        return EnumSet.copyOf(intents);
    }

    /**
     * Checks if the bot is configured to automatically log in upon startup.
     * 
     * @return {@code true} if auto-login is enabled, otherwise {@code false}.
     */
    public boolean autoLogin() {
        return this.autoLogin;
    }

    /**
     * Retrieves the database configuration object.
     * 
     * @return The {@link DatabaseConfig} instance.
     */
    public DatabaseConfig databaseConfig() {
        return this.databaseConfig;
    }

    /**
     * Updates the bot's authentication token at runtime. This method is marked as
     * package-private to restrict its use to trusted components, such as the
     * main application handler.
     *
     * @param token The new bot token.
     * @throws IllegalArgumentException if the provided token is null or blank.
     */
    void token(String token) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Token must be a valid string");
        Logger.warning("Token updated at runtime.");
        this.token = token;
    }

    /**
     * Resolves the bot's authentication token from the builder, with a fallback to
     * a system environment variable named "TOKEN". This promotes secure credential
     * management by avoiding hardcoded tokens in the codebase.
     *
     * @param token The token provided by the builder. Can be null.
     * @return The resolved token string.
     * @throws IllegalStateException if a token is not provided and cannot be found
     *                               in the environment.
     */
    private String resolveToken(String token) {
        if (token != null && !token.isBlank()) {
            if (this.debug)
                Logger.info("Using manually provided bot token");
            return token;
        }

        if (this.debug)
            Logger.info("No manual token provided, attempting to load from .env under \"TOKEN\"");
        String envToken = EnvLoader.get("TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            Logger.success("Loaded bot token from .env");
            return envToken;
        }

        throw new IllegalStateException("No bot token provided and .env does not contain a valid TOKEN key");
    }

    /**
     * Static factory method to create a new {@link Builder} instance. This is the
     * primary entry point for configuring the application.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    // --- Builder ---

    /**
     * <p>
     * A static nested class that implements the Builder pattern for creating
     * immutable {@link Config} instances. This class provides a fluent API for
     * setting application-wide configuration parameters.
     * </p>
     *
     * <p>
     * Each method in the builder returns the builder instance itself, enabling
     * method chaining. The final {@link #build()} method creates the immutable
     * {@code Config} object and performs a final validation check to ensure
     * consistency.
     * </p>
     *
     * @see Config
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        private String token;
        private String version;
        private String[] developers = new String[0];
        private String[] testServers = new String[0];
        private String prefix = "!";
        private boolean autoLogin = true;
        private boolean debug = false;
        private int shardCount = 0;
        private String eventsPackage;
        private String commandsPackage;
        private EnumSet<DefaultEvents> disabledDefaultEvents = EnumSet.noneOf(DefaultEvents.class);
        private EnumSet<GatewayIntent> intents = EnumSet.noneOf(GatewayIntent.class);
        private DatabaseConfig databaseConfig = null;
        private boolean connectToDatabase = false;

        /**
         * Sets the bot's version. This is a required field.
         * 
         * @param version The version string.
         * @return The current builder instance.
         * @throws IllegalStateException if the version is null or blank.
         */
        public Builder version(String version) {
            if (version == null || version.isBlank())
                throw new IllegalStateException("Config.version must be a valid string");
            this.version = version;
            return this;
        }

        /**
         * Sets whether the application should connect to a database.
         * 
         * @param connectToDatabase {@code true} to enable database connections.
         * @return The current builder instance.
         */
        public Builder connectToDatabase(boolean connectToDatabase) {
            this.connectToDatabase = connectToDatabase;
            return this;
        }

        /**
         * Sets the bot's command prefix.
         * 
         * @param prefix The prefix string.
         * @return The current builder instance.
         * @throws IllegalArgumentException if the prefix is null or blank.
         */
        public Builder prefix(String prefix) {
            if (prefix == null || prefix.isBlank())
                throw new IllegalArgumentException("Prefix must be a valid string");
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the list of developer IDs.
         * 
         * @param developers An array of developer IDs.
         * @return The current builder instance.
         */
        public Builder developers(String... developers) {
            this.developers = developers != null ? developers : new String[0];
            return this;
        }

        /**
         * Sets the list of test server IDs.
         * 
         * @param testServers An array of test server IDs.
         * @return The current builder instance.
         */
        public Builder testServers(String... testServers) {
            this.testServers = testServers != null ? testServers : new String[0];
            return this;
        }

        /**
         * Sets the debug mode flag.
         * 
         * @param debug {@code true} to enable debug mode.
         * @return The current builder instance.
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * Sets the auto-login flag.
         * 
         * @param autoLogin {@code true} to enable auto-login.
         * @return The current builder instance.
         */
        public Builder autoLogin(boolean autoLogin) {
            this.autoLogin = autoLogin;
            return this;
        }

        /**
         * Sets the bot's authentication token.
         * 
         * @param token The token string.
         * @return The current builder instance.
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the package name where event listeners are located.
         * 
         * @param eventsPackage The package name.
         * @return The current builder instance.
         */
        public Builder eventsPackage(String eventsPackage) {
            this.eventsPackage = eventsPackage;
            return this;
        }

        /**
         * Sets the package name where command classes are located.
         * 
         * @param commandsPackage The package name.
         * @return The current builder instance.
         */
        public Builder commandsPackage(String commandsPackage) {
            this.commandsPackage = commandsPackage;
            return this;
        }

        /**
         * Sets the set of default events to disable.
         * 
         * @param events An {@link EnumSet} of {@link DefaultEvents} to disable.
         * @return The current builder instance.
         */
        public Builder disableDefaultEvents(EnumSet<DefaultEvents> events) {
            this.disabledDefaultEvents = events != null ? EnumSet.copyOf(events) : EnumSet.noneOf(DefaultEvents.class);
            return this;
        }

        /**
         * Sets the number of shards for the bot.
         * 
         * @param shardCount The number of shards.
         * @return The current builder instance.
         * @throws IllegalArgumentException if the shard count is negative.
         */
        public Builder shardCount(int shardCount) {
            if (shardCount < 0)
                throw new IllegalArgumentException("Shard count must be >= 0");
            this.shardCount = shardCount;
            return this;
        }

        /**
         * Sets the required gateway intents for the bot.
         * 
         * @param intents An {@link EnumSet} of {@link GatewayIntent}s.
         * @return The current builder instance.
         */
        public Builder intents(EnumSet<GatewayIntent> intents) {
            this.intents = intents != null ? EnumSet.copyOf(intents) : EnumSet.noneOf(GatewayIntent.class);
            return this;
        }

        /**
         * <p>
         * Configures the bot to use a MongoDB database connection. This method
         * automatically creates a {@link MongoConfig} instance using the provided
         * environment key to resolve the connection URI.
         * </p>
         *
         * @param envKey The environment variable key containing the MongoDB URI.
         * @return The current builder instance.
         */
        public Builder mongo(String envKey) {
            return mongo(envKey, null);
        }

        /**
         * <p>
         * Configures the bot to use a MongoDB database connection with a specified
         * database name. This method creates a {@link MongoConfig} instance and
         * sets the database name, which overrides any name specified in the URI.
         * </p>
         *
         * @param envKey       The environment variable key containing the MongoDB URI.
         * @param databaseName The name of the database to connect to.
         * @return The current builder instance.
         */
        public Builder mongo(String envKey, String databaseName) {
            MongoConfig.Builder builder = MongoConfig.builder().envKey(envKey);
            if (databaseName != null) {
                builder.databaseName(databaseName);
            }
            this.databaseConfig = builder.build();
            return this;
        }

        /**
         * Builds and returns a new {@link Config} instance based on the parameters
         * set in this builder. This method performs a final validation to ensure
         * that the configuration is valid before the object is created.
         *
         * @return A new, immutable {@link Config} instance.
         * @throws IllegalArgumentException if {@code connectToDatabase} is true but
         *                                  no database configuration has been provided.
         */
        public Config build() {
            if (connectToDatabase && databaseConfig == null)
                throw new IllegalArgumentException("Connect to Database is true but no Database Config was provided");
            return new Config(this);
        }
    }

    /**
     * Provides a string representation of the {@code Config} object, primarily for
     * logging and debugging purposes.
     *
     * @return A string representation of the configuration.
     */
    @Override
    public String toString() {
        return "Config{version='" + version + "', shards=" + shardCount + ", eventsPackage='" + eventsPackage + "'}";
    }
}
