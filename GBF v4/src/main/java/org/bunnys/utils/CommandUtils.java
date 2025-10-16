package org.bunnys.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.bunnys.handler.utils.handler.Emojis;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CommandUtils {

    private record ErrorMessage(String title, String description) {
    }

    public static ErrorMessage createErrorMessage(String commandName, String traceId, boolean isTimeout) {
        if (isTimeout) {
            return new ErrorMessage(
                    Emojis.DEFAULT_ERROR + " Command Timed Out",
                    String.format("Your command `/%s` took too long and was cancelled.%n" +
                            "Please try again in a moment. (`%s`)", commandName, traceId));
        } else
            return new ErrorMessage(
                    Emojis.DEFAULT_ERROR + " Something went wrong",
                    String.format("We ran into an issue while running `/%s`. This isn't your fault.%n" +
                            "Please try again. If this keeps happening, share this code with support: `%s`",
                            commandName, traceId));
    }

    public static void sendUserErrorMessage(@NotNull SlashCommandInteractionEvent event,
            @NotNull String title,
            @NotNull String description) {
        try {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(ColorCodes.ERROR_RED)
                    .setTitle(title)
                    .setDescription(description);

            if (event.isAcknowledged())
                event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
            else
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();

        } catch (Exception e) {
            event.reply("Failed to send error message. Please try again.").setEphemeral(true).queue();
        }
    }

    public static String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}