package org.bunnys.handler.commands.slash;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A utility engine for handling Discord slash command autocompletion
 * functionality
 *
 * <p>
 * This class provides static methods to generate and respond with
 * autocompletion
 * suggestions for Discord slash commands based on user input. It automatically
 * filters
 * and limits suggestions according to Discord's API constraints
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Case-insensitive prefix matching for suggestions</li>
 * <li>Automatic limiting to Discord's maximum of 25 choices</li>
 * <li>Null-safe input handling with appropriate fallbacks</li>
 * <li>Comprehensive error handling and logging</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * 
 * <pre>{@code
 * List<String> gameNames = Arrays.asList("Overwatch", "Valorant", "Minecraft");
 * AutocompleteEngine.suggest(event, "min", gameNames);
 * // Will suggest "Minecraft" as it matches the "min" prefix
 * }</pre>
 *
 * @author Bunny
 * @see CommandAutoCompleteInteractionEvent
 * @see Command.Choice
 */
@SuppressWarnings("unused")
public final class AutocompleteEngine {
    /**
     * Discord's maximum number of autocomplete choices that can be returned
     * As per Discord API documentation, this limit is strictly enforced
     */
    private static final int MAX_DISCORD_CHOICES = 25;

    /**
     * Private constructor to prevent instantiation of this utility class
     *
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private AutocompleteEngine() {
        throw new UnsupportedOperationException("AutocompleteEngine is a utility class and cannot be instantiated");
    }

    /**
     * Generates and sends autocompletion suggestions based on user input
     *
     * <p>
     * This method filters the provided values list to find entries that start with
     * the given input (case-insensitive), limits the results to Discord's maximum
     * allowed choices (25), and sends the response back to Discord
     *
     * <p>
     * The filtering logic:
     * <ul>
     * <li>Performs case-insensitive prefix matching using {@code startsWith()}</li>
     * <li>Null or empty input strings are treated as empty prefixes (match
     * all)</li>
     * <li>Null values in the values list are filtered out</li>
     * <li>Results are automatically limited to {@value #MAX_DISCORD_CHOICES}
     * entries</li>
     * </ul>
     *
     * <p>
     * Error handling:
     * <ul>
     * <li>Null events are handled gracefully with warning logs</li>
     * <li>Null or empty values lists result in empty suggestion responses</li>
     * <li>Any exceptions during processing are logged and result in empty
     * responses</li>
     * </ul>
     *
     * @param event  the Discord autocomplete interaction event to respond to
     *               Must not be null
     * @param input  the user's current input string to filter suggestions against
     *               Can be null or empty (treated as matching all values)
     * @param values the list of possible values to suggest from
     *               Can be null or empty (results in no suggestions)
     *               Null entries within the list are automatically filtered out
     *
     * @throws IllegalArgumentException if the event parameter is null
     *
     * @see Command.Choice#Choice(String, String)
     */
    public static void suggest(@NotNull CommandAutoCompleteInteractionEvent event,
            @Nullable String input,
            @Nullable List<String> values) {
        Objects.requireNonNull(event, "CommandAutoCompleteInteractionEvent cannot be null");

        try {
            // Handle edge cases for input parameters
            if (values == null || values.isEmpty()) {
                Logger.debug(() -> "Empty or null values list provided for autocomplete suggestion");
                event.replyChoices(Collections.emptyList()).queue();
                return;
            }

            // Normalize input - treat null/empty as empty string for consistent behavior
            final String normalizedInput = (input != null) ? input.toLowerCase().trim() : "";

            // Generate filtered choices with comprehensive error handling
            List<Command.Choice> choices = values.stream()
                    .filter(Objects::nonNull) // Filter out null values
                    .filter(choice -> choice.toLowerCase().startsWith(normalizedInput))
                    .limit(MAX_DISCORD_CHOICES)
                    .map(choice -> new Command.Choice(choice, choice))
                    .toList();

            Logger.debug(() -> "Generated " + choices.size() + " autocomplete choices for input: '" + input + "'");

            // Send response to Discord
            event.replyChoices(choices).queue(
                    success -> Logger.debug(() -> "Successfully sent autocomplete response"),
                    failure -> Logger.warning("Failed to send autocomplete response: " + failure.getMessage()));

        } catch (Exception e) {
            Logger.error("Unexpected error while processing autocomplete suggestions for input: '" + input + "'", e);
            try {
                event.replyChoices(Collections.emptyList()).queue();
            } catch (Exception fallbackException) {
                Logger.error("Failed to send fallback empty autocomplete response", fallbackException);
            }
        }
    }

    /**
     * Generates and sends autocompletion suggestions with custom choice limiting
     *
     * <p>
     * This overloaded method provides the same functionality as
     * {@link #suggest(CommandAutoCompleteInteractionEvent, String, List)} but
     * allows
     * specifying a custom maximum number of choices to return, up to Discord's
     * limit
     *
     * @param event      the Discord autocomplete interaction event to respond to
     *                   Must not be null
     * @param input      the user's current input string to filter suggestions
     *                   against
     *                   Can be null or empty (treated as matching all values)
     * @param values     the list of possible values to suggest from
     *                   Can be null or empty (results in no suggestions)
     * @param maxChoices the maximum number of choices to return.
     *                   Must be between 1 and {@value #MAX_DISCORD_CHOICES}
     *                   inclusive
     *
     * @throws IllegalArgumentException if the event is null or maxChoices is out of
     *                                  valid range
     *
     * @since 1.1
     */
    public static void suggest(@NotNull CommandAutoCompleteInteractionEvent event,
            @Nullable String input,
            @Nullable List<String> values,
            int maxChoices) {
        Objects.requireNonNull(event, "CommandAutoCompleteInteractionEvent cannot be null");

        if (maxChoices < 1 || maxChoices > MAX_DISCORD_CHOICES)
            throw new IllegalArgumentException(
                    String.format("maxChoices must be between 1 and %d, got: %d", MAX_DISCORD_CHOICES, maxChoices));

        try {
            if (values == null || values.isEmpty()) {
                Logger.debug(() -> "Empty or null values list provided for autocomplete suggestion");
                event.replyChoices(Collections.emptyList()).queue();
                return;
            }

            final String normalizedInput = (input != null) ? input.toLowerCase().trim() : "";

            List<Command.Choice> choices = values.stream()
                    .filter(Objects::nonNull)
                    .filter(choice -> choice.toLowerCase().startsWith(normalizedInput))
                    .limit(maxChoices)
                    .map(choice -> new Command.Choice(choice, choice))
                    .toList();

            Logger.debug(() -> "Generated " + choices.size() +
                    " autocomplete choices (max: " + maxChoices + ") for input: '" + input + "'");

            event.replyChoices(choices).queue(
                    success -> Logger.debug(() -> "Successfully sent autocomplete response"),
                    failure -> Logger.warning("Failed to send autocomplete response: " + failure.getMessage()));

        } catch (Exception e) {
            Logger.error("Unexpected error while processing autocomplete suggestions for input: '" + input + "'", e);
            try {
                event.replyChoices(Collections.emptyList()).queue();
            } catch (Exception fallbackException) {
                Logger.error("Failed to send fallback empty autocomplete response", fallbackException);
            }
        }
    }
}