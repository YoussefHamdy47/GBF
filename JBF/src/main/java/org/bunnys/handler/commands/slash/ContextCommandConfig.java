package org.bunnys.handler.commands.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.*;

/**
 * A final, immutable configuration class for Discord Context Menu Commands (User & Message)
 * <p>
 * This class uses the builder design pattern to ensure thread safety and
 * simplify the construction of complex objects. It encapsulates all the necessary
 * attributes for defining a context menu command, including its name, type, and permissions.
 * </p>
 * <p>
 * Context menu commands are invoked by right-clicking on users or messages and
 * do not support options, subcommands, or built-in descriptions like slash commands.
 * </p>
 *
 * @author Bunny
 * @see net.dv8tion.jda.api.interactions.commands.Command.Type
 */
@SuppressWarnings("unused")
public final class ContextCommandConfig {
    private final String commandName;
    private final Command.Type commandType;
    private final boolean testOnly;
    private final boolean devOnly;
    private final boolean NSFW;
    private final long cooldownMS;
    private final EnumSet<Permission> userPermissions;
    private final EnumSet<Permission> botPermissions;

    /**
     * Private constructor to enforce the use of the Builder
     *
     * @param builder The builder instance containing the configured command data
     * @throws NullPointerException if the command name or type is null
     */
    private ContextCommandConfig(Builder builder) {
        this.commandName = Objects.requireNonNull(builder.commandName, "Context command name is required.");
        this.commandType = Objects.requireNonNull(builder.commandType, "Context command type is required.");
        this.testOnly = builder.testOnly;
        this.devOnly = builder.devOnly;
        this.NSFW = builder.NSFW;
        this.cooldownMS = builder.cooldownMS;
        this.userPermissions = EnumSet.copyOf(builder.userPermissions);
        this.botPermissions = EnumSet.copyOf(builder.botPermissions);
    }

    /**
     * Gets the name of the context menu command
     *
     * @return The command's name as a {@code String}
     */
    public String name() {
        return this.commandName;
    }

    /**
     * Gets the type of the context menu command (USER or MESSAGE)
     *
     * @return The command's type as a {@code Command.Type}
     */
    public Command.Type type() {
        return this.commandType;
    }

    /**
     * Whether the command will be registered in test servers or not
     *
     * @return testOnly as a {@code boolean}
     */
    public boolean testOnly() {
        return this.testOnly;
    }

    /**
     * Whether the command is restricted to developers only
     *
     * @return devOnly as a {@code boolean}
     */
    public boolean devOnly() {
        return this.devOnly;
    }

    /**
     * Whether the command is restricted to NSFW channels
     *
     * @return NSFW as a {@code boolean}
     */
    public boolean NSFW() {
        return this.NSFW;
    }

    /**
     * Gets the cooldown duration in milliseconds
     *
     * @return cooldown duration as a {@code long}
     */
    public long cooldown() {
        return this.cooldownMS;
    }

    /**
     * Gets an immutable copy of the required user permissions
     *
     * @return An {@code EnumSet<Permission>} of required user permissions
     */
    public EnumSet<Permission> userPermissions() {
        return EnumSet.copyOf(this.userPermissions);
    }

    /**
     * Gets an immutable copy of the required bot permissions
     *
     * @return An {@code EnumSet<Permission>} of required bot permissions
     */
    public EnumSet<Permission> botPermissions() {
        return EnumSet.copyOf(this.botPermissions);
    }

    // --------------------- Builder --------------------//

    /**
     * A static, final builder class for {@link ContextCommandConfig}
     * <p>
     * This class provides a fluent API for setting the command's attributes.
     * Each method returns the builder itself, allowing for method chaining.
     * It includes validation checks to ensure that required fields are not null or blank.
     * </p>
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        private String commandName;
        private Command.Type commandType;
        private boolean testOnly = false;
        private boolean devOnly = false;
        private boolean NSFW = false;
        private long cooldownMS = 0;
        private EnumSet<Permission> userPermissions = EnumSet.noneOf(Permission.class);
        private EnumSet<Permission> botPermissions = EnumSet.noneOf(Permission.class);

        /**
         * Sets the name of the context menu command
         *
         * @param name The name to set. Must be non-null and non-blank
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the provided name is null or blank
         */
        public Builder name(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Context command name cannot be empty");
            this.commandName = name.trim();
            return this;
        }

        /**
         * Sets the type of the context menu command
         *
         * @param type The command type (USER or MESSAGE). Must be non-null
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the provided type is null or SLASH_COMMAND
         */
        public Builder type(Command.Type type) {
            if (type == null)
                throw new IllegalArgumentException("Context command type cannot be null");
            if (type == Command.Type.SLASH)
                throw new IllegalArgumentException("Context commands cannot be of type SlashCommand");
            this.commandType = type;
            return this;
        }

        /**
         * Sets the testOnly boolean of the context menu command
         *
         * @param testOnly The boolean value to set
         * @return The current {@code Builder} instance
         */
        public Builder testOnly(boolean testOnly) {
            this.testOnly = testOnly;
            return this;
        }

        /**
         * Sets the devOnly boolean of the context menu command
         *
         * @param devOnly The boolean value to set
         * @return The current {@code Builder} instance
         */
        public Builder devOnly(boolean devOnly) {
            this.devOnly = devOnly;
            return this;
        }

        /**
         * Sets the NSFW boolean of the context menu command
         *
         * @param NSFW The boolean value to set
         * @return The current {@code Builder} instance
         */
        public Builder NSFW(boolean NSFW) {
            this.NSFW = NSFW;
            return this;
        }

        /**
         * Sets the cooldown duration in seconds for the context menu command
         * Note: Discord does not provide built-in cooldown support for context commands,
         * this must be implemented in your command handler
         *
         * @param seconds The cooldown duration in seconds. Must not be negative
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if seconds is negative
         */
        public Builder cooldown(long seconds) {
            if (seconds < 0)
                throw new IllegalArgumentException("Cooldown cannot be negative");
            this.cooldownMS = seconds * 1000;
            return this;
        }

        /**
         * Sets the required user permissions for the context menu command
         *
         * @param permissions The permissions required by the user to execute this command
         * @return The current {@code Builder} instance
         */
        public Builder userPermissions(Permission... permissions) {
            this.userPermissions = permissions == null
                    ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(Arrays.asList(permissions));
            return this;
        }

        /**
         * Sets the required bot permissions for the context menu command
         *
         * @param permissions The permissions required by the bot to execute this command
         * @return The current {@code Builder} instance
         */
        public Builder botPermissions(Permission... permissions) {
            this.botPermissions = permissions == null
                    ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(Arrays.asList(permissions));
            return this;
        }

        /**
         * Constructs and returns a new {@link ContextCommandConfig} object
         * <p>
         * This method performs the final validation and object creation
         * </p>
         *
         * @return A new, immutable {@link ContextCommandConfig} instance
         * @throws IllegalStateException if the command name or type has not been set
         */
        public ContextCommandConfig build() {
            if (this.commandName == null || this.commandName.isBlank()) {
                throw new IllegalStateException("Command name must be set before building the config.");
            }
            if (this.commandType == null) {
                throw new IllegalStateException("Command type must be set before building the config.");
            }
            return new ContextCommandConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ContextCommandConfig{name='%s', type=%s}".formatted(this.commandName, this.commandType);
    }
}