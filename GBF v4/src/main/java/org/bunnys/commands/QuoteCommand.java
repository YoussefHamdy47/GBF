package org.bunnys.commands;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.spi.ContextCommand;

@SuppressWarnings("unused")
public class QuoteCommand extends ContextCommand {

    @Override
    protected void commandOptions(ContextCommandConfig.Builder options) {
        options.name("Quote Message")
                .type(Command.Type.MESSAGE); // important: this is a Message context menu
    }

    @Override
    public void onMessageCommand(BunnyNexus client, MessageContextInteractionEvent event) {
        var targetMsg = event.getTarget();

        String content = targetMsg.getContentDisplay();
        if (content.isEmpty()) {
            content = "*[Message has no text content]*";
        }

        String quoted = "> " + content + "\n— " + targetMsg.getAuthor().getAsTag();

        event.reply(quoted).setEphemeral(false).queue();
    }
}
