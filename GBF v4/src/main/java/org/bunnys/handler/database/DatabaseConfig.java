package org.bunnys.handler.database;

/**
 * <p>
 * An abstract base class for defining database configurations. This class
 * serves as a
 * core component of a flexible and extensible database management framework,
 * providing a
 * standardized way to handle configuration details for various database types.
 * </p>
 *
 * <p>
 * It implements a simplified form of the
 * <a href="https://en.wikipedia.org/wiki/Template_method_pattern">Template
 * Method pattern</a>,
 * where concrete subclasses must implement methods for validating their
 * specific configurations
 * and for creating the appropriate database provider. This design decouples the
 * configuration
 * details from the database connection and management logic.
 * </p>
 *
 * <p>
 * The generic type parameter {@code <T>} represents the specific database
 * client type
 * (e.g., {@code com.mongodb.client.MongoDatabase} for MongoDB or
 * {@code java.sql.Connection} for a
 * SQL database), ensuring strong typing and type safety across the framework.
 * </p>
 *
 * @param <T> The type of the database client or connection object that this
 *            configuration
 *            will be used to create.
 * @author Bunny
 */
public abstract class DatabaseConfig<T> {
    /**
     * The type of the database (e.g., MONGO, POSTGRES).
     */
    private final DatabaseType type;

    /**
     * Constructs a new DatabaseConfig instance with a specified database type.
     * This constructor is called by subclasses to set the type of database they
     * configure.
     *
     * @param type The {@link DatabaseType} associated with this configuration.
     */
    protected DatabaseConfig(DatabaseType type) {
        this.type = type;
    }

    /**
     * Retrieves the immutable type of the database.
     *
     * @return The {@link DatabaseType} of this configuration.
     */
    public final DatabaseType type() {
        return this.type;
    }

    /**
     * An abstract method that must be implemented by concrete subclasses to
     * validate
     * their specific configuration parameters. This method is typically called by
     * the
     * builder's {@code build()} method or the constructor of the configuration
     * class
     * to ensure the object's state is valid and consistent.
     *
     * @throws IllegalArgumentException if any configuration parameter is invalid.
     */
    public abstract void validate();

    /**
     * An abstract factory method that creates and returns a concrete implementation
     * of a {@link DatabaseProvider} specific to this configuration. This method
     * is the "template method" that allows the framework to instantiate the
     * correct provider without knowing the concrete configuration type.
     *
     * @return A {@link DatabaseProvider} configured with the settings from this
     *         object.
     */
    public abstract DatabaseProvider<T> createProvider();
}
