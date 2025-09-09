package org.bunnys.handler.database.configs;

import com.mongodb.ConnectionString;
import com.mongodb.WriteConcern;
import org.bunnys.handler.database.DatabaseConfig;
import org.bunnys.handler.database.DatabaseProvider;
import org.bunnys.handler.database.DatabaseType;
import org.bunnys.handler.database.providers.MongoProvider;
import org.bunnys.handler.utils.handler.EnvLoader;

/**
 * <p>
 * A final, immutable configuration class for establishing and managing
 * connections
 * to a MongoDB database. This class provides a robust and centralized way to
 * configure
 * MongoDB connection settings, including connection string, database name,
 * connection
 * pool size, and write concerns.
 * </p>
 *
 * <p>
 * This class implements the
 * <a href="https://en.wikipedia.org/wiki/Builder_pattern">Builder pattern</a>
 * to facilitate the creation of immutable instances. It resolves configuration
 * properties from
 * environment variables, providing a flexible way to manage different
 * configurations
 * for various deployment environments (e.g., development, staging, production).
 * It also applies sensible defaults for common settings, reducing the need for
 * explicit configuration in most cases.
 * </p>
 *
 * <p>
 * Key features include:
 * </p>
 * <ul>
 * <li><b>Environment Variable Integration:</b> The MongoDB URI is resolved from
 * an
 * environment variable, promoting secure credential management and
 * externalization of configuration.</li>
 * <li><b>Immutable State:</b> Once created, a {@code MongoConfig} instance
 * cannot be modified,
 * ensuring thread-safety and predictability.</li>
 * <li><b>Default Values:</b> Sensible defaults are provided for settings like
 * connection
 * pool size, timeouts, and write concerns, simplifying setup.</li>
 * <li><b>Comprehensive Validation:</b> The builder performs extensive
 * validation to ensure
 * that the configuration is consistent and valid before an instance is
 * created.</li>
 * <li><b>Connection Pool Configuration:</b> Allows fine-grained control over
 * the
 * connection pool, including minimum and maximum sizes, and idle/life
 * times.</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * </p>
 * 
 * <pre>{@code
 * MongoConfig config = MongoConfig.builder()
 *         .envKey("MONGODB_URI")
 *         .databaseName("myDatabase")
 *         .poolSize(100)
 *         .retryWrites(true)
 *         .build();
 *
 * DatabaseProvider<com.mongodb.client.MongoDatabase> provider = config.createProvider();
 * // Use the provider to get a database instance
 * com.mongodb.client.MongoDatabase database = provider.getDatabase();
 * }</pre>
 *
 * @see DatabaseConfig
 * @see MongoProvider
 * @see com.mongodb.ConnectionString
 * @see com.mongodb.WriteConcern
 *
 * @author Bunny
 */
@SuppressWarnings("unused")
public final class MongoConfig extends DatabaseConfig<com.mongodb.client.MongoDatabase> {
    private static final int DEFAULT_POOL_SIZE = 50;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_SERVER_SELECTION_TIMEOUT_MS = 5_000;
    private static final WriteConcern DEFAULT_WRITE_CONCERN = WriteConcern.MAJORITY;

    private final String URI;
    private final String databaseName;
    private final boolean sslEnabled;
    private final int poolSize;
    private final boolean retryWrites;
    private final WriteConcern writeConcern;
    private final int connectTimeoutMS;
    private final int serverSelectionTimeoutMS;
    private final int minPoolSize;
    // private final int maxIdleTimeMS;
    // private final int maxLifeTimeMS;

    /**
     * Private constructor to enforce the use of the Builder pattern.
     * All configuration parameters are sourced from the provided {@link Builder}
     * instance.
     *
     * @param builder The {@link Builder} instance containing all configured
     *                parameters.
     */
    private MongoConfig(Builder builder) {
        super(DatabaseType.MONGO);

        this.URI = resolveUri(builder.envKey);

        // Parse connection string for defaults
        ConnectionString cs = new ConnectionString(this.URI);

        this.databaseName = builder.databaseName != null ? builder.databaseName : cs.getDatabase();
        this.sslEnabled = builder.sslEnabled != null ? builder.sslEnabled
                : (cs.getSslEnabled() != null && cs.getSslEnabled());
        this.poolSize = builder.poolSize != null ? builder.poolSize
                : (cs.getMaxConnectionPoolSize() != null ? cs.getMaxConnectionPoolSize() : DEFAULT_POOL_SIZE);
        this.minPoolSize = builder.minPoolSize != null ? builder.minPoolSize : 5;
        this.retryWrites = builder.retryWrites != null ? builder.retryWrites
                : (cs.getRetryWritesValue() != null ? cs.getRetryWritesValue() : true);
        this.writeConcern = builder.writeConcern != null ? builder.writeConcern
                : (cs.getWriteConcern() != null ? cs.getWriteConcern() : DEFAULT_WRITE_CONCERN);
        this.connectTimeoutMS = builder.connectTimeoutMS != null ? builder.connectTimeoutMS
                : DEFAULT_CONNECT_TIMEOUT_MS;
        this.serverSelectionTimeoutMS = builder.serverSelectionTimeoutMS != null ? builder.serverSelectionTimeoutMS
                : DEFAULT_SERVER_SELECTION_TIMEOUT_MS;
        // this.maxIdleTimeMS = builder.maxIdleTimeMS != null ? builder.maxIdleTimeMS :
        // 600_000; // 10 minutes
        // this.maxLifeTimeMS = builder.maxLifeTimeMS != null ? builder.maxLifeTimeMS :
        // 1800_000; // 30 minutes

        validate();
    }

    /**
     * Resolves the MongoDB connection URI from an environment variable.
     *
     * @param envKey The key of the environment variable containing the URI.
     * @return The resolved MongoDB URI.
     * @throws IllegalArgumentException if the environment key is null or blank, or
     *                                  if the
     *                                  URI is not found or is blank in the
     *                                  environment.
     */
    private String resolveUri(String envKey) {
        if (envKey == null || envKey.isBlank())
            throw new IllegalArgumentException("Environment key cannot be null or blank");

        String resolvedUri = EnvLoader.get(envKey);
        if (resolvedUri == null || resolvedUri.isBlank())
            throw new IllegalArgumentException(
                    String.format("MongoDB URI not found in environment variable '%s'", envKey));

        return resolvedUri;
    }

    /**
     * <p>
     * Validates the final state of the {@code MongoConfig} instance to ensure all
     * properties are consistent and valid. This method is called by the private
     * constructor to guarantee that any created instance is in a valid state.
     * </p>
     *
     * <p>
     * The validation includes:
     * </p>
     * <ul>
     * <li>Checking that the URI and database name are not null or blank.</li>
     * <li>Verifying that the pool size is within a valid range (1 to 1000).</li>
     * <li>Ensuring that the minimum pool size is less than the maximum pool
     * size.</li>
     * <li>Validating that timeout values are positive.</li>
     * <li>Ensuring the MongoDB URI has a valid format.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if any validation rule is not met.
     */
    @Override
    public void validate() {
        if (this.URI == null || this.URI.isBlank())
            throw new IllegalArgumentException("MongoDB URI cannot be null or blank");

        if (this.databaseName == null || this.databaseName.isBlank())
            throw new IllegalArgumentException("Database name must be specified either in URI or config");

        if (this.poolSize <= 0 || this.poolSize > 1000)
            throw new IllegalArgumentException("Pool size must be between 1 and 1000");

        if (this.minPoolSize < 0 || this.minPoolSize >= this.poolSize)
            throw new IllegalArgumentException("Min pool size must be >= 0 and < max pool size");

        if (this.connectTimeoutMS <= 0)
            throw new IllegalArgumentException("Connect timeout must be positive");

        if (this.serverSelectionTimeoutMS <= 0)
            throw new IllegalArgumentException("Server selection timeout must be positive");

        // Validate URI format
        try {
            new ConnectionString(URI);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid MongoDB connection string: " + e.getMessage(), e);
        }
    }

    /**
     * <p>
     * Creates a new {@link MongoProvider} instance based on the configuration
     * defined
     * by this object. This method serves as the factory for obtaining the provider
     * responsible for managing the MongoDB connection lifecycle and client
     * creation.
     * </p>
     *
     * @return A new {@link MongoProvider} instance configured with the settings
     *         from this object.
     */
    @Override
    public DatabaseProvider<com.mongodb.client.MongoDatabase> createProvider() {
        return new MongoProvider(this);
    }

    // Getters

    /**
     * Retrieves the MongoDB connection URI.
     * 
     * @return The connection URI string.
     */
    public String URI() {
        return this.URI;
    }

    /**
     * Retrieves the name of the database to connect to.
     * 
     * @return The database name.
     */
    public String databaseName() {
        return this.databaseName;
    }

    /**
     * Checks if SSL is enabled for the connection.
     * 
     * @return {@code true} if SSL is enabled, {@code false} otherwise.
     */
    public boolean sslEnabled() {
        return this.sslEnabled;
    }

    /**
     * Retrieves the maximum size of the connection pool.
     * 
     * @return The maximum pool size.
     */
    public int poolSize() {
        return this.poolSize;
    }

    /**
     * Retrieves the minimum size of the connection pool.
     * 
     * @return The minimum pool size.
     */
    public int minPoolSize() {
        return this.minPoolSize;
    }

    /**
     * Checks if retryable writes are enabled.
     * 
     * @return {@code true} if retryable writes are enabled, {@code false}
     *         otherwise.
     */
    public boolean retryWrites() {
        return this.retryWrites;
    }

    /**
     * Retrieves the configured {@link WriteConcern}.
     * 
     * @return The {@link WriteConcern} for write operations.
     */
    public WriteConcern writeConcern() {
        return this.writeConcern;
    }

    /**
     * Retrieves the connection timeout in milliseconds.
     * 
     * @return The connection timeout in milliseconds.
     */
    public int connectTimeoutMS() {
        return this.connectTimeoutMS;
    }

    /**
     * Retrieves the server selection timeout in milliseconds.
     * 
     * @return The server selection timeout in milliseconds.
     */
    public int serverSelectionTimeoutMS() {
        return this.serverSelectionTimeoutMS;
    }

    /**
     * /**
     * Retrieves the maximum time a connection can remain idle in the pool before
     * being
     * closed, in milliseconds.
     * 
     * @return The max idle time in milliseconds.
     */
    // public int maxIdleTimeMS() {
    // return this.maxIdleTimeMS;
    // }

    /**
     * Retrieves the maximum lifetime of a connection in the pool, in milliseconds.
     * 
     * @return The max lifetime in milliseconds.
     */
    // public int maxLifeTimeMS() {
    // return this.maxLifeTimeMS;
    // }

    /**
     * <p>
     * Static factory method to create a new {@link Builder} instance.
     * This is the entry point for configuring and creating a {@code MongoConfig}
     * object.
     * </p>
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * A static nested class that implements the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">Builder pattern</a>
     * for creating immutable {@link MongoConfig} instances. This class provides a
     * fluent
     * API for setting various configuration parameters.
     * </p>
     *
     * <p>
     * Each method in the builder returns the builder instance itself, allowing for
     * method chaining. The final {@link #build()} method creates the
     * {@code MongoConfig}
     * object, performing all necessary validation.
     * </p>
     *
     * @see MongoConfig
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        private String envKey;
        private String databaseName;
        private Boolean sslEnabled;
        private Integer poolSize;
        private Integer minPoolSize;
        private Boolean retryWrites;
        private WriteConcern writeConcern;
        private Integer connectTimeoutMS;
        private Integer serverSelectionTimeoutMS;
        // private Integer maxIdleTimeMS;
        // private Integer maxLifeTimeMS;

        /**
         * Sets the environment variable key for the MongoDB URI.
         * This is a mandatory parameter.
         * 
         * @param envKey The environment variable key.
         * @return The builder instance for method chaining.
         */
        public Builder envKey(String envKey) {
            this.envKey = envKey;
            return this;
        }

        /**
         * Sets the database name. If not provided, it will be extracted from the URI.
         * 
         * @param databaseName The name of the database.
         * @return The builder instance for method chaining.
         */
        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        /**
         * Sets whether SSL is enabled.
         * 
         * @param sslEnabled {@code true} to enable SSL, {@code false} otherwise.
         * @return The builder instance for method chaining.
         */
        public Builder sslEnabled(boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
            return this;
        }

        /**
         * Sets the maximum connection pool size. Defaults to 50.
         * 
         * @param poolSize The max pool size.
         * @return The builder instance for method chaining.
         */
        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        /**
         * Sets the minimum connection pool size. Defaults to 5.
         * 
         * @param minPoolSize The min pool size.
         * @return The builder instance for method chaining.
         */
        public Builder minPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        /**
         * Sets whether retryable writes are enabled. Defaults to {@code true}.
         * 
         * @param retryWrites {@code true} to enable retryable writes, {@code false}
         *                    otherwise.
         * @return The builder instance for method chaining.
         */
        public Builder retryWrites(boolean retryWrites) {
            this.retryWrites = retryWrites;
            return this;
        }

        /**
         * Sets the {@link WriteConcern} for write operations. Defaults to
         * {@link WriteConcern#MAJORITY}.
         * 
         * @param writeConcern The write concern.
         * @return The builder instance for method chaining.
         */
        public Builder writeConcern(WriteConcern writeConcern) {
            this.writeConcern = writeConcern;
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds. Defaults to 10,000 ms.
         * 
         * @param ms The timeout in milliseconds.
         * @return The builder instance for method chaining.
         */
        public Builder connectTimeoutMS(int ms) {
            this.connectTimeoutMS = ms;
            return this;
        }

        /**
         * Sets the server selection timeout in milliseconds. Defaults to 5,000 ms.
         * 
         * @param ms The timeout in milliseconds.
         * @return The builder instance for method chaining.
         */
        public Builder serverSelectionTimeoutMS(int ms) {
            this.serverSelectionTimeoutMS = ms;
            return this;
        }

        /**
         * Sets the maximum idle time for a connection in the pool, in milliseconds.
         * Defaults to 600,000 ms (10 minutes).
         * 
         * @param ms The maximum idle time in milliseconds.
         * @return The builder instance for method chaining.
         */
        // public Builder maxIdleTimeMS(int ms) {
        // this.maxIdleTimeMS = ms;
        // return this;
        // }

        /**
         * Sets the maximum lifetime for a connection in the pool, in milliseconds.
         * Defaults to 1,800,000 ms (30 minutes).
         * 
         * @param ms The maximum lifetime in milliseconds.
         * @return The builder instance for method chaining.
         */
        // public Builder maxLifeTimeMS(int ms) {
        // this.maxLifeTimeMS = ms;
        // return this;
        // }

        /**
         * <p>
         * Builds and returns a new {@link MongoConfig} instance based on the
         * parameters set in this builder. This method calls the private constructor
         * of {@code MongoConfig} and triggers the validation process.
         * </p>
         *
         * @return A new, immutable {@link MongoConfig} instance.
         * @throws IllegalArgumentException if any of the configuration parameters are
         *                                  invalid.
         */
        public MongoConfig build() {
            return new MongoConfig(this);
        }
    }
}