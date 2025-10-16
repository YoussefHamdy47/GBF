package org.bunnys.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.spi.SlashCommand;

public class Test extends SlashCommand {
    @Override
    protected void commandOptions(SlashCommandConfig.Builder options) {
        options.name("test")
                .description("Test command with subcommands")
                .addSubcommand(new SubcommandData("hello", "Say hello world"))
                .addSubcommand(new SubcommandData("bye", "Say goodbye world"));
    }

    @Override
    public void execute(BunnyNexus client, SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();

        if (sub == null) {
            event.reply("⚠ You must pick a subcommand!").setEphemeral(true).queue();
            return;
        }

        switch (sub) {
            case "hello" -> event.reply("👋 Hello, world!").queue();
            case "bye" -> event.reply("👋 Goodbye, world!").queue();
            default -> event.reply("❓ Unknown subcommand: `" + sub + "`").setEphemeral(true).queue();
        }
    }
}
