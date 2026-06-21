package org.bunnys.handler.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.bunnys.handler.BunnyHub;
import org.bunnys.utils.BunnyLog;

import java.util.*;

public abstract class BunnyCommand {
    protected final BunnyHub client;

    private String name;
    private String description;

    // Feature Flags
    private boolean developerOnly = false;
    private boolean testOnly = false;
    private boolean nsfw = false;
    private boolean dmEnabled = true;
    private boolean developerBypass = false;
    private int cooldown = 0; // In Seconds

    private final List<OptionData> options = new ArrayList<>();
    private final List<Permission> userPermissions = new ArrayList<>();
    private final List<Permission> appPermissions = new ArrayList<>();
    private final Map<String, BunnySubcommand> subcommands = new HashMap<>();

    public BunnyCommand(BunnyHub client) {
        this.client = client;
    }

    // Setters
    public BunnyCommand setName(String name) {
        this.name = name;
        return this;
    }

    public BunnyCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    public BunnyCommand setDeveloperOnly(boolean developerOnly) {
        this.developerOnly = developerOnly;
        return this;
    }

    public BunnyCommand setTestOnly(boolean testOnly) {
        this.testOnly = testOnly;
        return this;
    }

    public BunnyCommand setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
        return this;
    }

    public BunnyCommand setDmEnabled(boolean dmEnabled) {
        this.dmEnabled = dmEnabled;
        return this;
    }

    public BunnyCommand setDeveloperBypass(boolean developerBypass) {
        this.developerBypass = developerBypass;
        return this;
    }

    public BunnyCommand setCooldown(int seconds) {
        this.cooldown = seconds;
        return this;
    }

    public BunnyCommand addOption(OptionData option) {
        this.options.add(option);
        return this;
    }

    public BunnyCommand addUserPermissions(Permission... permissions) {
        this.userPermissions.addAll(Arrays.asList(permissions));
        return this;
    }

    public BunnyCommand addAppPermissions(Permission... permissions) {
        this.appPermissions.addAll(Arrays.asList(permissions));
        return this;
    }

    public BunnyCommand addSubcommand(BunnySubcommand subcommand) {
        this.subcommands.put(subcommand.getName(), subcommand);
        return this;
    }

    // Getters
    public String getName() {
        return name;
    }

    public boolean isDeveloperOnly() {
        return developerOnly;
    }

    public boolean isTestOnly() {
        return testOnly;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public boolean isDmEnabled() {
        return dmEnabled;
    }

    public boolean isDeveloperBypass() {
        return developerBypass;
    }

    public int getCooldown() {
        return cooldown;
    }

    public List<Permission> getUserPermissions() {
        return userPermissions;
    }

    public List<Permission> getAppPermissions() {
        return appPermissions;
    }

    public Map<String, BunnySubcommand> getSubcommands() {
        return subcommands;
    }

    public SlashCommandData buildCommandData() {
        SlashCommandData data = Commands.slash(name, description);
        data.setGuildOnly(!dmEnabled);
        data.setNSFW(nsfw);
        data.addOptions(options);

        for (BunnySubcommand sub : subcommands.values()) {
            data.addSubcommands(sub.buildData());
        }

        if (!userPermissions.isEmpty()) {
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(userPermissions));
        }
        return data;
    }

    /**
     * Override this method to provide a raw list of strings for Discord to show the user.
     * The InteractionListener will automatically filter this based on user input and enforce Discord's 25-item limit.
     */
    public List<String> autocomplete(BunnyHub client, CommandAutoCompleteInteractionEvent event) {
        return Collections.emptyList();
    }

    public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
        BunnyLog.error("CRITICAL: Command '" + this.name + "' was triggered but has no execute() method implemented");

        String mentions = String.join(" ", client.getCommandRegistry().getDeveloperIds().stream()
                .map(id -> "<@" + id + ">").toList());

        event.reply("This command is missing its execution logic. Please notify a developer: " + mentions)
                .setEphemeral(true).queue();
    }
}