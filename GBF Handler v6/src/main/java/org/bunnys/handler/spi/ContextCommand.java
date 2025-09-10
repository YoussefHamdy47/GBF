package org.bunnys.handler.spi;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.ContextCommandConfig;

/**
 * An abstract base class for JDA Context Menu Commands (User & Message)
 *
 * <p>
 * This class provides a standardized structure for defining and executing
 * context menu commands,
 * ensuring a consistent pattern across different command implementations
 * </p>
 */
public abstract class ContextCommand {
    private volatile ContextCommandConfig config;

    /**
     * Configures the command’s metadata and options
     *
     * <p>
     * Subclasses must override this method to define the command's name, type, and
     * any other
     * relevant settings using the provided builder
     * </p>
     *
     * @param options The builder for configuring the command's metadata
     */
    protected abstract void commandOptions(ContextCommandConfig.Builder options);

    /**
     * The execution logic for User Context Menu Commands
     *
     * <p>
     * This method is invoked when a user right-clicks on another user and executes
     * a context command
     * </p>
     *
     * @param client The {@link BunnyNexus} client instance
     * @param event  The {@link UserContextInteractionEvent} for the command
     *               interaction
     */
    public void onUserCommand(BunnyNexus client, UserContextInteractionEvent event) {
    }

    /**
     * The execution logic for Message Context Menu Commands
     *
     * <p>
     * This method is invoked when a user right-clicks on a message and executes
     * a context command
     * </p>
     *
     * @param client The {@link BunnyNexus} client instance
     * @param event  The {@link MessageContextInteractionEvent} for the command
     *               interaction
     */
    public void onMessageCommand(BunnyNexus client, MessageContextInteractionEvent event) {
    }

    /**
     * Initializes and retrieves the command's configuration
     *
     * <p>
     * This method uses a lazy initialization pattern to ensure that the command's
     * configuration
     * is built only once when it's first needed, providing thread-safe access
     * </p>
     *
     * @return The immutable {@link ContextCommandConfig} for this command
     */
    public final ContextCommandConfig initAndGetConfig() {
        ContextCommandConfig local = this.config;
        if (local != null)
            return local;

        synchronized (this) {
            if (this.config != null)
                return this.config;

            ContextCommandConfig.Builder builder = new ContextCommandConfig.Builder();
            this.commandOptions(builder);
            ContextCommandConfig built = builder.build();
            this.config = built;
            return built;
        }
    }
}