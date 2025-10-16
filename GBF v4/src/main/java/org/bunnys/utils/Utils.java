package org.bunnys.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
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

    /**
     * Convert milliseconds to a human-readable time string.
     * Examples: "2d 3h 45m", "3h 12m", "45s"
     * This is a simple version of your msToTime. It's deterministic and concise.
     *
     * @param milliseconds input ms
     * @return human-readable duration (short form)
     */
    public static String msToTime(long milliseconds) {
        if (milliseconds <= 0)
            return "0s";

        long secondsTotal = Math.abs(milliseconds / 1000);
        long years = secondsTotal / 31_557_600L; // approx year
        long days = (secondsTotal % 31_557_600L) / 86_400L;
        long hours = (secondsTotal % 86_400L) / 3_600L;
        long minutes = (secondsTotal % 3_600L) / 60L;
        long seconds = secondsTotal % 60L;

        StringBuilder sb = new StringBuilder();
        if (years > 0)
            sb.append(years).append(" years ");
        if (days > 0)
            sb.append(days).append(" days ");
        if (hours > 0)
            sb.append(hours).append(" hours ");
        if (minutes > 0)
            sb.append(minutes).append(" minutes ");
        if (seconds > 0 || sb.isEmpty())
            sb.append(seconds).append(" seconds");

        return sb.toString().trim();
    }

    /**
     * A convenience alias used by TimerEvents previously named formatDuration(...)
     *
     * @param milliseconds duration in ms
     * @return formatted string, e.g. "1h 2m 3s"
     */
    public static String formatDuration(long milliseconds) {
        return msToTime(milliseconds);
    }

    /**
     * Return a Discord-style formatted timestamp string: e.g. "<t:1672531199:F>"
     * Supported types: 'd','D','t','T','f','F','R' (same as your TS helper)
     * If the type is invalid, returns ISO string fallback.
     *
     * @param date date
     * @param type one of the characters above, e.g. 'F'
     * @return formatted timestamp string
     */
    public static String getTimestamp(Date date, char type) {
        if (date == null)
            return "null";
        long unixSeconds = date.toInstant().getEpochSecond();
        return switch (type) {
            case 'd', 'D', 't', 'T', 'f', 'F', 'R' -> "<t:" + unixSeconds + ":" + type + ">";
            default ->
                    Instant.ofEpochSecond(unixSeconds).atZone(ZoneOffset.UTC).toString();
        };
    }

    /**
     * Simple formatTimestamp used by TimerEvents for human-readable fallback
     * (non-discord)
     *
     * @param date date
     * @return date.toString() (or you can improve to a DateTimeFormatter)
     */
    public static String formatTimestamp(Date date) {
        if (date == null)
            return "null";
        return date.toString();
    }

    public static <T> T chooseRandomFromArray(T[] array) {
        if (array == null || array.length == 0)
            return null;
        int idx = (int) (Math.random() * array.length);
        return array[idx];
    }

    public static <T> T getRandomFromArray(java.util.List<T> list) {
        if (list == null || list.isEmpty())
            return null;
        int idx = (int) (Math.random() * list.size());
        return list.get(idx);
    }
}
