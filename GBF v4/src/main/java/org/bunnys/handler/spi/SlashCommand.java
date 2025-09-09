package org.bunnys.handler.spi;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.SlashCommandConfig;

/**
 * An abstract base class for implementing JDA slash commands
 * <p>
 * This class provides a standardized structure for defining and executing
 * Discord
 * slash commands. It leverages a builder pattern for declarative command
 * configuration
 * and uses lazy initialization to ensure the command's metadata is built
 * efficiently.
 * The implementation is thread-safe, making it suitable for concurrent use in a
 * sharded bot environment
 * </p>
 *
 * @author Bunny
 * @version 1.0
 */
@SuppressWarnings("unused")
public abstract class SlashCommand {
    /**
     * Lazily initialized, thread-safe snapshot of the command's configuration
     * The `volatile` keyword guarantees that the `config` field's value is always
     * read from main memory, ensuring visibility across multiple threads
     */
    private volatile SlashCommandConfig config;

    /**
     * Defines the command's metadata and configuration using a builder
     * <p>
     * Implementations must override this method to specify the command's name,
     * description, and any subcommand groups, subcommands, or options
     * This method
     * is called once by the command loader to create an immutable configuration
     * object
     * </p>
     *
     * @param options The {@link SlashCommandConfig.Builder} instance to configure
     */
    protected abstract void commandOptions(SlashCommandConfig.Builder options);

    /**
     * Contains the core execution logic for the command
     * <p>
     * This method is called by the JDA event listener when the command is invoked
     * by a user. All command-specific actions, such as interacting with a database,
     * making an API call, or sending a reply, should be handled here
     * </p>
     *
     * @param client The {@link BunnyNexus} client instance, providing access to
     *               shared resources and the bot's state
     * @param event  The {@link SlashCommandInteractionEvent} that triggered the
     *               command, providing
     *               all necessary context and user input
     */
    public abstract void execute(BunnyNexus client, SlashCommandInteractionEvent event);

    /**
     * Initializes and retrieves the command's immutable configuration
     * <p>
     * This method is designed to be called by the command loader to prepare the
     * command
     * for registration with Discord. It uses a double-checked locking pattern to
     * ensure thread-safe, lazy initialization of the command configuration,
     * guaranteeing
     * that the expensive build operation occurs only once
     * </p>
     *
     * @return The immutable {@link SlashCommandConfig} for this command.
     */
    public final SlashCommandConfig initAndGetConfig() {
        // First check without locking for performance
        SlashCommandConfig local = this.config;
        if (local != null)
            return local;

        // Synchronize to ensure thread safety during initialization
        synchronized (this) {
            // Second check inside the lock
            if (this.config != null)
                return this.config;

            SlashCommandConfig.Builder builder = new SlashCommandConfig.Builder();
            this.commandOptions(builder);
            SlashCommandConfig built = builder.build();
            this.config = built; // Assign the initialized object
            return built;
        }
    }

    public void onAutoComplete(BunnyNexus client, CommandAutoCompleteInteractionEvent event) {}
}