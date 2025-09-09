package org.bunnys.handler.commands.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.*;

/**
 * A final, immutable configuration class for a JDA slash command
 * <p>
 * This class uses the builder design pattern to ensure thread safety and
 * simplify
 * the construction of complex objects
 * It encapsulates all the necessary attributes
 * for defining a slash command, including its name, description, and a list of
 * options
 * The use of {@code List.copyOf()} ensures that the internal list of options is
 * immutable after construction
 * </p>
 * <p>
 * This class is designed to be used in conjunction with a command handler to
 * register slash commands with Discord
 * </p>
 *
 * @author Bunny
 * @see net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
 */
@SuppressWarnings("unused")
public final class SlashCommandConfig {
    private final String commandName;
    private final String commandDescription;
    private final String category;
    private final String commandUsage;
    private final boolean testOnly;
    private final boolean devOnly;
    private final boolean NSFW;
    private final long cooldownMS;
    private final EnumSet<Permission> userPermissions;
    private final EnumSet<Permission> botPermissions;
    private final List<OptionData> commandOptions;

    // Subcommands
    private final List<SubcommandData> subcommands;
    private final List<SubcommandGroupData> subcommandGroups;

    /**
     * Private constructor to enforce the use of the Builder
     *
     * @param builder The builder instance containing the configured command data
     * @throws NullPointerException if the command name or description is null
     */
    private SlashCommandConfig(Builder builder) {
        this.commandName = Objects.requireNonNull(builder.commandName, "Slash Command name is required.");
        this.commandDescription = Objects.requireNonNull(builder.commandDescription,
                "Slash Command description is required.");
        this.category = builder.category;
        this.commandUsage = builder.commandUsage;
        this.testOnly = builder.testOnly;
        this.devOnly = builder.devOnly;
        this.NSFW = builder.NSFW;
        this.cooldownMS = builder.cooldownMS;
        this.userPermissions = EnumSet.copyOf(builder.userPermissions);
        this.botPermissions = EnumSet.copyOf(builder.botPermissions);
        this.commandOptions = List.copyOf(builder.options);
        this.subcommands = List.copyOf(builder.subcommands);
        this.subcommandGroups = List.copyOf(builder.subcommandGroups);
    }

    /**
     * Gets the name of the slash command
     *
     * @return The command's name as a {@code String}
     */
    public String name() {
        return this.commandName;
    }

    /**
     * Gets the description of the slash command
     *
     * @return The command's description as a {@code String}
     */
    public String description() {
        return this.commandDescription;
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
     * Gets an immutable list of the command's options
     *
     * @return An unmodifiable {@code List<OptionData>} of the command's options
     */
    public List<OptionData> options() {
        return this.commandOptions;
    }

    public String category() {
        return this.category;
    }

    public String usage() {
        return this.commandUsage;
    }

    public EnumSet<Permission> userPermissions() {
        return EnumSet.copyOf(this.userPermissions);
    }

    public EnumSet<Permission> botPermissions() {
        return EnumSet.copyOf(this.botPermissions);
    }

    public boolean devOnly() {
        return this.devOnly;
    }

    public boolean NSFW() {
        return this.NSFW;
    }

    public long cooldown() {
        return this.cooldownMS;
    }

    public List<SubcommandData> subcommands() {
        return this.subcommands;
    }

    public List<SubcommandGroupData> subcommandGroups() {
        return this.subcommandGroups;
    }

    // --------------------- Builder --------------------//

    /**
     * A static, final builder class for {@link SlashCommandConfig}
     * <p>
     * This class provides a fluent API for setting the command's attributes
     * Each method
     * returns the builder itself, allowing for method chaining
     * It includes validation
     * checks to ensure that required fields are not null or blank
     * </p>
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        private String commandName;
        private String commandDescription;
        private boolean testOnly = false;
        private String category = "General";
        private String commandUsage = "No usage provided";
        private boolean devOnly = false;
        private boolean NSFW = false;
        private long cooldownMS = 0;
        private EnumSet<Permission> userPermissions = EnumSet.noneOf(Permission.class);
        private EnumSet<Permission> botPermissions = EnumSet.noneOf(Permission.class);
        private final List<OptionData> options = new ArrayList<>();
        private final List<SubcommandData> subcommands = new ArrayList<>();
        private final List<SubcommandGroupData> subcommandGroups = new ArrayList<>();

        /**
         * Sets the name of the slash command
         *
         * @param name The name to set. Must be non-null and non-blank
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the provided name is null or blank
         */
        public Builder name(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Slash Command name cannot be empty");
            this.commandName = name.trim().toLowerCase(Locale.US);
            return this;
        }

        /**
         * Sets the description of the slash command
         *
         * @param description The description to set. Must be non-null and non-blank
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the provided description is null or blank
         */
        public Builder description(String description) {
            if (description == null || description.isBlank())
                throw new IllegalArgumentException("Slash Command description cannot be empty");
            this.commandDescription = description;
            return this;
        }

        /**
         * Sets the testOnly boolean of the slash command
         *
         * @param testOnly The boolean value to set
         * @return The current {@code Builder} instance
         */
        public Builder testOnly(boolean testOnly) {
            this.testOnly = testOnly;
            return this;
        }

        /**
         * Adds a single option to the command
         *
         * @param option The {@link OptionData} to add. Must be non-null
         * @return The current {@code Builder} instance
         * @throws IllegalArgumentException if the provided option is null
         */
        public Builder addOption(OptionData option) {
            if (option == null)
                throw new IllegalArgumentException("Slash Command Option data cannot be null");
            this.options.add(option);
            return this;
        }

        public Builder category(String category) {
            this.category = category == null ? "General" : category;
            return this;
        }

        public Builder usage(String usage) {
            this.commandUsage = usage == null ? "/" + commandName : usage;
            return this;
        }

        public Builder userPermissions(Permission... permissions) {
            this.userPermissions = permissions == null
                    ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(Arrays.asList(permissions));
            return this;
        }

        public Builder botPermissions(Permission... permissions) {
            this.botPermissions = permissions == null
                    ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(Arrays.asList(permissions));
            return this;
        }

        public Builder devOnly(boolean devOnly) {
            this.devOnly = devOnly;
            return this;
        }

        public Builder NSFW(boolean NSFW) {
            this.NSFW = NSFW;
            return this;
        }

        public Builder cooldown(long seconds) {
            if (seconds < 0)
                throw new IllegalArgumentException("Cooldown cannot be negative");
            this.cooldownMS = seconds * 1000;
            return this;
        }

        /**
         * Adds a list of options to the command.
         *
         * @param optionList A {@code List} of {@link OptionData} to add. The list
         *                   itself can be null
         * @return The current {@code Builder} instance
         */
        public Builder addOptions(List<OptionData> optionList) {
            if (optionList != null)
                optionList.forEach(this::addOption);
            return this;
        }

        public Builder addSubcommand(SubcommandData subcommand) {
            if (subcommand == null)
                throw new IllegalArgumentException("Subcommand Data cannot be null");
            this.subcommands.add(subcommand);
            return this;
        }

        public Builder addSubcommands(List<SubcommandData> subcommandList) {
            if (subcommandList != null)
                subcommandList.forEach(this::addSubcommand);
            return this;
        }

        public Builder addSubcommandGroup(SubcommandGroupData group) {
            if (group == null)
                throw new IllegalArgumentException("Subcommand group cannot be null");
            this.subcommandGroups.add(group);
            return this;
        }

        public Builder addSubcommandGroups(List<SubcommandGroupData> groupList) {
            if (groupList != null)
                groupList.forEach(this::addSubcommandGroup);
            return this;
        }

        /**
         * Constructs and returns a new {@link SlashCommandConfig} object
         * <p>
         * This method performs the final validation and object creation
         * </p>
         *
         * @return A new, immutable {@link SlashCommandConfig} instance
         * @throws IllegalStateException if the command name or description has not been
         *                               set
         */
        public SlashCommandConfig build() {
            if (this.commandName == null || this.commandName.isBlank()) {
                throw new IllegalStateException("Command name must be set before building the config.");
            }
            if (this.commandDescription == null || this.commandDescription.isBlank()) {
                throw new IllegalStateException("Command description must be set before building the config.");
            }
            return new SlashCommandConfig(this);
        }
    }

    @Override
    public String toString() {
        return "SlashCommandConfig{name='%s', description='%s'}".formatted(this.commandName, this.commandDescription);
    }
}