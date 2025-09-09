package org.bunnys.handler.utils.commands.handlers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.message.MessageCommandConfig;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.utils.handler.Emojis;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A utility class for validating command execution before it is handled.
 * <p>
 * This class provides a centralized and robust mechanism to verify various
 * preconditions
 * for command execution, such as user and bot permissions, developer-only
 * access,
 * channel restrictions (NSFW), and command cooldowns. It supports both
 * message-based
 * and slash commands by abstracting common validation logic.
 * <p>
 * The primary entry points are the {@code validateExecution} methods, which
 * return a
 * {@link ValidationResult} object. This object encapsulates the outcome of the
 * validation,
 * including the type of failure and a user-friendly message. This design
 * promotes
 * clean separation of concerns, allowing command handlers to simply check the
 * result
 * and delegate error-handling logic back to this class.
 * <p>
 * This class is a utility and cannot be instantiated. All methods are static.
 *
 * @author Bunny
 */
@SuppressWarnings("unused")
public final class CommandVerification {

    private static final Logger logger = LoggerFactory.getLogger(CommandVerification.class);

    // Constants
    private static final int AUTO_DELETE_SECONDS = 5;
    private static final String PERMISSION_DELIMITER = ", ";

    // Error messages
    private static final String ERROR_DEV_ONLY = "Only **developers** can use this command.";
    private static final String ERROR_DM_NOT_SUPPORTED = "This command cannot be used outside of a server.";
    private static final String ERROR_USER_PERMISSIONS = "You are missing the following permissions:";
    private static final String ERROR_BOT_PERMISSIONS = "I am missing the following permissions:";
    private static final String ERROR_NSFW_ONLY = "This command can only be used in **Age-Restricted** channels.";

    // Titles
    private static final String TITLE_ACCESS_DENIED = "You cannot do that";
    private static final String TITLE_NOT_SUPPORTED = "Not Supported";
    private static final String TITLE_MISSING_PERMISSIONS = "Missing Permissions";
    private static final String TITLE_COOLDOWN_ACTIVE = "You can't use that yet";
    private static final String TITLE_NSFW_REQUIRED = "NSFW Command";

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always.
     */
    private CommandVerification() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ---------------------------
    // Public validation entrypoints
    // ---------------------------

    /**
     * Validates the execution context for a message-based command.
     * <p>
     * This method orchestrates the full validation process for a command triggered
     * by a standard message.
     * It checks for NSFW channel requirements, developer-only access, guild
     * context, user permissions,
     * bot permissions, and command cooldowns.
     *
     * @param client The {@link BunnyNexus} client instance.
     * @param event  The {@link MessageReceivedEvent} from JDA.
     * @param config The {@link MessageCommandConfig} for the command being
     *               validated.
     * @return A {@link ValidationResult} indicating whether the command can be
     *         executed and, if not, the reason.
     * @throws NullPointerException if any of the input parameters are null.
     */
    public static ValidationResult validateExecution(BunnyNexus client,
            MessageReceivedEvent event,
            MessageCommandConfig config) {

        Objects.requireNonNull(client, "Client cannot be null");
        Objects.requireNonNull(event, "Event object cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        Guild guild = event.getGuild();
        Member member = event.getMember();

        return validateCommon(
                /* nsfwRequired */ config.NSFW(),
                /* isNsfw */ event.getChannelType().isGuild() && event.getChannel().asTextChannel().isNSFW(),
                /* devOnly */ config.devOnly(),
                /* isDeveloper */ isDeveloper(client, event.getAuthor()),
                /* guild */ guild,
                /* invoking member */ member,
                /* userPerms */ config.userPermissions(),
                /* botPerms */ config.botPermissions(),
                /* user */ event.getAuthor(),
                /* commandName */ config.commandName(),
                /* cooldownMs */ config.cooldown());
    }

    /**
     * Validates the execution context for a slash command.
     * <p>
     * This method is the slash command equivalent of
     * {@link #validateExecution(BunnyNexus, MessageReceivedEvent, MessageCommandConfig)}.
     * It performs the same series of checks tailored for the
     * {@link SlashCommandInteractionEvent} context.
     *
     * @param client The {@link BunnyNexus} client instance.
     * @param event  The {@link SlashCommandInteractionEvent} from JDA.
     * @param config The {@link SlashCommandConfig} for the command being validated.
     * @return A {@link ValidationResult} indicating whether the command can be
     *         executed and, if not, the reason.
     * @throws NullPointerException if any of the input parameters are null.
     */
    public static ValidationResult validateExecution(BunnyNexus client,
            SlashCommandInteractionEvent event,
            SlashCommandConfig config) {

        Objects.requireNonNull(client, "Client cannot be null");
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");

        Guild guild = event.getGuild();
        Member member = event.getMember();

        boolean isNsfw = guild != null && event.getChannel().asTextChannel().isNSFW();

        return validateCommon(
                /* nsfwRequired */ config.NSFW(),
                /* isNsfw */ isNsfw,
                /* devOnly */ config.devOnly(),
                /* isDeveloper */ isDeveloper(client, event.getUser()),
                /* guild */ guild,
                /* invoking member */ member,
                /* userPerms */ config.userPermissions(),
                /* botPerms */ config.botPermissions(),
                /* user */ event.getUser(),
                /* commandName */ config.name(),
                /* cooldownMs */ config.cooldown());
    }

    /**
     * A convenience method to perform validation and handle any failures for a
     * message command.
     * <p>
     * This method is a shortcut for the common pattern of validating a command and
     * immediately
     * handling a failed result. It returns {@code true} if the command is valid and
     * should be executed,
     * and {@code false} otherwise.
     *
     * @param client The {@link BunnyNexus} client instance.
     * @param event  The {@link MessageReceivedEvent} to validate.
     * @param config The {@link MessageCommandConfig} for the command.
     * @return {@code true} if the command can be executed, {@code false} if
     *         validation failed.
     */
    public static boolean canExecute(BunnyNexus client,
            MessageReceivedEvent event,
            MessageCommandConfig config) {
        ValidationResult result = validateExecution(client, event, config);

        if (result.hasFailed()) {
            handleValidationFailure(event, result);
            return false;
        }
        return true;
    }

    // ---------------------------
    // Private, shared validation core
    // ---------------------------

    /**
     * The core logic for command validation, shared by both message and slash
     * command entry points.
     * <p>
     * This method performs a series of sequential checks and returns a failure
     * result as soon as
     * the first validation rule is violated. The checks are ordered by their
     * computational cost,
     * with simple checks like NSFW and dev-only flags coming first.
     *
     * @param nsfwRequired   Whether the command must be used in an NSFW channel.
     * @param isNsfw         Whether the current channel is NSFW.
     * @param devOnly        Whether the command is restricted to developers.
     * @param isDeveloper    Whether the invoking user is a developer.
     * @param guild          The guild where the command was invoked, or null if in
     *                       a DM.
     * @param invokingMember The member who invoked the command, or null if in a DM.
     * @param userPerms      A collection of required user permissions.
     * @param botPerms       A collection of required bot permissions.
     * @param user           The user who invoked the command.
     * @param commandName    The name of the command.
     * @param cooldownMs     The cooldown duration in milliseconds.
     * @return A {@link ValidationResult} object.
     */
    private static ValidationResult validateCommon(boolean nsfwRequired,
            boolean isNsfw,
            boolean devOnly,
            boolean isDeveloper,
            Guild guild,
            Member invokingMember,
            Collection<Permission> userPerms,
            Collection<Permission> botPerms,
            User user,
            String commandName,
            long cooldownMs) {

        // NSFW check
        if (nsfwRequired && !isNsfw) {
            return ValidationResult.failure(
                    ValidationFailureType.NSFW_REQUIRED,
                    TITLE_NSFW_REQUIRED,
                    ERROR_NSFW_ONLY);
        }

        // Dev-only check
        if (devOnly && !isDeveloper) {
            return ValidationResult.failure(
                    ValidationFailureType.DEVELOPER_ONLY,
                    TITLE_ACCESS_DENIED,
                    ERROR_DEV_ONLY);
        }

        // Guild/Member context
        if (guild == null || invokingMember == null) {
            return ValidationResult.failure(
                    ValidationFailureType.INVALID_CONTEXT,
                    TITLE_NOT_SUPPORTED,
                    ERROR_DM_NOT_SUPPORTED);
        }

        // User permissions
        ValidationResult userPermResult = validateMemberPermissions(
                invokingMember,
                userPerms,
                ValidationFailureType.INSUFFICIENT_USER_PERMISSIONS,
                ERROR_USER_PERMISSIONS);
        if (userPermResult.hasFailed())
            return userPermResult;

        // Bot permissions
        ValidationResult botPermResult = validateMemberPermissions(
                guild.getSelfMember(),
                botPerms,
                ValidationFailureType.INSUFFICIENT_BOT_PERMISSIONS,
                ERROR_BOT_PERMISSIONS);
        if (botPermResult.hasFailed())
            return botPermResult;

        // Cooldown
        ValidationResult cooldown = validateCooldown(commandName, cooldownMs, user);
        if (cooldown.hasFailed())
            return cooldown;

        return ValidationResult.success();
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    /**
     * Checks if a given user is a registered developer.
     * <p>
     * The list of developer IDs is retrieved from the client's configuration.
     *
     * @param client The {@link BunnyNexus} client.
     * @param user   The user to check.
     * @return {@code true} if the user's ID is in the developer list, {@code false}
     *         otherwise.
     */
    private static boolean isDeveloper(BunnyNexus client, User user) {
        String[] developers = client.getConfig().developers();
        if (developers == null || developers.length == 0)
            return false;
        String userId = user.getId();
        for (String id : developers) {
            if (Objects.equals(id, userId))
                return true;
        }
        return false;
    }

    /**
     * Validates if a member has a set of required permissions.
     * <p>
     * If the member is missing any required permission, a failure result is
     * returned,
     * including a formatted string of the missing permissions.
     *
     * @param member              The member to check permissions for.
     * @param requiredPermissions The permissions required to run the command.
     * @param failureType         The type of failure to return if permissions are
     *                            missing.
     * @param baseMessage         The base error message for the failure.
     * @return A {@link ValidationResult} indicating success or failure due to
     *         missing permissions.
     */
    private static ValidationResult validateMemberPermissions(Member member,
            Collection<Permission> requiredPermissions,
            ValidationFailureType failureType,
            String baseMessage) {
        if (requiredPermissions == null || requiredPermissions.isEmpty())
            return ValidationResult.success();

        List<Permission> missing = requiredPermissions.stream()
                .filter(p -> !member.hasPermission(p))
                .toList();

        if (!missing.isEmpty()) {
            String formatted = missing.stream()
                    .map(CommandVerification::humanPermissionName)
                    .collect(Collectors.joining(PERMISSION_DELIMITER));

            String description = baseMessage + "\n`" + formatted + "`";
            return ValidationResult.failure(failureType, TITLE_MISSING_PERMISSIONS, description);
        }

        return ValidationResult.success();
    }

    /**
     * Checks if a command is on cooldown for a specific user.
     * <p>
     * This method delegates to a {@link CooldownManager} (assumed to exist
     * externally) to check
     * the cooldown status. If a command is on cooldown, it returns a specialized
     * {@link ValidationResult} that includes the remaining cooldown time.
     *
     * @param commandName The name of the command.
     * @param cooldownMs  The cooldown duration in milliseconds.
     * @param user        The user to check the cooldown for.
     * @return A {@link ValidationResult} indicating cooldown status.
     */
    private static ValidationResult validateCooldown(String commandName, long cooldownMs, User user) {
        if (cooldownMs <= 0)
            return ValidationResult.success();

        final String userId = user.getId();
        if (CooldownManager.isOnCooldown(commandName, userId, cooldownMs)) {
            long remainingMs = CooldownManager.getRemainingCooldown(commandName, userId);

            // Use Discord relative timestamp everywhere for consistency
            long retryEpochSeconds = (System.currentTimeMillis() + remainingMs) / 1000;
            String description = String.format(
                    "%s %s\nYou can use this command again <t:%d:R>.",
                    Emojis.DEFAULT_ERROR,
                    TITLE_COOLDOWN_ACTIVE,
                    retryEpochSeconds);

            return ValidationResult.cooldownFailure(TITLE_COOLDOWN_ACTIVE, description, remainingMs);
        }

        return ValidationResult.success();
    }

    /**
     * Converts a JDA {@link Permission} enum to a human-readable string.
     * <p>
     * This utility method formats the constant case permission names (e.g.,
     * {@code MANAGE_CHANNEL})
     * into a more readable format (e.g., "Manage Channel").
     *
     * @param permission The JDA permission enum.
     * @return A formatted, human-readable string for the permission.
     */
    private static String humanPermissionName(Permission permission) {
        // If the name looks like CONSTANT_CASE, pretty-print it; otherwise return as-is
        String raw = permission.getName();
        if (raw.isBlank())
            return "Unknown Permission";
        boolean looksConstant = raw.equals(raw.toUpperCase(Locale.ROOT));
        if (!looksConstant)
            return raw;

        return Arrays.stream(raw.replace("_", " ").toLowerCase(Locale.ROOT).split(" "))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    // ---------------------------
    // Error handling
    // ---------------------------

    /**
     * A public handler for responding to a failed validation for a message command.
     * <p>
     * This method is responsible for sending an ephemeral or auto-deleting error
     * message
     * to the user, based on the type of failure. It delegates to specific handler
     * methods
     * for different failure types.
     *
     * @param event  The {@link MessageReceivedEvent} that triggered the failure.
     * @param result The {@link ValidationResult} containing failure details.
     */
    public static void handleValidationFailure(MessageReceivedEvent event, ValidationResult result) {
        if (result.getFailureType() == ValidationFailureType.COOLDOWN_ACTIVE)
            sendCooldownEmbed(event, result);
        else
            sendStandardErrorEmbed(event, result);
    }

    /**
     * A public handler for responding to a failed validation for a slash command.
     * <p>
     * This method sends an ephemeral error message to the user via the JDA
     * or
     * {@link net.dv8tion.jda.api.interactions.InteractionHook#sendMessageEmbeds(java.util.Collection)}
     * based on whether the interaction has already been acknowledged.
     *
     * @param event  The {@link SlashCommandInteractionEvent} that triggered the
     *               failure.
     * @param result The {@link ValidationResult} containing failure details.
     */
    public static void handleValidationFailure(SlashCommandInteractionEvent event, ValidationResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(ColorCodes.ERROR_RED)
                .setTitle(Emojis.DEFAULT_ERROR + " " + result.getTitle())
                .setDescription(result.getDescription());

        if (result.getFailureType() == ValidationFailureType.COOLDOWN_ACTIVE) {
            long remainingMs = result.getCooldownRemainingMs();
            embed.setFooter("You can retry in " + (remainingMs / 1000) + "s");
        }

        if (event.isAcknowledged())
            event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        else
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    /**
     * Sends a standard, auto-deleting error embed for message commands.
     * <p>
     * This method sends a reply to the channel with the error details and queues it
     * to be
     * automatically deleted after a short period. It also includes an error handler
     * to
     * gracefully ignore if the message is deleted before the deletion request is
     * processed.
     *
     * @param event  The {@link MessageReceivedEvent} to reply to.
     * @param result The {@link ValidationResult} with the error details.
     */
    private static void sendStandardErrorEmbed(MessageReceivedEvent event, ValidationResult result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(ColorCodes.ERROR_RED)
                .setTitle(Emojis.DEFAULT_ERROR + " " + result.getTitle())
                .setDescription(result.getDescription());

        event.getChannel()
                .sendMessage(event.getAuthor().getAsMention())
                .setEmbeds(embed.build())
                .queue(
                        message -> message.delete().queueAfter(AUTO_DELETE_SECONDS, TimeUnit.SECONDS,
                                null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)),
                        throwable -> logger.warn("Failed to send error message in channel {}: {}",
                                event.getChannel().getId(), throwable.getMessage()));
    }

    /**
     * Sends a specialized, auto-deleting cooldown embed for message commands.
     * <p>
     * This method is similar to
     * {@link #sendStandardErrorEmbed(MessageReceivedEvent, ValidationResult)}
     * but the auto-deletion time is dynamically set to match the remaining cooldown
     * duration,
     * providing a clear visual cue to the user.
     *
     * @param event  The {@link MessageReceivedEvent} to reply to.
     * @param result The {@link ValidationResult} with the cooldown details.
     */
    private static void sendCooldownEmbed(MessageReceivedEvent event, ValidationResult result) {
        long remainingMs = result.getCooldownRemainingMs();

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(ColorCodes.ERROR_RED)
                .setDescription(result.getDescription())
                .setFooter("This message will auto-delete in " + (remainingMs / 1000) + "s");

        event.getChannel()
                .sendMessageEmbeds(embed.build())
                .queue(
                        message -> message.delete().queueAfter(remainingMs, TimeUnit.MILLISECONDS,
                                null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)),
                        throwable -> logger.warn("Failed to send cooldown message in channel {}: {}",
                                event.getChannel().getId(), throwable.getMessage()));
    }

    // ---------------------------
    // Result types
    // ---------------------------

    /**
     * A class representing the outcome of a command validation check.
     * <p>
     * This immutable object encapsulates whether the validation was a success or
     * failure,
     * the specific type of failure, and the corresponding user-facing message.
     */
    public static class ValidationResult {
        private final boolean success;
        private final ValidationFailureType failureType;
        private final String title;
        private final String description;
        private final long cooldownRemainingMs;

        private ValidationResult(boolean success,
                ValidationFailureType failureType,
                String title,
                String description,
                long cooldownRemainingMs) {
            this.success = success;
            this.failureType = failureType;
            this.title = title;
            this.description = description;
            this.cooldownRemainingMs = cooldownRemainingMs;
        }

        /**
         * Creates a new successful validation result.
         * 
         * @return A new {@code ValidationResult} indicating success.
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null, null, null, 0);
        }

        /**
         * Creates a new failure validation result.
         * 
         * @param type        The type of validation failure.
         * @param title       The title for the error message.
         * @param description The description for the error message.
         * @return A new {@code ValidationResult} indicating a generic failure.
         */
        public static ValidationResult failure(ValidationFailureType type, String title, String description) {
            return new ValidationResult(false, type, title, description, 0);
        }

        /**
         * Creates a new failure result specific to an active cooldown.
         * 
         * @param title       The title for the error message.
         * @param description The description for the error message.
         * @param remainingMs The remaining cooldown time in milliseconds.
         * @return A new {@code ValidationResult} indicating an active cooldown.
         */
        public static ValidationResult cooldownFailure(String title, String description, long remainingMs) {
            return new ValidationResult(false, ValidationFailureType.COOLDOWN_ACTIVE, title, description, remainingMs);
        }

        /**
         * Checks if the validation result represents a failure.
         * 
         * @return {@code true} if the command cannot be executed, {@code false}
         *         otherwise.
         */
        public boolean hasFailed() {
            return !success;
        }

        /**
         * Gets the type of failure, if any.
         * 
         * @return The {@link ValidationFailureType}, or {@code null} if successful.
         */
        public ValidationFailureType getFailureType() {
            return failureType;
        }

        /**
         * Gets the title for the error message.
         * 
         * @return The title string, or {@code null} if successful.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Gets the description for the error message.
         * 
         * @return The description string, or {@code null} if successful.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets the remaining cooldown time in milliseconds.
         * 
         * @return The remaining cooldown time, or {@code 0} if not a cooldown failure.
         */
        public long getCooldownRemainingMs() {
            return cooldownRemainingMs;
        }
    }

    /**
     * An enum representing the specific types of validation failures.
     */
    public enum ValidationFailureType {
        DEVELOPER_ONLY,
        INVALID_CONTEXT,
        INSUFFICIENT_USER_PERMISSIONS,
        INSUFFICIENT_BOT_PERMISSIONS,
        COOLDOWN_ACTIVE,
        NSFW_REQUIRED,
        EXECUTION_FAILURE
    }
}