package org.bunnys.handler.utils.commands.handlers;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.commands.message.MessageCommandConfig;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.bunnys.handler.utils.handler.logging.TimestampUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * <h1 id="messagecommandparser-class">MessageCommandParser Class</h1>
 * <p>
 * A static utility class responsible for parsing incoming messages from Discord
 * and extracting command-related information
 * </p>
 * <p>
 * This class serves as the primary entry point for processing raw message
 * content It performs a series of checks, including prefix validation, command
 * existence, and user-based rate limiting, before a command is dispatched for
 * execution It's designed to be robust and efficient, filtering out irrelevant
 * messages and preventing abuse from a single user
 * </p>
 * <p>
 * Key features include:
 * <ul>
 * <li><b>Efficient Parsing:</b> Uses a pre-compiled regex pattern for fast
 * splitting of command and arguments</li>
 * <li><b>User Rate Limiting:</b>
 * Implements a sliding window rate limit to protect against message spam and
 * Denial-of-Service attacks on the bot</li>
 * <li><b>Error Handling:</b> Gracefully handles parsing failures and logs them
 * for debugging</li>
 * <li><b>Clear Separation of Concerns:</b> The parsing logic is encapsulated
 * here, separate from command execution and business logic</li>
 * </ul>
 * </p>
 *
 * @author Bunny
 * @see CommandRegistry
 * @see CommandVerification
 */
@SuppressWarnings("unused")
public class MessageCommandParser {
    /**
     * A pre-compiled {@link Pattern} for splitting strings by one or more
     * whitespace characters
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    /**
     * A constant for the name of this parser, used in logging
     */
    private static final String PARSER_NAME = "MessageCommandParser";

    // Rate limiting settings
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    private static final int RATE_LIMIT_MAX_ATTEMPTS = 30; // 30 per minute per user
    /**
     * A thread-safe map to store rate limit information for each user
     */
    private static final ConcurrentHashMap<String, UserRateLimit> userRateLimits = new ConcurrentHashMap<>();

    /**
     * <h2 id="parseresult-record">Record to hold parsing results</h2>
     * <p>
     * An immutable data carrier for the output of the parsing operation It provides
     * a clean and type-safe way to return the command, its name, arguments, and
     * configuration
     * </p>
     *
     * @param command       The {@link MessageCommand} instance
     * @param commandName   The name of the command
     * @param args          An array of string arguments provided by the user
     * @param commandConfig The configuration object for the command
     */
    public record ParseResult(MessageCommand command,
            String commandName,
            String[] args,
            MessageCommandConfig commandConfig) {
    }

    /**
     * <h2 id="userratelimit-class">Internal class for user rate limiting</h2>
     * <p>
     * A helper class that tracks a user's command attempts within a specified time
     * window
     * </p>
     */
    private static class UserRateLimit {
        private long windowStart;
        private int attempts;

        /**
         * Constructs a new rate limit tracker for a user, initializing the window and
         * attempt count
         */
        UserRateLimit() {
            this.windowStart = System.currentTimeMillis();
            this.attempts = 1;
        }

        /**
         * <h3 id="isratelimited">Checks and updates the user's rate limit status</h3>
         * <p>
         * This method atomically checks if the user is rate-limited If the time window
         * has expired, it resets the counter Otherwise, it increments the attempt count
         * and checks if it has exceeded the maximum allowed attempts
         * </p>
         *
         * @return `true` if the user is rate-limited, `false` otherwise
         */
        boolean isRateLimited() {
            long currentTime = System.currentTimeMillis();

            // Reset window if expired
            if (currentTime - windowStart > RATE_LIMIT_WINDOW_MS) {
                windowStart = currentTime;
                attempts = 1;
                return false;
            }

            attempts++;
            return attempts > RATE_LIMIT_MAX_ATTEMPTS;
        }

        /**
         * <h3 id="getremainingtime">Calculates the remaining time in the current rate
         * limit window</h3>
         *
         * @return The time remaining in milliseconds until the rate limit window resets
         */
        long getRemainingTime() {
            return RATE_LIMIT_WINDOW_MS - (System.currentTimeMillis() - windowStart);
        }
    }

    /**
     * <h2 id="parse">Parses a message for a valid command</h2>
     * <p>
     * This is the main public method for command parsing It first validates the
     * message author, then calls the private helper to parse the command structure
     * </p>
     *
     * @param client The bot client instance
     * @param event  The message event to parse
     * @return A {@link ParseResult} containing the parsed command data, or `null`
     *         if no valid command was found or a rate limit was hit
     */
    public static ParseResult parse(BunnyNexus client, MessageReceivedEvent event) {
        if (!shouldProcessMessage(event))
            return null;

        return parseCommandStructure(client, event);
    }

    /**
     * <h3 id="shouldprocessmessage">Determines if a message should be
     * processed</h3>
     * <p>
     * Filters out messages from bots, webhooks, or system users to prevent a bot
     * from responding to itself or other automated messages
     * </p>
     *
     * @param event The message event
     * @return `true` if the message should be processed, `false` otherwise
     */
    private static boolean shouldProcessMessage(MessageReceivedEvent event) {
        User author = event.getAuthor();

        // Filter bots, webhooks, and system messages
        return !(author.isBot() || event.isWebhookMessage() || author.isSystem());
    }

    /**
     * <h3 id="parsecommandstructure">Parses the command and arguments from the
     * message content</h3>
     * <p>
     * This method handles the core parsing logic, including prefix matching,
     * argument splitting, and looking up the command in the registry It also
     * applies the user rate limit check
     * </p>
     *
     * @param client The bot client
     * @param event  The message event
     * @return A `ParseResult` object or `null`
     */
    private static ParseResult parseCommandStructure(BunnyNexus client, MessageReceivedEvent event) {
        try {
            String prefix = client.getConfig().prefix();
            String rawContent = event.getMessage().getContentRaw();

            if (!rawContent.startsWith(prefix))
                return null;

            // Split command and arguments
            String[] parts = WHITESPACE_PATTERN.split(rawContent.substring(prefix.length()).trim());
            if (parts.length == 0)
                return null;

            String commandName = parts[0];
            String canonicalName = CommandRegistry.canonical(commandName);

            String[] args = parts.length > 1 ? java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

            CommandRegistry.CommandEntry entry = client.commandRegistry().findMessage(canonicalName);

            if (entry == null)
                return null;

            // Apply rate limit check
            if (isUserRateLimited(event.getAuthor())) {
                long remainingMs = getRemainingCooldown(event.getAuthor());

                event.getChannel().sendMessage(
                        event.getAuthor().getAsMention() + " you are being rate limited Try again "
                                + String.format("<t:%d:R>", TimestampUtils.getFutureUnixTimestamp(remainingMs / 1000)))
                        .queue(msg -> msg.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS));

                return null;
            }

            return new ParseResult(
                    entry.messageCommand(),
                    canonicalName,
                    args,
                    entry.messageMetaData());

        } catch (Exception e) {
            Logger.error("[" + PARSER_NAME + "] Failed to parse command from message in channel "
                    + event.getChannel().getId() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * <h3 id="isuserratelimited">Checks if a user has hit the rate limit</h3>
     * <p>
     * Retrieves or creates a `UserRateLimit` instance for the user and checks their
     * current status
     * </p>
     *
     * @param user The user to check
     * @return `true` if the user is rate-limited, `false` otherwise
     */
    private static boolean isUserRateLimited(User user) {
        String userId = user.getId();

        UserRateLimit rateLimit = userRateLimits.compute(userId, (key, existing) -> {
            if (existing == null)
                return new UserRateLimit();
            return existing;
        });

        return rateLimit.isRateLimited();
    }

    /**
     * <h3 id="getremainingcooldown">Gets the remaining rate limit time for a
     * user</h3>
     *
     * @param user The user to check
     * @return The remaining time in milliseconds, or 0 if not rate-limited
     */
    private static long getRemainingCooldown(User user) {
        UserRateLimit rateLimit = userRateLimits.get(user.getId());
        return (rateLimit != null) ? rateLimit.getRemainingTime() : 0;
    }

    /**
     * <h2 id="getratelimitedusercount">Gets the count of currently rate-limited
     * users</h2>
     * <p>
     * Cleans up expired rate limit entries from the map before returning the size
     * This helps prevent the map from growing indefinitely
     * </p>
     *
     * @return The number of users currently tracked for rate limiting
     */
    public static int getRateLimitedUserCount() {
        long currentTime = System.currentTimeMillis();
        userRateLimits.entrySet()
                .removeIf(entry -> currentTime - entry.getValue().windowStart > RATE_LIMIT_WINDOW_MS * 2);

        return userRateLimits.size();
    }

    /**
     * <h2 id="clearratelimits">Clears all rate limit data</h2>
     * <p>
     * A utility method to completely reset the rate limit state, useful for testing
     * or administrative purposes
     * </p>
     */
    public static void clearRateLimits() {
        userRateLimits.clear();
    }
}