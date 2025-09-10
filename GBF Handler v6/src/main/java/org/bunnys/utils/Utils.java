package org.bunnys.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility methods
 */
@SuppressWarnings("unused")
public final class Utils {

    private Utils() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Sends a message (text, embeds, or both) to the channel and deletes it after a
     * delay
     *
     * @param channel The channel to send the message to
     * @param content Optional text content (can be null)
     * @param embed   Optional embed (can be null)
     * @param delay   Delay in seconds before deleting (min 1 sec)
     * @example
     *          // Just text <br>
     *          Utils.sendAndDelete(channel, "Hello World!", 5); <br>
     *          // Just embed <br>
     *          EmbedBuilder eb = new
     *          EmbedBuilder().setTitle("Ping!").setDescription("Pong!");
     *          Utils.sendAndDelete(channel, eb, 5); <br>
     *          // Text + embed <br>
     *          Utils.sendAndDelete(channel, "Here’s some info:", eb, 10);
     */
    public static void sendAndDelete(MessageChannel channel, String content, EmbedBuilder embed, long delay) {
        MessageCreateBuilder builder = new MessageCreateBuilder();

        if (content != null && !content.isBlank())
            builder.addContent(content);

        if (embed != null)
            builder.addEmbeds(embed.build());

        MessageCreateData data = builder.build();

        if (channel instanceof PrivateChannel)
            channel.sendMessage(data).queue();
        else
            channel.sendMessage(data).queue(sent -> sent.delete().queueAfter(Math.max(1, delay), TimeUnit.SECONDS));
    }

    /**
     * Overload: just text
     */
    public static void sendAndDelete(MessageChannel channel, String content, long delay) {
        sendAndDelete(channel, content, null, delay);
    }

    /**
     * Overload: just embed
     */
    public static void sendAndDelete(MessageChannel channel, EmbedBuilder embed, long delay) {
        sendAndDelete(channel, null, embed, delay);
    }

    /**
     * Converts a string to title case (capitalizing the first letter of each word)
     * Words are assumed to be separated by underscores or spaces
     * Example: "KICK_MEMBERS" -> "Kick Members"
     *
     * @param input The input string
     * @return The title-cased string
     */
    public static String toTitleCase(String input) {
        if (input == null || input.isBlank())
            return "";

        return Arrays.stream(input.split("[_\\s]+"))
                .map(word -> word.isEmpty() ? ""
                        : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
