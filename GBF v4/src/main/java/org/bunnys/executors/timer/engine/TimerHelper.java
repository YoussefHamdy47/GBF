package org.bunnys.executors.timer.engine;

import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bunnys.database.models.timer.Semester;
import org.bunnys.database.models.timer.TimerData;
import org.bunnys.database.models.users.GBFUser;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * A comprehensive utility class for managing timer-related operations,
 * particularly
 * for a Discord bot's timer subsystem. This class provides helper methods for
 * formatting time, generating dynamic UI components (like buttons), and
 * encapsulating
 * key data structures used in the timer's logic flow.
 * </p>
 *
 * <p>
 * The class is designed to be a stateless, static helper, making it safe for
 * concurrent use across multiple threads handling user interactions. It defines
 * enums for button IDs and event return messages to ensure consistency and
 * type-safety in the application's command handling logic.
 * </p>
 */
@SuppressWarnings("unused")
public class TimerHelper {
    /**
     * <p>
     * Defines the canonical IDs for the interactive buttons used in the timer
     * interface. Using this enum ensures that button IDs are consistent, typo-safe,
     * and easily manageable across the application.
     * </p>
     *
     * <p>
     * This approach simplifies button handling by allowing developers to switch on
     * the enum rather than relying on raw string comparisons.
     * </p>
     */
    public enum TimerButtonID {
        Start("startTimer"),
        Pause("pauseTimer"),
        Info("timerInfo"),
        Stop("stopTimer"),
        Unpause("unpauseTimer");

        private final String ID;

        TimerButtonID(String ID) {
            this.ID = ID;
        }

        /**
         * Returns the unique string identifier for the button. This ID is used by the
         * JDA library to recognize and handle button interactions.
         *
         * @return The string ID of the button.
         */
        public String getID() {
            return this.ID;
        }
    }

    /**
     * <p>
     * Encapsulates standard return messages for various timer events. This enum
     * centralizes all user-facing messages, ensuring consistent wording and tone
     * for feedback provided by the timer system. This promotes a better user
     * experience and simplifies internationalization efforts if needed in the
     * future.
     * </p>
     */
    public enum TimerEventsReturns {
        TimerAlreadyRunning("You have an active session."),
        TimerStarted("Timer Started"),
        TimerAlreadyPaused("The timer is already paused, stop it before starting a new break."),
        TimerNotStarted("You don't have an active session."),
        TimerNotPaused("The timer is not paused."),
        CannotStopPaused("You cannot end the session when the timer is paused.");

        private final String message;

        TimerEventsReturns(String message) {
            this.message = message;
        }

        /**
         * Retrieves the human-readable message associated with a timer event.
         *
         * @return The event message string.
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * Defines the types of progression activities tracked by the system.
     */
    public enum ActivityType {
        SESSION,
        SEMESTER
    }

    /**
     * <p>
     * A record to encapsulate the options for fixing or dynamically
     * enabling/disabling
     * timer buttons. This DTO simplifies method signatures and ensures that
     * related parameters are grouped together logically.
     * </p>
     *
     * @param enabledButtons A {@link List} of {@link TimerButtonID}s that should be
     *                       enabled.
     * @param isPaused       A boolean indicating whether the timer is currently in
     *                       a paused state.
     */
    public record ButtonFixerOptions(List<TimerButtonID> enabledButtons, boolean isPaused) {
    }

    /**
     * <p>
     * A record to hold data related to a record-breaking event. This DTO
     * facilitates
     * the transfer of context when a user achieves a new personal best (e.g.,
     * longest session, longest semester).
     * </p>
     *
     * @param type        The {@link ActivityType} of the record broken (e.g.,
     *                    SESSION or SEMESTER).
     * @param interaction The {@link SlashCommandInteraction} context, providing
     *                    access to guild and channel details.
     * @param sessionTime The duration of the session in milliseconds, or null if
     *                    not applicable.
     * @param semester    The {@link Semester} data, or null if not applicable.
     */
    public record RecordBrokenOptions(ActivityType type, SlashCommandInteraction interaction, Long sessionTime,
            Semester semester) {
    }

    /**
     * <p>
     * A record to encapsulate all the necessary data for handling a level-up event.
     * This DTO is used to pass a complete state snapshot to the level-up processing
     * logic.
     * </p>
     *
     * @param interaction The {@link SlashCommandInteraction} that triggered the
     *                    level-up.
     * @param levelUps    The number of levels gained in this event.
     * @param carryOverXP The amount of XP remaining after the level-up.
     * @param timerData   The user's {@link TimerData} to access timer-specific
     *                    stats.
     * @param userData    The user's {@link GBFUser} data, providing access to
     *                    progression metrics.
     */
    public record LevelUpOptions(SlashCommandInteraction interaction, int levelUps, int carryOverXP,
            TimerData timerData, GBFUser userData) {
    }

    // --- Utility Functions --- //

    /**
     * <p>
     * Formats a duration in hours into a more human-readable string representation,
     * including both minutes and hours. This method is useful for displaying study
     * session durations in a user-friendly manner.
     * </p>
     *
     * @param hours The duration in hours (can be fractional).
     * @return A formatted string, e.g., "90m (1.500 hours)" or "0.500 hours".
     */
    public static String formatHours(double hours) {
        int minutes = (int) Math.round(hours * 60);
        return minutes > 0
                ? minutes + "m (" + String.format("%.3f", hours) + " hours)"
                : String.format("%.3f", hours) + " hours";
    }

    /**
     * <p>
     * Dynamically creates and returns a JDA {@link ActionRow} containing the
     * standard
     * timer control buttons. The state of these buttons (enabled/disabled and text)
     * is determined by the provided parameters, allowing for responsive UI updates.
     * </p>
     *
     * @param disabledButtons A {@link Map} where the key is a {@link TimerButtonID}
     *                        and the
     *                        value is a {@link Boolean} indicating whether the
     *                        button should be disabled.
     * @param isPaused        A boolean indicating whether the timer is currently
     *                        paused, which
     *                        alters the "Pause" button's text and ID to "Unpause".
     * @return An {@link ActionRow} object ready to be sent to a Discord channel.
     */
    public static ActionRow createTimerActionRow(Map<TimerButtonID, Boolean> disabledButtons, boolean isPaused) {
        return ActionRow.of(
                Button.secondary(TimerButtonID.Start.getID(), "🕛 Start Session")
                        .withDisabled(disabledButtons.getOrDefault(TimerButtonID.Start, false)),

                Button.secondary(isPaused ? TimerButtonID.Unpause.getID() : TimerButtonID.Pause.getID(),
                        isPaused ? "▶️ Unpause Timer" : "⏰ Pause Timer")
                        .withDisabled(disabledButtons.getOrDefault(
                                isPaused ? TimerButtonID.Unpause : TimerButtonID.Pause, false)),

                Button.secondary(TimerButtonID.Info.getID(), "ℹ️ Session Stats")
                        .withDisabled(disabledButtons.getOrDefault(TimerButtonID.Info, false)),

                Button.secondary(TimerButtonID.Stop.getID(), "🕧 End Session")
                        .withDisabled(disabledButtons.getOrDefault(TimerButtonID.Stop, false)));
    }

    /**
     * <p>
     * Constructs a Discord message URL from a guild, channel, and message ID.
     * This method is useful for creating permalinks to specific messages, which
     * can be embedded in replies or logging to provide a direct link to the timer
     * UI.
     * </p>
     *
     * @param guildID   The ID of the Discord guild.
     * @param channelID The ID of the Discord channel.
     * @param messageID The ID of the Discord message.
     * @return The complete Discord message URL as a {@link String}.
     */
    public static String messageURL(String guildID, String channelID, String messageID) {
        return "https://discord.com/channels/" + guildID + "/" + channelID + "/" + messageID;
    }
}