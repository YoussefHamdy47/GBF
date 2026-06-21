package org.bunnys.handler.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bunnys.handler.BunnyHub;
import org.bunnys.utils.BunnyLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BunnySubcommand {
    private String name;
    private String description;

    // Local Subcommand Overrides
    private int cooldown = 0;
    private boolean developerOnly = false;
    private boolean nsfw = false;

    private final List<OptionData> options = new ArrayList<>();

    public BunnySubcommand setName(String name) {
        this.name = name;
        return this;
    }

    public BunnySubcommand setDescription(String description) {
        this.description = description;
        return this;
    }

    public BunnySubcommand setCooldown(int seconds) {
        this.cooldown = seconds;
        return this;
    }

    public BunnySubcommand setDeveloperOnly(boolean developerOnly) {
        this.developerOnly = developerOnly;
        return this;
    }

    public BunnySubcommand setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
        return this;
    }

    public BunnySubcommand addOption(OptionData option) {
        this.options.add(option);
        return this;
    }

    public String getName() {
        return name;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean isDeveloperOnly() {
        return developerOnly;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public SubcommandData buildData() {
        SubcommandData data = new SubcommandData(name, description);
        data.addOptions(options);
        return data;
    }

    /**
     * Override this method to provide a raw list of strings for Discord to show the user.
     * The InteractionListener will automatically filter this based on user input.
     */
    public List<String> autocomplete(BunnyHub client, CommandAutoCompleteInteractionEvent event) {
        return Collections.emptyList();
    }

    public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
        BunnyLog.error("CRITICAL: Subcommand '" + this.name + "' was triggered but has no execute() method implemented");

        String mentions = String.join(" ", client.getCommandRegistry().getDeveloperIds().stream()
                .map(id -> "<@" + id + ">").toList());

        event.reply("This subcommand is missing its execution logic. Please notify a developer: " + mentions)
                .setEphemeral(true).queue();
    }
}