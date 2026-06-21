package org.bunnys.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bunnys.database.models.timers.Subject;
import org.bunnys.database.models.timers.TimerData;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.handler.BunnyHub;
import org.bunnys.handler.commands.BunnyCommand;
import org.bunnys.handler.commands.BunnySubcommand;
import org.bunnys.nexus.timers.buttons.GPAPaginator;
import org.bunnys.nexus.timers.Timers;
import org.bunnys.nexus.timers.buttons.SessionMenuManager;
import org.bunnys.nexus.timers.services.PendingSessionManager;
import org.bunnys.utils.AppDesign;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class Timer extends BunnyCommand {

    public Timer(BunnyHub client) {
        super(client);
        setName("timer");
        setDescription("Track study sessions, courses, and academic progress.");
        setDmEnabled(true);
        setNsfw(false);
        setTestOnly(false);
        setCooldown(3);

        // View Stats Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("stats");
                setDescription("View your complete study statistics, streaks, and progress.");
                addOption(new OptionData(OptionType.BOOLEAN, "ephemeral", "Hide this from others? (Default: False)", false));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                OptionMapping ephemeralOpt = event.getOption("ephemeral");
                boolean isEphemeral = ephemeralOpt != null && ephemeralOpt.getAsBoolean();

                try {
                    Timers timerSystem = new Timers(userId, event);
                    MessageEmbed responseEmbed = timerSystem.statDisplay();
                    event.replyEmbeds(responseEmbed).setEphemeral(isEphemeral).queue();

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }
        });

        // Register Semester Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("register");
                setDescription("Create a new semester and start tracking your study time.");
                addOption(new OptionData(OptionType.STRING, "semester", "The name of the semester", true));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                String semesterName = Objects.requireNonNull(event.getOption("semester")).getAsString();

                try {
                    Timers timerSystem = new Timers(userId, event);
                    MessageEmbed responseEmbed = timerSystem.register(semesterName);
                    event.replyEmbeds(responseEmbed).queue();

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }
        });

        // View GPA Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("gpa");
                setDescription("View your academic record and cumulative GPA.");
                addOption(new OptionData(OptionType.BOOLEAN, "ephemeral", "Hide this from others? (Default: True)", false));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                OptionMapping ephemeralOpt = event.getOption("ephemeral");
                boolean isEphemeral = ephemeralOpt == null || ephemeralOpt.getAsBoolean();

                try {
                    Timers timerSystem = new Timers(userId, event);
                    List<MessageEmbed> pages = timerSystem.buildGPAMenu();

                    if (pages.isEmpty()) {
                        event.reply("> " + AppDesign.Emojis.ERROR + " **No GPA pages could be generated.**")
                                .setEphemeral(true).queue();
                        return;
                    }

                    String sessionId = GPAPaginator.createSession(userId, pages);

                    event.replyEmbeds(pages.getFirst())
                            .addActionRow(GPAPaginator.buildButtons(sessionId, 0, pages.size()))
                            .setEphemeral(isEphemeral)
                            .queue(
                                    hook -> GPAPaginator.attachHook(sessionId, hook),
                                    failure -> event.getHook()
                                            .sendMessage("> " + AppDesign.Emojis.ERROR + " **Failed to open GPA menu.**")
                                            .setEphemeral(true).queue()
                            );

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }
        });

        // Add Subject Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("add-subject");
                setDescription("Add a course to your semester or academic record.");

                addOption(new OptionData(OptionType.STRING, "destination", "Where should this course be added?", true)
                        .addChoice("Current Semester", "SEMESTER")
                        .addChoice("Academic Record", "ACCOUNT"));

                addOption(new OptionData(OptionType.STRING, "code", "Course code (e.g., ECES201)", true));
                addOption(new OptionData(OptionType.STRING, "name", "Course name (e.g., Digital and Analog Electronics)", true));
                addOption(new OptionData(OptionType.INTEGER, "credits", "Credit hours.", true));

                addOption(new OptionData(OptionType.STRING, "grade", "Final letter grade. Only used for Academic Record.", false)
                        .addChoice("A+", "A+")
                        .addChoice("A", "A")
                        .addChoice("A-", "A-")
                        .addChoice("B+", "B+")
                        .addChoice("B", "B")
                        .addChoice("B-", "B-")
                        .addChoice("C+", "C+")
                        .addChoice("C", "C")
                        .addChoice("C-", "C-")
                        .addChoice("D+", "D+")
                        .addChoice("D", "D")
                        .addChoice("F", "F"));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                String destStr = Objects.requireNonNull(event.getOption("destination")).getAsString();
                String code = Objects.requireNonNull(event.getOption("code")).getAsString();
                String name = Objects.requireNonNull(event.getOption("name")).getAsString();
                int credits = Objects.requireNonNull(event.getOption("credits")).getAsInt();

                OptionMapping gradeOpt = event.getOption("grade");
                String grade = gradeOpt != null ? gradeOpt.getAsString() : null;

                Timers.RecordDestination destination = Timers.RecordDestination.valueOf(destStr);

                Subject subject = new Subject();
                subject.setSubjectCode(code);
                subject.setSubjectName(name);
                subject.setCreditHours(credits);

                if (destination == Timers.RecordDestination.ACCOUNT && grade != null)
                    subject.setGrade(grade);

                try {
                    Timers timerSystem = new Timers(userId, event);
                    MessageEmbed responseEmbed = timerSystem.addSubject(destination, subject);
                    event.replyEmbeds(responseEmbed).queue();

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }
        });

        // Remove Subject Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("remove-subject");
                setDescription("Remove a course from your semester or academic record.");

                addOption(new OptionData(OptionType.STRING, "destination", "Where should this course be removed from?", true)
                        .addChoice("Current Semester", "SEMESTER")
                        .addChoice("Academic Record", "ACCOUNT"));
                addOption(new OptionData(OptionType.STRING, "code", "Course code (e.g., ECES201)", true)
                        .setAutoComplete(true));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                String destStr = Objects.requireNonNull(event.getOption("destination")).getAsString();
                String rawCode = Objects.requireNonNull(event.getOption("code")).getAsString();

                String code = rawCode.split("-")[0].trim();
                Timers.RecordDestination destination = Timers.RecordDestination.valueOf(destStr);

                try {
                    Timers timerSystem = new Timers(userId, event);
                    MessageEmbed responseEmbed = timerSystem.removeSubject(destination, code);
                    event.replyEmbeds(responseEmbed).queue();

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }

            @Override
            public java.util.List<String> autocomplete(org.bunnys.handler.BunnyHub client, net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent event) {
                if (event.getFocusedOption().getName().equals("code")) {
                    String userId = event.getUser().getId();

                    // 1. Read the CURRENT value of the 'destination' option
                    net.dv8tion.jda.api.interactions.commands.OptionMapping destMapping = event.getOption("destination");
                    String destination = destMapping != null ? destMapping.getAsString() : "Semester";

                    try {
                        java.util.List<String> choices = new java.util.ArrayList<>();

                        if (destination.equalsIgnoreCase("Academic Record")) {
                            // Fetch Global History
                            org.bunnys.database.models.user.BunnyUser user = org.bunnys.handler.database.DB.findOne(
                                    org.bunnys.database.models.user.BunnyUser.class,
                                    "BunnyUsers",
                                    com.mongodb.client.model.Filters.eq("userID", userId)
                            );

                            if (user != null && user.getSubjects() != null) {
                                choices = user.getSubjects().stream()
                                        .map(sub -> sub.getSubjectCode().toUpperCase() + " - " + sub.getSubjectName())
                                        .collect(java.util.stream.Collectors.toList());
                            }
                        } else {
                            // Fetch Active Semester
                            org.bunnys.database.models.timers.TimerData timer = org.bunnys.handler.database.DB.findOne(
                                    org.bunnys.database.models.timers.TimerData.class,
                                    "TimerData",
                                    com.mongodb.client.model.Filters.eq("account.userID", userId)
                            );

                            if (timer != null && timer.getCurrentSemester() != null && timer.getCurrentSemester().getSemesterSubjects() != null) {
                                choices = timer.getCurrentSemester().getSemesterSubjects().stream()
                                        .map(sub -> sub.getSubjectCode().toUpperCase() + " - " + sub.getSubjectName())
                                        .collect(java.util.stream.Collectors.toList());
                            }
                        }

                        if (choices.isEmpty()) {
                            return java.util.List.of("No subjects found for " + destination + ".");
                        }

                        // Return the raw list; your InteractionListener will handle the 25-item limit and filtering!
                        return choices;

                    } catch (Exception e) {
                        org.bunnys.utils.BunnyLog.error("Remove-Subject Autocomplete failed: " + e.getMessage());
                        return java.util.Collections.emptyList();
                    }
                }
                return java.util.Collections.emptyList();
            }
        });

        // Start Study Session Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("start");
                setDescription("Start a study session for a course.");

                addOption(new OptionData(OptionType.STRING, "module", "The course to study.", true)
                        .setAutoComplete(true));

                addOption(new OptionData(OptionType.STRING, "objective", "Optional objective for this session.", false));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                String moduleSelection = Objects.requireNonNull(event.getOption("module")).getAsString();

                OptionMapping objectiveOpt = event.getOption("objective");
                String objective = objectiveOpt != null ? objectiveOpt.getAsString() : null;

                if (moduleSelection.contains("No subjects found")) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** You must add a course before starting a study session.")
                            .setEphemeral(true).queue();
                    return;
                }

                String channelId = event.getChannel().getId();
                String guildId = event.isFromGuild() ? Objects.requireNonNull(event.getGuild()).getId() : "DM";

                try {
                    Timers timerSystem = new Timers(userId, event);
                    MessageEmbed pendingEmbed = timerSystem.buildPendingSessionEmbed(moduleSelection);

                    event.replyEmbeds(pendingEmbed)
                            .addActionRow(SessionMenuManager.buildButtons(userId, SessionMenuManager.SessionState.PENDING))
                            .queue(hook -> PendingSessionManager.createPendingSession(userId, moduleSelection, objective, channelId, guildId, hook));

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }

            @Override
            public java.util.List<String> autocomplete(org.bunnys.handler.BunnyHub client, net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent event) {
                if (event.getFocusedOption().getName().equals("module")) {
                    String userId = event.getUser().getId();

                    try {
                        org.bunnys.database.models.timers.TimerData timer = org.bunnys.handler.database.DB.findOne(
                                org.bunnys.database.models.timers.TimerData.class,
                                "TimerData",
                                com.mongodb.client.model.Filters.eq("account.userID", userId)
                        );

                        if (timer == null || timer.getCurrentSemester() == null || timer.getCurrentSemester().getSemesterSubjects() == null || timer.getCurrentSemester().getSemesterSubjects().isEmpty())
                            return java.util.List.of("No active modules found. Please add a course to your semester.");

                        return timer.getCurrentSemester().getSemesterSubjects().stream()
                                .map(sub -> sub.getSubjectCode().toUpperCase() + " - " + sub.getSubjectName())
                                .collect(java.util.stream.Collectors.toList());

                    } catch (Exception e) {
                        org.bunnys.utils.BunnyLog.error("Autocomplete fetch failed: " + e.getMessage());
                        return java.util.Collections.emptyList();
                    }
                }
                return java.util.Collections.emptyList();
            }
        });

        // Switch Subject Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("switch");
                setDescription("Seamlessly switch your active study module without stopping the timer.");
                addOption(new OptionData(OptionType.STRING, "module", "The new course to study.", true)
                        .setAutoComplete(true));
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();
                String moduleSelection = Objects.requireNonNull(event.getOption("module")).getAsString();

                if (moduleSelection.contains("No subjects found")) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** You must add a course first.")
                            .setEphemeral(true).queue();
                    return;
                }

                try {
                    Timers timerSystem = new Timers(userId, event);
                    MessageEmbed responseEmbed = timerSystem.changeSubject(moduleSelection);
                    event.replyEmbeds(responseEmbed).queue();

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }
            @Override
            public java.util.List<String> autocomplete(org.bunnys.handler.BunnyHub client, net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent event) {
                if (event.getFocusedOption().getName().equals("module")) {
                    String userId = event.getUser().getId();

                    try {
                        org.bunnys.database.models.timers.TimerData timer = org.bunnys.handler.database.DB.findOne(
                                org.bunnys.database.models.timers.TimerData.class,
                                "TimerData",
                                com.mongodb.client.model.Filters.eq("account.userID", userId)
                        );

                        if (timer == null || timer.getCurrentSemester() == null || timer.getCurrentSemester().getSemesterSubjects() == null || timer.getCurrentSemester().getSemesterSubjects().isEmpty())
                            return java.util.List.of("No active modules found. Please add a course to your semester.");

                        return timer.getCurrentSemester().getSemesterSubjects().stream()
                                .map(sub -> sub.getSubjectCode().toUpperCase() + " - " + sub.getSubjectName())
                                .collect(java.util.stream.Collectors.toList());

                    } catch (Exception e) {
                        org.bunnys.utils.BunnyLog.error("Autocomplete fetch failed: " + e.getMessage());
                        return java.util.Collections.emptyList();
                    }
                }
                return java.util.Collections.emptyList();
            }
        });

        // End Semester Subcommand
        addSubcommand(new BunnySubcommand() {
            {
                setName("end_semester");
                setDescription("Permanently conclude and archive your current academic semester.");
            }

            @Override
            public void execute(BunnyHub client, SlashCommandInteractionEvent event) {
                String userId = event.getUser().getId();

                try {
                    Timers timerSystem = new Timers(userId, event);
                    timerSystem.checkSemester();

                    // Miami Aesthetic Warning Embed
                    EmbedBuilder warningEmbed = new EmbedBuilder()
                            .setColor(AppDesign.ColorCodes.ERROR_RED)
                            .setTitle("🛑 Semester Termination Protocol")
                            .setDescription("""
                                    You are about to permanently conclude your current semester.
                                    
                                    ✦ **This action will archive your active modules.**
                                    ✦ **Your telemetry data will be locked and finalized.**
                                    
                                    > *Are you absolutely sure you want to proceed?*""");

                    net.dv8tion.jda.api.interactions.components.buttons.Button confirmBtn =
                            net.dv8tion.jda.api.interactions.components.buttons.Button.danger("semester_end_btn:confirm:" + userId, "🛑 Terminate Semester");

                    net.dv8tion.jda.api.interactions.components.buttons.Button cancelBtn =
                            net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("semester_end_btn:cancel:" + userId, "Cancel");

                    event.replyEmbeds(warningEmbed.build()).addActionRow(confirmBtn, cancelBtn).queue(hook -> {

                        // --- AUTO TIMEOUT LOGIC ---
                        // Wait exactly 60 seconds without blocking the thread
                        hook.retrieveOriginal().queueAfter(60, java.util.concurrent.TimeUnit.SECONDS, msg -> {

                            // Check if the title is still the warning (meaning the user hasn't clicked Cancel)
                            if (!msg.getEmbeds().isEmpty() && "🛑 Semester Termination Protocol".equals(msg.getEmbeds().getFirst().getTitle())) {
                                msg.editMessageComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(
                                        confirmBtn.asDisabled(), cancelBtn.asDisabled()
                                )).queue(null, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE));
                            }
                        }, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE));
                    });

                } catch (IllegalStateException | IllegalArgumentException e) {
                    event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                            .setEphemeral(true).queue();
                }
            }
        });
    }

    /**
     * Helper method to dynamically generate a deduplicated list of active subjects.
     * Utilizes a LinkedHashSet to guarantee O(1) deduplication while maintaining insertion order.
     */
    @NotNull
    private static List<String> getStrings(BunnyUser user, TimerData timer) {
        Set<String> subjectChoices = new LinkedHashSet<>();

        if (user != null && user.getSubjects() != null) {
            for (Subject sub : user.getSubjects()) {
                subjectChoices.add(sub.getSubjectCode().toUpperCase() + " - " + sub.getSubjectName());
            }
        }

        if (timer != null && timer.getCurrentSemester() != null && timer.getCurrentSemester().getSemesterSubjects() != null) {
            for (Subject sub : timer.getCurrentSemester().getSemesterSubjects()) {
                subjectChoices.add(sub.getSubjectCode().toUpperCase() + " - " + sub.getSubjectName());
            }
        }

        return new ArrayList<>(subjectChoices);
    }
}