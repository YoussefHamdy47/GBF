package org.bunnys.handler.commands.message;

import net.dv8tion.jda.api.Permission;

import java.util.*;

/**
 * An immutable configuration class for a JDA message command
 * <p>
 * This class uses the builder design pattern to provide a robust and extensible
 * way
 * to configure a message-based command It encapsulates all essential
 * properties,
 * including command metadata, permissions, and behavior flags
 * The class is final and its state cannot be modified after creation, ensuring
 * thread safety and predictable behavior
 * </p>
 *
 * @author Bunny
 * @see net.dv8tion.jda.api.events.message.MessageReceivedEvent
 */
@SuppressWarnings("unused")
public final class MessageCommandConfig {
    private final String commandName;
    private final String commandDescription;
    private final String category;
    private final String commandUsage;
    private final List<String> aliases;
    private final EnumSet<Permission> userPermissions;
    private final EnumSet<Permission> botPermissions;
    private final boolean devOnly;
    private final long cooldownMS;
    private final boolean NSFW;

    /**
     * Private constructor to enforce the use of the Builder
     *
     * @param builder The builder instance containing the configured command data
     */
    private MessageCommandConfig(Builder builder) {
        this.commandName = Objects.requireNonNull(builder.commandName, "Command name is required");
        this.commandDescription = builder.commandDescription;
        this.category = builder.category;
        this.commandUsage = builder.commandUsage;
        this.NSFW = builder.NSFW;
        this.aliases = List.copyOf(builder.aliases);
        this.userPermissions = EnumSet.copyOf(builder.userPermissions);
        this.botPermissions = EnumSet.copyOf(builder.botPermissions);
        this.devOnly = builder.devOnly;
        this.cooldownMS = builder.cooldownMS;
    }

    /**
     * Gets the primary name of the command
     *
     * @return The command name
     */
    public String commandName() {
        return this.commandName;
    }

    /**
     * Gets the description of the command, providing a brief overview of its
     * function
     *
     * @return The command description
     */
    public String description() {
        return this.commandDescription;
    }

    /**
     * Gets the category to which the command belongs
     *
     * @return The command category
     */
    public String category() {
        return this.category;
    }

    /**
     * Gets a usage example for the command, clarifying its syntax
     *
     * @return The command usage string
     */
    public String usage() {
        return this.commandUsage;
    }

    /**
     * Gets an unmodifiable list of aliases for the command
     *
     * @return An immutable list of aliases
     */
    public List<String> aliases() {
        return List.copyOf(this.aliases);
    }

    /**
     * Checks if the command is marked as Not Safe for Work (NSFW)
     *
     * @return {@code true} if the command is NSFW, otherwise {@code false}
     */
    public boolean NSFW() {
        return this.NSFW;
    }

    /**
     * Gets an immutable set of permissions a user must have to use the command
     *
     * @return An immutable set of required user permissions
     */
    public EnumSet<Permission> userPermissions() {
        return EnumSet.copyOf(this.userPermissions);
    }

    /**
     * Gets an immutable set of permissions the bot must have to execute the command
     *
     * @return An immutable set of required bot permissions
     */
    public EnumSet<Permission> botPermissions() {
        return EnumSet.copyOf(this.botPermissions);
    }

    /**
     * Checks if the command is restricted to bot developers only
     *
     * @return {@code true} if the command is developer-only, otherwise
     *         {@code false}
     */
    public boolean devOnly() {
        return this.devOnly;
    }

    /**
     * Gets the cooldown duration for the command in milliseconds
     *
     * @return The cooldown period in milliseconds
     */
    public long cooldown() {
        return this.cooldownMS;
    }

    // --------------------- Builder --------------------//

    /**
     * A static, final builder class for {@link MessageCommandConfig}
     * <p>
     * This class provides a fluent API for setting the command's attributes Each
     * method
     * returns the builder itself, enabling method chaining
     * It performs initial validation to ensure a valid command configuration
     * can be built
     * </p>
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        private String commandName;
        private String commandDescription = "No description provided";
        private String commandUsage = "No usage provided";
        private String category = "General";
        private List<String> aliases = List.of();
        private EnumSet<Permission> userPermissions = EnumSet.noneOf(Permission.class);
        private EnumSet<Permission> botPermissions = EnumSet.noneOf(Permission.class);
        private boolean devOnly = false;
        private long cooldownMS = 0; // No cooldown by default
        private boolean NSFW = false;

        /**
         * Sets the primary name of the command
         *
         * @param commandName The name to set Must not be null or blank
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the provided name is null or blank
         */
        public Builder name(String commandName) {
            if (commandName == null || commandName.isBlank())
                throw new IllegalArgumentException("Command name cannot be null or blank");
            this.commandName = commandName.trim();
            return this;
        }

        /**
         * Sets the description of the command
         *
         * @param commandDescription The description to set
         * @return The current {@code Builder} instance
         */
        public Builder description(String commandDescription) {
            this.commandDescription = commandDescription == null ? "No description provided" : commandDescription;
            return this;
        }

        /**
         * Sets the category for the command
         *
         * @param category The category to set
         * @return The current {@code Builder} instance
         */
        public Builder category(String category) {
            this.category = category == null ? "General" : category;
            return this;
        }

        /**
         * Sets the usage example for the command
         *
         * @param commandUsage The usage string to set
         * @return The current {@code Builder} instance
         */
        public Builder usage(String commandUsage) {
            this.commandUsage = commandUsage == null ? "No usage provided" : commandUsage;
            return this;
        }

        /**
         * Sets whether the command is NSFW
         *
         * @param NSFW A boolean indicating if the command is NSFW
         * @return The current {@code Builder} instance
         */
        public Builder NSFW(boolean NSFW) {
            this.NSFW = NSFW;
            return this;
        }

        /**
         * Sets the aliases for the command from a list
         *
         * @param aliases A list of alias strings
         * @return The current {@code Builder} instance
         */
        public Builder aliases(List<String> aliases) {
            if (aliases == null || aliases.isEmpty()) {
                this.aliases = List.of();
                return this;
            }
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String a : aliases) {
                if (a == null)
                    continue;
                String t = a.trim();
                if (t.isEmpty())
                    continue;
                seen.add(t);
            }
            this.aliases = List.copyOf(seen);
            return this;
        }

        /**
         * Sets the aliases for the command from a variable-length argument list
         *
         * @param aliases A varargs of alias strings
         * @return The current {@code Builder} instance
         */
        public Builder aliases(String... aliases) {
            if (aliases == null || aliases.length == 0)
                return aliases(List.of());
            return aliases(Arrays.asList(aliases));
        }

        /**
         * Sets the user permissions required to use the command
         *
         * @param permissions A varargs of {@link Permission}
         * @return The current {@code Builder} instance
         */
        public Builder userPermissions(Permission... permissions) {
            this.userPermissions = permissions == null ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(Arrays.asList(permissions));
            return this;
        }

        /**
         * Sets the bot permissions required to execute the command
         *
         * @param permissions A varargs of {@link Permission}
         * @return The current {@code Builder} instance
         */
        public Builder botPermissions(Permission... permissions) {
            this.botPermissions = permissions == null ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(Arrays.asList(permissions));
            return this;
        }

        /**
         * Sets whether the command is restricted to developers
         *
         * @param devOnly A boolean indicating if the command is developer-only
         * @return The current {@code Builder} instance
         */
        public Builder devOnly(boolean devOnly) {
            this.devOnly = devOnly;
            return this;
        }

        /**
         * Sets the cooldown for the command in seconds
         *
         * @param seconds The cooldown duration in seconds
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the cooldown is negative
         */
        public Builder cooldown(long seconds) {
            if (seconds < 0)
                throw new IllegalArgumentException("Cooldown cannot be negative");
            this.cooldownMS = seconds * 1000;
            return this;
        }

        /**
         * Constructs and returns a new {@link MessageCommandConfig} instance
         *
         * @return A new, immutable {@link MessageCommandConfig}
         * @throws IllegalStateException if the command name is not set
         */
        public MessageCommandConfig build() {
            if (this.commandName == null || this.commandName.isBlank())
                throw new IllegalStateException("Command name is required (call name(...))");

            String cnLower = this.commandName.toLowerCase(Locale.ROOT);
            List<String> filtered = this.aliases.stream()
                    .filter(a -> !a.equalsIgnoreCase(cnLower))
                    .toList();

            this.aliases = List.copyOf(filtered);
            return new MessageCommandConfig(this);
        }
    }

    @Override
    public String toString() {
        return "MessageCommandConfig{name='%s', aliases=%s, devOnly=%s}".formatted(
                this.commandName, this.aliases, this.devOnly);
    }
}