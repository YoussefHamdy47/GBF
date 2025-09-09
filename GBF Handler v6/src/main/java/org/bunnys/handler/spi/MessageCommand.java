package org.bunnys.handler.spi;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.message.MessageCommandConfig;

/**
 * An abstract base class for implementing message-based commands
 * <p>
 * This class provides a structured and consistent way to define a command's
 * metadata
 * and its execution logic. It uses a builder pattern for configuration and
 * employs
 * lazy initialization to ensure the command's metadata is built efficiently and
 * only
 * when needed. The design is thread-safe and idempotent, making it suitable for
 * multithreaded environments
 * </p>
 *
 * @author Bunny
 */
@SuppressWarnings("unused")
public abstract class MessageCommand {
    /**
     * Lazily initialized, thread-safe snapshot of the command's configuration
     * The `volatile` keyword ensures that changes to this field are visible across
     * threads
     */
    private volatile MessageCommandConfig config;

    /**
     * Defines the command's metadata and configuration using a builder
     * <p>
     * Implementations must override this method to set properties such as the
     * command's
     * name, description, aliases, and permissions. This method is called exactly
     * once
     * during the command's lifecycle to create an immutable configuration object
     * </p>
     *
     * @param options The {@link MessageCommandConfig.Builder} instance to
     *                configure
     */
    protected abstract void commandOptions(MessageCommandConfig.Builder options);

    /**
     * Contains the core execution logic for the command
     * <p>
     * This method is invoked when a message matching the command's name or alias is
     * received,
     * The implementation should be quick and non-blocking. Any computationally
     * intensive
     * or long-running tasks should be offloaded to an appropriate executor to avoid
     * blocking the JDA event listener thread
     * </p>
     *
     * @param client  The {@link BunnyNexus} client instance, providing access to
     *                shared resources and configuration
     * @param message The {@link MessageReceivedEvent} that triggered the command.
     * @param args    A {@code String} array of command arguments, with the prefix
     *                and command name removed
     */
    public abstract void execute(BunnyNexus client, MessageReceivedEvent message, String[] args);

    /**
     * Initializes and retrieves the command's immutable configuration
     * <p>
     * This method is called by the command loader to prepare the command for use
     * It ensures
     * that the configuration is built only once and is returned on subsequent
     * calls.
     * The use of a double-checked locking pattern ensures thread safety and high
     * performance
     * for concurrent access
     * </p>
     *
     * @return The immutable {@link MessageCommandConfig} for this command
     */
    public final MessageCommandConfig initAndGetConfig() {
        // First check without locking for performance
        MessageCommandConfig local = this.config;
        if (local != null)
            return local;

        // Synchronize to ensure thread safety during initialization
        synchronized (this) {
            // Second check inside the lock
            if (this.config != null)
                return this.config;

            MessageCommandConfig.Builder builder = new MessageCommandConfig.Builder();
            this.commandOptions(builder);
            MessageCommandConfig built = builder.build();
            this.config = built; // Assign the initialized object
            return built;
        }
    }
}