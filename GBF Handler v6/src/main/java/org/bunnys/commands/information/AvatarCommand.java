package org.bunnys.commands.information;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.bunnys.database.services.UserService;
import org.bunnys.executors.user.UserAvatar;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AvatarCommand extends ContextCommand {

    @Autowired
    private UserService userService;

    // No-argument constructor for reflection-based instantiation
    public AvatarCommand() {
    }

    @Override
    protected void commandOptions(ContextCommandConfig.Builder options) {
        options.name("Avatar")
                .type(Command.Type.USER)
                .cooldown(5);
    }

    @Override
    public void onUserCommand(BunnyNexus client, UserContextInteractionEvent interaction) {
        Logger.debug("UserService: " + userService); // Should not be null
        User targetUser = interaction.getTarget();
        String guildID = Objects.requireNonNull(interaction.getGuild()).getId();

        try {
            org.bunnys.database.models.User u = userService.createOrUpdateUser(targetUser.getId(),
                    targetUser.getName());
            Logger.debug("User created/updated: " + u);

            UserAvatar avatar = new UserAvatar.Builder(client, targetUser.getId())
                    .guild(interaction.getGuild() != null ? interaction.getGuild().getId() : null)
                    .embedColor(ColorCodes.DEFAULT)
                    .avatarPriority(UserAvatar.AvatarPriority.GUILD)
                    .imageFormat("png", 1024)
                    .build();

            interaction.replyEmbeds(avatar.generateEmbed(true))
                    .addComponents(ActionRow.of(avatar.getAvatarButtons()))
                    .queue();
        } catch (Exception error) {
            Logger.error("Failed to execute Avatar command for user " + targetUser.getId() + ": " + error.getMessage(),
                    error);
            interaction.reply("Failed to retrieve user data. Please try again later.").setEphemeral(true).queue();
        }
    }
}