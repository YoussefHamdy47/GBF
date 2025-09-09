package org.bunnys.handler.database;

import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * A generic, asynchronous interface for managing the lifecycle of a database
 * connection.
 * This contract defines the core functionalities required for any database
 * provider
 * within the framework, including connection management, state checks, and
 * resource cleanup.
 * </p>
 *
 * <p>
 * The use of {@link CompletableFuture} for connection and disconnection methods
 * ensures
 * that database operations are non-blocking and can be integrated seamlessly
 * into
 * asynchronous application flows. This design is crucial for building scalable
 * and
 * responsive applications that do not block on I/O operations.
 * </p>
 *
 * <p>
 * This interface also extends {@link AutoCloseable}, allowing it to be used
 * with the Java try-with-resources statement, which guarantees that the
 * {@link #disconnect()} method is called automatically, even in the event of an
 * exception.
 *
 * @param <T> The type of the underlying database client or connection object.
 * @author Bunny
 */
public interface DatabaseProvider<T> extends AutoCloseable {
    /**
     * <p>
     * Asynchronously establishes a connection to the database. This method is
     * idempotent
     * and can be called multiple times without side effects if a connection is
     * already
     * established. The method should handle all necessary connection setup,
     * including
     * authentication and initial state validation.
     * </p>
     *
     * @return A {@link CompletableFuture<Void>} that completes when the connection
     *         is
     *         successfully established, or completes exceptionally if a connection
     *         error occurs.
     */
    CompletableFuture<Void> connect();

    /**
     * <p>
     * Asynchronously closes the active connection to the database. This method is
     * also idempotent and handles graceful shutdown of the underlying client.
     * It should release all associated resources and clean up the connection pool.
     * </p>
     *
     * @return A {@link CompletableFuture<Void>} that completes when the
     *         disconnection
     *         process is finished.
     */
    CompletableFuture<Void> disconnect();

    /**
     * <p>
     * Performs a lightweight, synchronous check to determine if the provider is
     * currently connected to the database. This method should rely on the internal
     * state of the provider and should not perform a network call.
     * </p>
     *
     * @return {@code true} if a connection is currently active, {@code false}
     *         otherwise.
     */
    boolean isConnected();

    /**
     * <p>
     * Performs a more robust health check on the active connection by attempting to
     * communicate with the database server. This can involve a lightweight command
     * like a 'ping' to ensure the server is responsive and the connection is not
     * stale.
     * </p>
     *
     * @return {@code true} if the connection is healthy and the server is
     *         responsive,
     *         {@code false} otherwise.
     */
    boolean isHealthy();

    /**
     * <p>
     * Retrieves the underlying database client or connection object. This method
     * should only be called after a successful connection has been established.
     * </p>
     *
     * @return The database client or connection object of type {@code T}.
     * @throws IllegalStateException if the provider is not currently connected.
     */
    T getConnection();

    /**
     * <p>
     * Provides a default implementation for the {@link AutoCloseable} interface,
     * allowing this provider to be used in a try-with-resources block.
     * It asynchronously disconnects and blocks the calling thread until the
     * disconnection is complete.
     * </p>
     *
     * <p>
     * Note: The use of {@code join()} makes this a blocking operation. In an
     * asynchronous context, it is generally preferred to call {@code disconnect()}
     * directly and handle the returned future.
     * </p>
     */
    @Override
    default void close() {
        disconnect().join();
    }
}
