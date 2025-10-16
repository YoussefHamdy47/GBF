package org.bunnys.commands.timer;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.bunnys.database.services.GBFUserService;
import org.bunnys.database.services.TimerDataService;
import org.bunnys.executors.timer.TimerEventPublisher;
import org.bunnys.executors.timer.Timers;
import org.bunnys.executors.timer.commands.AddSubjectSubcommandExecutor;
import org.bunnys.executors.timer.commands.StatsSubcommandExecutor;
import org.bunnys.executors.timer.engine.GradeEngine;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.spi.SlashCommand;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@SuppressWarnings("unused")
public class TimersCommands extends SlashCommand {

        @Autowired
        private StatsSubcommandExecutor statsExecutor;

        @Autowired
        private AddSubjectSubcommandExecutor addSubjectSubcommandExecutor;

        @Autowired
        private TimerDataService timerDataService;

        @Autowired
        private GBFUserService gbfUserService;

        @Autowired
        private TimerEventPublisher eventPublisher;

        @Override
        protected void commandOptions(SlashCommandConfig.Builder options) {
                // Stats Subcommand
                SubcommandData statsSub = new SubcommandData("stats", "View your study stats")
                                .addOptions(
                                                new OptionData(OptionType.BOOLEAN, "ephemeral",
                                                                "Whether the message should be ephemeral",
                                                                false));

                // Add Subject Subcommand
                OptionData typeOption = new OptionData(OptionType.STRING, "type",
                                "The account type to add the subject to",
                                true)
                                .addChoice("Account", "A")
                                .addChoice("Semester", "S");

                OptionData subjectNameOption = new OptionData(OptionType.STRING, "subject-name",
                                "The name of the subject",
                                true);
                OptionData subjectCodeOption = new OptionData(OptionType.STRING, "subject-code", "The subject's code",
                                true);
                OptionData creditsOption = new OptionData(OptionType.INTEGER, "credits",
                                "Number of credits for the subject",
                                true)
                                .setMinValue(1)
                                .setMaxValue(4);

                OptionData gradeOption = new OptionData(OptionType.STRING, "grade", "Grade (only for account)", false)
                                .addChoice("A+", "A_PLUS")
                                .addChoice("A", "A")
                                .addChoice("A-", "A_MINUS")
                                .addChoice("B+", "B_PLUS")
                                .addChoice("B", "B")
                                .addChoice("B-", "B_MINUS")
                                .addChoice("C+", "C_PLUS")
                                .addChoice("C", "C")
                                .addChoice("C-", "C_MINUS")
                                .addChoice("D+", "D_PLUS")
                                .addChoice("D", "D")
                                .addChoice("F", "F")
                                .addChoice("W", "W")
                                .addChoice("P", "P");

                SubcommandData addSubjectSub = new SubcommandData("add-subject",
                                "Add a subject to account or current semester")
                                .addOptions(typeOption, subjectNameOption, subjectCodeOption, creditsOption,
                                                gradeOption);

                options.name("timer")
                                .description("Manage all GBF Timer features")
                                .addSubcommand(statsSub)
                                .addSubcommand(addSubjectSub);
        }

        @Override
        public void execute(BunnyNexus client, SlashCommandInteractionEvent interaction) {
                String subCommand = interaction.getSubcommandName();

                if (subCommand == null) {
                        interaction.reply("You must specify a subcommand").setEphemeral(true).queue();
                        return;
                }

                switch (subCommand) {
                        case "stats" -> {
                                boolean ephemeral = interaction.getOption("ephemeral") != null &&
                                                Objects.requireNonNull(interaction.getOption("ephemeral"))
                                                                .getAsBoolean();

                                if (statsExecutor == null) {
                                        interaction.reply(
                                                        "Timer Stats executor is not available, please contact a developer.")
                                                        .setEphemeral(true).queue();
                                        return;
                                }

                                statsExecutor.execute(interaction, ephemeral);
                        }

                        case "add-subject" -> {
                                String type = Objects.requireNonNull(interaction.getOption("type")).getAsString();
                                String subjectName = Objects.requireNonNull(interaction.getOption("subject-name"))
                                                .getAsString();
                                String subjectCode = Objects.requireNonNull(interaction.getOption("subject-code"))
                                                .getAsString();
                                long creditsLong = Objects.requireNonNull(interaction.getOption("credits")).getAsLong();

                                int credits = (int) creditsLong;

                                String grade = interaction.getOption("grade") != null
                                                ? Objects.requireNonNull(interaction.getOption("grade")).getAsString()
                                                : null;

                                addSubjectSubcommandExecutor.execute(interaction, type, subjectName, subjectCode,
                                                credits, grade);
                        }

                        default -> throw new IllegalArgumentException("Unknown subcommand");
                }
        }
}