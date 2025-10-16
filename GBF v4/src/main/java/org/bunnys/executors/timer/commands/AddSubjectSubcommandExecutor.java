package org.bunnys.executors.timer.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bunnys.database.models.timer.Subject;
import org.bunnys.database.services.GBFUserService;
import org.bunnys.database.services.TimerDataService;
import org.bunnys.executors.timer.TimerEventPublisher;
import org.bunnys.executors.timer.Timers;
import org.bunnys.executors.timer.engine.GradeEngine;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Executor for adding subjects to account or semester.
 * Handles validation, error cases, and user feedback.
 */
@Component
public class AddSubjectSubcommandExecutor {

    private final TimerDataService timerDataService;
    private final GBFUserService userService;
    private final TimerEventPublisher eventPublisher;

    public AddSubjectSubcommandExecutor(
            TimerDataService timerDataService,
            GBFUserService userService,
            TimerEventPublisher eventPublisher) {
        this.timerDataService = timerDataService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public void execute(
            SlashCommandInteractionEvent interaction,
            String type,
            String subjectName,
            String subjectCode,
            int credits,
            String grade) {

        String userId = interaction.getUser().getId();

        // Validate grade requirement for account type
        if ("A".equals(type) && grade == null) {
            sendErrorEmbed(interaction,
                    "Grade Required",
                    "You must specify a grade when adding a subject to your account.",
                    true);
            return;
        }

        // CRITICAL: Acknowledge interaction immediately to prevent 3-second timeout
        interaction.deferReply(true).queue(hook -> {
            // Process async after acknowledging
            CompletableFuture.supplyAsync(() -> addSubject(userId, type, subjectName, subjectCode, credits, grade))
                    .thenAccept(result -> {
                        if (result.isError()) {
                            sendErrorEmbedHook(hook, "Failed to Add Subject", result.message());
                        } else {
                            sendSuccessEmbedHook(hook, result.message(), type, subjectName, credits);
                        }
                    })
                    .exceptionally(throwable -> {
                        Logger.error("Unexpected error adding subject for user " + userId, throwable);
                        sendErrorEmbedHook(hook,
                                "Unexpected Error",
                                "An unexpected error occurred. Please try again later.");
                        return null;
                    });
        }, failure -> {
            Logger.error("Failed to defer reply for user " + userId, failure);
        });
    }

    private AddSubjectResult addSubject(
            String userId,
            String type,
            String subjectName,
            String subjectCode,
            int credits,
            String grade) {

        try {
            // Create Timers instance
            Timers timers = Timers.create(
                    userId,
                    timerDataService,
                    userService,
                    eventPublisher,
                    null,
                    false,
                    false);

            // Parse grade using the enum's fromString method instead of valueOf
            GradeEngine.Grade gradeEnum = null;
            if (grade != null) {
                // Convert Discord choice format to display format
                String gradeDisplay = grade.replace("_PLUS", "+").replace("_MINUS", "-");

                // Use the Grade.fromString() method which handles various formats
                gradeEnum = GradeEngine.Grade.fromString(gradeDisplay)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid grade: " + gradeDisplay));
            }

            // Build subject object
            Subject newSubject = new Subject();
            newSubject.setSubjectName(subjectName);
            newSubject.setSubjectCode(subjectCode);
            newSubject.setCreditHours(credits);
            newSubject.setTimesStudied(0);
            newSubject.setMarksLost(0);
            newSubject.setGrade(gradeEnum);

            // Add to appropriate location
            if ("S".equals(type)) {
                timers.addSubjectSemester(newSubject);
                return AddSubjectResult.success("Subject added to current semester successfully.");
            } else {
                timers.addSubjectAccount(newSubject);
                return AddSubjectResult.success("Subject added to account successfully.");
            }

        } catch (IllegalStateException e) {
            // Handle expected business logic errors
            return AddSubjectResult.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            return AddSubjectResult.error("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            Logger.error("Error adding subject for userId: " + userId, e);
            return AddSubjectResult.error("Failed to add subject. Please try again later.");
        }
    }

    private void sendSuccessEmbed(
            SlashCommandInteractionEvent interaction,
            String message,
            String type,
            String subjectName,
            int credits) {

        String location = "S".equals(type) ? "Current Semester" : "Account";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Subject Added")
                .setDescription(String.format(
                        "**%s** has been added to **%s**\n\n" +
                                "• Credits: %d\n" +
                                "• Best of luck with your studies! 📚",
                        subjectName,
                        location,
                        credits))
                .setColor(ColorCodes.DEFAULT)
                .setTimestamp(Instant.now());

        interaction.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void sendSuccessEmbedHook(
            net.dv8tion.jda.api.interactions.InteractionHook hook,
            String message,
            String type,
            String subjectName,
            int credits) {

        String location = "S".equals(type) ? "Current Semester" : "Account";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Subject Added")
                .setDescription(String.format(
                        "**%s** has been added to **%s**\n\n" +
                                "• Credits: %d\n" +
                                "• Best of luck with your studies! 📚",
                        subjectName,
                        location,
                        credits))
                .setColor(ColorCodes.DEFAULT)
                .setTimestamp(Instant.now());

        hook.editOriginalEmbeds(embed.build()).queue();
    }

    private void sendErrorEmbed(
            SlashCommandInteractionEvent interaction,
            String title,
            String message,
            boolean ephemeral) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + title)
                .setDescription(message)
                .setColor(ColorCodes.DEFAULT)
                .setTimestamp(Instant.now());

        interaction.replyEmbeds(embed.build()).setEphemeral(ephemeral).queue();
    }

    private void sendErrorEmbedHook(
            net.dv8tion.jda.api.interactions.InteractionHook hook,
            String title,
            String message) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + title)
                .setDescription(message)
                .setColor(ColorCodes.DEFAULT)
                .setTimestamp(Instant.now());

        hook.editOriginalEmbeds(embed.build()).queue();
    }

    // Result holder
    private record AddSubjectResult(String message, boolean isError) {
        public static AddSubjectResult success(String message) {
            return new AddSubjectResult(message, false);
        }

        public static AddSubjectResult error(String message) {
            return new AddSubjectResult(message, true);
        }
    }
}