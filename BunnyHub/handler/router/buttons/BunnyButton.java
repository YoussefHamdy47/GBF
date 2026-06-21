package org.bunnys.handler.router.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.bunnys.handler.BunnyHub;

public abstract class BunnyButton {

    /**
     * The prefix that triggers this handler (e.g., "btn" for "btn:next:12345")
     */
    public abstract String getPrefix();

    /**
     * Executes the button logic.
     * @param client The main bot instance
     * @param event The interaction event
     * @param args The split array of the component ID (e.g., ["btn", "next", "12345"])
     */
    public abstract void execute(BunnyHub client, ButtonInteractionEvent event, String[] args);
}