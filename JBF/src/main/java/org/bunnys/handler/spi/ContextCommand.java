package org.bunnys.handler.spi;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.ContextCommandConfig;

/**
 * Abstract base for JDA Context Menu Commands (User & Message).
 *
 * Provides standardized structure for defining and executing context commands.
 */
public abstract class ContextCommand {
    private volatile ContextCommandConfig config;

    /** Configure the command’s metadata */
    protected abstract void commandOptions(ContextCommandConfig.Builder options);

    /** Execution logic for User commands */
    public void onUserCommand(BunnyNexus client, UserContextInteractionEvent event) {}

    /** Execution logic for Message commands */
    public void onMessageCommand(BunnyNexus client, MessageContextInteractionEvent event) {}

    /** Lazy initialization of the config */
    public final ContextCommandConfig initAndGetConfig() {
        ContextCommandConfig local = this.config;
        if (local != null) return local;

        synchronized (this) {
            if (this.config != null) return this.config;

            ContextCommandConfig.Builder builder = new ContextCommandConfig.Builder();
            this.commandOptions(builder);
            ContextCommandConfig built = builder.build();
            this.config = built;
            return built;
        }
    }
}
