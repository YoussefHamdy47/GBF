package org.bunnys.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.Command;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.commands.slash.SlashCommandConfig;

import java.util.List;
import java.util.stream.Collectors;

public class ColorCommand extends SlashCommand {

    private static final List<String> COLORS = List.of(
            "Red", "Green", "Blue", "Yellow", "Purple", "Black", "White"
    );

    @Override
    protected void commandOptions(SlashCommandConfig.Builder options) {
        options.name("color")
                .description("Pick a color")
                .addOption(
                        new OptionData(OptionType.STRING, "name", "Color name", true)
                                .setAutoComplete(true) // 🔑 enables autocomplete
                );

    }

    @Override
    public void execute(BunnyNexus client, SlashCommandInteractionEvent event) {
        String chosen = event.getOption("name").getAsString();
        event.reply("🎨 You picked **" + chosen + "**").queue();
    }

    @Override
    public void onAutoComplete(BunnyNexus client, CommandAutoCompleteInteractionEvent event) {
        String input = event.getFocusedOption().getValue();

        List<Command.Choice> choices = COLORS.stream()
                .filter(c -> c.toLowerCase().startsWith(input.toLowerCase()))
                .limit(25)
                .map(c -> new Command.Choice(c, c))
                .collect(Collectors.toList());

            System.out.println(choices);

        event.replyChoices(choices).queue();
    }
}
