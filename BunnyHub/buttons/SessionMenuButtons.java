package org.bunnys.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.bunnys.handler.BunnyHub;
import org.bunnys.handler.router.buttons.BunnyButton;
import org.bunnys.nexus.timers.buttons.SessionMenuManager;

@SuppressWarnings("unused")
public class SessionMenuButtons extends BunnyButton {

    @Override
    public String getPrefix() {
        return "session";
    }

    @Override
    public void execute(BunnyHub client, ButtonInteractionEvent event, String[] args) {
        // Expected array format from the router: ["session", "action", "userId"]
        if (args.length < 3) return;

        String actionId = args[1];
        String targetUserId = args[2];

        SessionMenuManager.handle(event, actionId, targetUserId);
    }
}