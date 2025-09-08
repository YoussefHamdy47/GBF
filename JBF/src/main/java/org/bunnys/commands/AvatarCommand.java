package org.bunnys.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.spi.ContextCommand;

import java.awt.*;

@SuppressWarnings("unused")
public class AvatarCommand extends ContextCommand {

    @Override
    protected void commandOptions(ContextCommandConfig.Builder options) {
        options.name("Avatar")
                .type(Command.Type.USER); // important: this is a User context menu
    }

    @Override
    public void onUserCommand(BunnyNexus client, UserContextInteractionEvent event) {
        var user = event.getTarget();
        String avatarUrl = user.getEffectiveAvatarUrl() + "?size=4096";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(user.getName() + "'s Avatar")
                .setImage(avatarUrl)
                .setColor(Color.CYAN);

        event.replyEmbeds(embed.build()).queue();
    }
}
