package org.bunnys.nexus.timers;

import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.bunnys.database.models.timers.Subject;
import org.bunnys.database.models.timers.TimerData;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.handler.database.DB;
import org.bunnys.nexus.timers.engine.LevelEngine;
import org.bunnys.nexus.timers.services.TimerAccountService;
import org.bunnys.nexus.timers.services.TimerSubjectService;
import org.bunnys.utils.AppDesign;
import org.bunnys.utils.Utils;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SuppressWarnings("unused")
public class Timers {

    public enum RecordDestination {
        ACCOUNT,
        SEMESTER
    }

    // Miami pink is too ugly idc enough to update and remove the old one, so just
    // use cyan for now
    private static final Color MIAMI_PINK = AppDesign.ColorCodes.CYAN;
    private static final Color MIAMI_CYAN = new Color(5, 217, 232);

    private final String userId;
    private final IReplyCallback interaction;

    private BunnyUser cachedUser = null;
    private TimerData cachedTimerData = null;
    private boolean isDataLoaded = false;

    public Timers(String userId, IReplyCallback interaction) {
        this.userId = userId;
        this.interaction = interaction;
    }

    private void loadData() {
        if (isDataLoaded)
            return;
        this.cachedUser = DB.findOne(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId));
        this.cachedTimerData = DB.findOne(TimerData.class, "TimerData", Filters.eq("account.userID", userId));
        this.isDataLoaded = true;
    }

    public void checkUser() {
        loadData();
        if (cachedUser == null || cachedTimerData == null)
            throw new IllegalStateException(
                    "You don't have a BunnyHub Timers account. Use `/timer register <semester>` first.");
    }

    public void checkSemester() {
        checkUser();
        if (cachedTimerData.getCurrentSemester() == null
                || cachedTimerData.getCurrentSemester().getSemesterName() == null)
            throw new IllegalStateException("You don't have an active semester. Register one to begin tracking time.");
    }

    public MessageEmbed register(String semesterName) {
        if (semesterName == null || semesterName.trim().isEmpty())
            throw new IllegalArgumentException(
                    "You must provide a semester name to register (e.g., `" + getCurrentSemester() + "`).");

        loadData();
        String cleanName = semesterName.trim();
        boolean isBrandNewAccount = false;

        if (cachedUser == null || cachedTimerData == null) {
            TimerAccountService.registerAccount(userId);
            isBrandNewAccount = true;
            this.isDataLoaded = false;
            loadData();
        }

        if (cachedTimerData.getCurrentSemester() != null
                && cachedTimerData.getCurrentSemester().getSemesterName() != null)
            throw new IllegalStateException(
                    "Semester `" + cachedTimerData.getCurrentSemester().getSemesterName() + "` is already active.\n" +
                            "Use `/timer end` to close it before starting a new one.");

        TimerAccountService.registerSemester(userId, cleanName);

        String userName = interaction.getUser().getEffectiveName();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(MIAMI_PINK);
        embed.setTitle("🌴 Welcome to BunnyHub");
        embed.setTimestamp(Instant.now());

        if (isBrandNewAccount) {
            embed.setDescription("Welcome aboard, **" + userName + "**.\n\nYour account has been registered and **"
                    + cleanName + "** is now active.");
        } else {
            embed.setDescription("Successfully registered **" + cleanName
                    + "**.\n\n> A new chapter has been added to your academic record.");
        }

        embed.setFooter("🌴 Semester file opened");
        return embed.build();
    }

    public MessageEmbed addSubject(RecordDestination destination, Subject subject) {
        String code = subject.getSubjectCode().toUpperCase();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(MIAMI_PINK);
        embed.setTimestamp(Instant.now());

        if (destination == RecordDestination.SEMESTER) {
            checkSemester();
            TimerSubjectService.addSubjectToSemester(userId, subject);

            embed.setTitle(AppDesign.Emojis.WHITE_HEART_SPIN + " Course Added");
            embed.setDescription("🍸 Module **" + code + "** — *" + subject.getSubjectName()
                    + "* has been added to the current semester.\n\n" +
                    "> " + AppDesign.Emojis.DIAMOND_SPIN + " Telemetry tracking is now live.");
            embed.setFooter("🌴 Semester record updated");

        } else if (destination == RecordDestination.ACCOUNT) {
            checkUser();
            TimerSubjectService.addSubjectToAccount(userId, subject);

            embed.setTitle("🎓 Course Registered");
            embed.setDescription("💎 Module **" + code + "** — *" + subject.getSubjectName()
                    + "* has been logged into your permanent academic record.\n\n");
            embed.setFooter("💎 Academic record updated");
        }
        return embed.build();
    }

    public MessageEmbed removeSubject(RecordDestination destination, String subjectCode) {
        String code = subjectCode.toUpperCase().trim();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(MIAMI_PINK);
        embed.setTimestamp(Instant.now());

        if (destination == RecordDestination.SEMESTER) {
            checkSemester();

            boolean existsInSemester = cachedTimerData.getCurrentSemester().getSemesterSubjects() != null &&
                    cachedTimerData.getCurrentSemester().getSemesterSubjects().stream()
                            .anyMatch(sub -> sub.getSubjectCode().equalsIgnoreCase(code));

            if (!existsInSemester)
                throw new IllegalArgumentException("`" + code
                        + "` is not registered in your active semester. If you are trying to delete it completely, please change the destination to **Academic Record**.");

            TimerSubjectService.removeSubjectFromSemester(userId, code);

            embed.setTitle("📚 Course Dropped");
            embed.setDescription("**" + code + "** has been removed from the active semester.\n\n" +
                    "> The course is no longer available for study tracking this term.");
            embed.setFooter("🌴 Semester record updated");

        } else if (destination == RecordDestination.ACCOUNT) {
            checkUser();

            boolean existsGlobally = cachedUser.getSubjects() != null &&
                    cachedUser.getSubjects().stream()
                            .anyMatch(sub -> sub.getSubjectCode().equalsIgnoreCase(code));

            if (!existsGlobally)
                throw new IllegalArgumentException("`" + code
                        + "` was not found in your Academic Record. If you are trying to drop it from your active term, please change the destination to **Semester**.");

            TimerSubjectService.removeSubjectFromAccount(userId, code);

            embed.setTitle("🎓 Course Removed");
            embed.setDescription("**" + code + "** has been removed from your academic record.\n\n");
            embed.setFooter("💎 Academic record updated");
        }

        return embed.build();
    }

    public MessageEmbed startSession(String messageId, String channelId, String guildId, String topic,
            String objective) {
        checkSemester();
        org.bunnys.database.models.timers.Session session = cachedTimerData.getSessionData();

        if (session.getSessionStartTime() != null)
            throw new IllegalStateException(
                    "An active session is already running. Please end it before starting a new one.");

        Instant now = Instant.now();
        if (cachedTimerData.getCurrentSemester().getSessionStartTimes() == null)
            cachedTimerData.getCurrentSemester().setSessionStartTimes(new ArrayList<>());

        cachedTimerData.getCurrentSemester().getSessionStartTimes().add(now.toEpochMilli());

        session.setSessionStartTime(Date.from(now));
        session.setLastSessionDate(Date.from(now));
        session.setSessionTopic(topic);
        session.setMessageID(messageId);
        session.setChannelID(channelId);
        session.setGuildID(guildId);
        session.setNumberOfBreaks(0);
        session.setSessionTime(0.0);

        if (session.getSubjectsStudied() == null) {
            session.setSubjectsStudied(new ArrayList<>());
        } else {
            session.getSubjectsStudied().clear();
        }

        if (session.getSessionBreaks() != null) {
            session.getSessionBreaks().setSessionBreakStart(null);
            session.getSessionBreaks().setSessionBreakTime(0.0);
        }

        String cleanTopic = topic.split("-")[0].trim().toUpperCase();

        if (cachedTimerData.getCurrentSemester().getSemesterSubjects() != null) {
            cachedTimerData.getCurrentSemester().getSemesterSubjects().stream()
                    .filter(s -> s.getSubjectCode().equalsIgnoreCase(cleanTopic))
                    .findFirst()
                    .ifPresent(subject -> {
                        session.getSubjectsStudied().add(subject.getSubjectCode());
                        subject.setTimesStudied(subject.getTimesStudied() + 1);
                    });
        }

        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), cachedTimerData);

        return buildActiveSessionEmbed(cleanTopic, objective, 0.0, 0.0, false);
    }

    public MessageEmbed buildPendingSessionEmbed(String topic) {
        checkSemester();

        String userName = interaction.getUser().getEffectiveName();
        String greetingText = TimerQuotes.getRandomGreeting(userName);
        String cleanTopic = topic.split("-")[0].trim().toUpperCase();

        return new EmbedBuilder()
                .setColor(MIAMI_CYAN)
                .setTitle("🌴 " + greetingText + " | " + topic)
                .setDescription("Telemetry link established for **" + cleanTopic
                        + "**.\n\n> ☕ Click the **Start** button below when you are physically ready to begin.")
                .setTimestamp(Instant.now())
                .build();
    }

    public MessageEmbed changeSubject(String newTopic) {
        checkSemester();
        org.bunnys.database.models.timers.Session session = cachedTimerData.getSessionData();

        if (session.getSessionStartTime() == null)
            throw new IllegalStateException("You don't have an active session to modify.");

        if (session.getSessionBreaks() != null && session.getSessionBreaks().getSessionBreakStart() != null)
            throw new IllegalStateException("The timer is paused. Please unpause before changing subjects.");

        if (session.getSessionTopic() != null && session.getSessionTopic().equalsIgnoreCase(newTopic))
            throw new IllegalStateException("Your telemetry feed is already routed to this module.");

        Instant now = Instant.now();
        long startMs = session.getSessionStartTime().getTime();
        double totalBreakSecs = session.getSessionBreaks() != null ? session.getSessionBreaks().getSessionBreakTime()
                : 0.0;
        double totalElapsedSecs = (now.toEpochMilli() - startMs) / 1000.0;
        double totalActiveSecs = Math.max(0, totalElapsedSecs - totalBreakSecs);

        double previouslyAllocatedSecs = session.getSessionTime();
        double timeToCredit = Math.max(0, totalActiveSecs - previouslyAllocatedSecs);

        String oldTopic = session.getSessionTopic();
        if (oldTopic != null && !oldTopic.trim().isEmpty()) {
            String oldCode = oldTopic.split("-")[0].trim().toUpperCase();
            if (cachedTimerData.getCurrentSemester().getSemesterSubjects() != null) {
                cachedTimerData.getCurrentSemester().getSemesterSubjects().stream()
                        .filter(s -> s.getSubjectCode().equalsIgnoreCase(oldCode))
                        .findFirst()
                        .ifPresent(subject -> {
                            double currentSubTime = subject.getTotalStudyTime() != null ? subject.getTotalStudyTime()
                                    : 0.0;
                            subject.setTotalStudyTime(currentSubTime + timeToCredit);
                        });
            }
        }

        session.setSessionTime(totalActiveSecs);
        session.setSessionTopic(newTopic);

        if (newTopic != null && !newTopic.trim().isEmpty()) {
            String newCode = newTopic.split("-")[0].trim().toUpperCase();

            if (!session.getSubjectsStudied().contains(newCode)) {
                session.getSubjectsStudied().add(newCode);

                if (cachedTimerData.getCurrentSemester().getSemesterSubjects() != null) {
                    cachedTimerData.getCurrentSemester().getSemesterSubjects().stream()
                            .filter(s -> s.getSubjectCode().equalsIgnoreCase(newCode))
                            .findFirst()
                            .ifPresent(subject -> subject.setTimesStudied(subject.getTimesStudied() + 1));
                }
            }
        }

        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), cachedTimerData);

        String cleanTopic = newTopic != null ? newTopic.split("-")[0].trim().toUpperCase() : "UNKNOWN";
        return new EmbedBuilder()
                .setColor(MIAMI_CYAN)
                .setTitle("🔄 Module Updated | " + cleanTopic)
                .setDescription("✦ **New Active Module:** `" + cleanTopic
                        + "`\n\n> *Telemetry feed successfully routed to the new subject. Your existing terminal will automatically update its data on its next refresh.*")
                .setTimestamp(Instant.now())
                .build();
    }

    public Modal buildEndSemesterModal() {
        checkSemester();
        String semName = cachedTimerData.getCurrentSemester().getSemesterName();
        String confirmationPhrase = "Archive " + semName;

        TextInput confirmInput = TextInput
                .create("confirmation_input", "Type '" + confirmationPhrase + "' to confirm", TextInputStyle.SHORT)
                .setPlaceholder(confirmationPhrase)
                .setRequired(true)
                .setMinLength(confirmationPhrase.length())
                .setMaxLength(confirmationPhrase.length() + 5)
                .build();

        String customId = "semester_end_modal:" + userId + ":" + System.currentTimeMillis();

        return Modal.create(customId, "End Current Semester")
                .addActionRow(confirmInput)
                .build();
    }

    /**
     * Processes the submitted modal, verifies the 5-minute rule, checks the phrase,
     * archives the semester using internal cached logic, and generates the final
     * recap embed.
     */
    public net.dv8tion.jda.api.entities.MessageEmbed processEndSemesterModal(
            net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event, long requestTime) {
        checkSemester();

        String semName = cachedTimerData.getCurrentSemester().getSemesterName();
        String expectedPhrase = "Archive " + semName;

        var mapping = event.getValue("confirmation_input");
        if (mapping == null)
            throw new IllegalArgumentException(
                    "Confirmation input was completely missing. The operation has been cancelled.");

        String userInput = mapping.getAsString().trim();

        if (System.currentTimeMillis() - requestTime > 300_000)
            throw new IllegalStateException(
                    "Confirmation timed out. You must confirm within 5 minutes. The operation has been cancelled.");

        if (!userInput.equalsIgnoreCase(expectedPhrase))
            throw new IllegalArgumentException("Invalid confirmation phrase. Expected `" + expectedPhrase
                    + "`. The operation has been cancelled.");

        String recapData = finalizeSemesterArchival(event);
        String userName = event.getUser().getEffectiveName();

        net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
        embed.setColor(org.bunnys.utils.AppDesign.ColorCodes.CYAN);
        embed.setTitle("💎 Semester Archived | " + semName);
        embed.setTimestamp(java.time.Instant.now());
        embed.setFooter("Telemetry finalized • " + userName, event.getUser().getEffectiveAvatarUrl());

        String sb = recapData
                + "\n> *Your telemetry for this semester has been successfully finalized. Your dedication is officially on the record.*";
        embed.setDescription(sb);

        return embed.build();
    }

    /**
     * Private worker method that handles the mathematical execution and database
     * updates for ending a semester, using the class's built-in memory caching.
     */
    private String finalizeSemesterArchival(net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event) {
        org.bunnys.database.models.timers.Semester currentSemester = cachedTimerData.getCurrentSemester();
        org.bunnys.database.models.timers.Semester longestSemester = cachedTimerData.getAccount().getLongestSemester();

        if (longestSemester == null || currentSemester.getSemesterTime() > longestSemester.getSemesterTime()) {
            cachedTimerData.getAccount().setLongestSemester(currentSemester);
            event.getJDA().getEventManager().handle(new org.bunnys.nexus.events.custom.RecordBrokenEvent(
                    event.getJDA(), event, org.bunnys.nexus.events.custom.RecordBrokenEvent.RecordType.SEMESTER, null,
                    currentSemester));
        }

        long convertedXP = org.bunnys.nexus.timers.engine.LevelEngine
                .convertSeasonLevel(currentSemester.getSemesterLevel()) + (long) currentSemester.getSemesterXP();
        long totalSemesterXP = org.bunnys.nexus.timers.engine.LevelEngine
                .calculateTotalSeasonXP(currentSemester.getSemesterLevel()) + (long) currentSemester.getSemesterXP();

        // Core Time Variables
        double totalSecs = currentSemester.getSemesterTime();
        double longestSecs = currentSemester.getLongestSession();
        double breakSecs = currentSemester.getTotalBreakTime();
        int sessionCount = currentSemester.getSessionStartTimes() != null
                ? currentSemester.getSessionStartTimes().size()
                : 0;
        double avgSecs = sessionCount > 0 ? totalSecs / sessionCount : 0.0;

        // String Conversions
        String totalTimeStr = totalSecs > 0 ? org.bunnys.utils.Utils.msToTime((long) (totalSecs * 1000)).orElse("0s")
                : "0s";
        String longestSessionStr = longestSecs > 0
                ? org.bunnys.utils.Utils.msToTime((long) (longestSecs * 1000)).orElse("0s")
                : "0s";
        String avgSessionStr = avgSecs > 0 ? org.bunnys.utils.Utils.msToTime((long) (avgSecs * 1000)).orElse("0s")
                : "0s";
        String totalBreakTimeStr = breakSecs > 0
                ? org.bunnys.utils.Utils.msToTime((long) (breakSecs * 1000)).orElse("0s")
                : "0s";

        // Calculate Top Subject (MVP)
        String topSubject = "None";
        double topSubjectSecs = 0.0;
        int subjectsTracked = 0;

        if (currentSemester.getSemesterSubjects() != null) {
            subjectsTracked = currentSemester.getSemesterSubjects().size();
            for (var sub : currentSemester.getSemesterSubjects()) {
                double time = sub.getTotalStudyTime() != null ? sub.getTotalStudyTime() : 0.0;
                if (time > topSubjectSecs) {
                    topSubjectSecs = time;
                    topSubject = sub.getSubjectName() + " - " + sub.getSubjectCode().toUpperCase();
                }
            }
        }
        String topSubjectTimeStr = topSubjectSecs > 0
                ? org.bunnys.utils.Utils.msToTime((long) (topSubjectSecs * 1000)).orElse("0s")
                : "0s";

        StringBuilder recap = new StringBuilder();
        recap.append("**Executive Summary**\n")
                .append("✦ **Total Focus Time:** `").append(totalTimeStr).append("` *[")
                .append(String.format("%.2f", totalSecs / 3600.0)).append(" hours]*\n")
                .append("✦ **Total Sessions:** `").append(sessionCount).append("`\n")
                .append("✦ **Average Session Time:** `").append(avgSessionStr).append("` *[")
                .append(String.format("%.2f", avgSecs / 3600.0)).append(" hours]*\n")
                .append("✦ **Longest Session:** `").append(longestSessionStr).append("` *[")
                .append(String.format("%.2f", longestSecs / 3600.0)).append(" hours]*\n\n")

                .append("**Academic Portfolio**\n")
                .append("✦ **Subjects Studied:** `").append(subjectsTracked).append("`\n");

        if (topSubjectSecs > 0) {
            recap.append("✦ **Top Subject:** **").append(topSubject).append("** — `").append(topSubjectTimeStr)
                    .append("` *[").append(String.format("%.2f", topSubjectSecs / 3600.0)).append(" hours]*\n");
        }

        recap.append("✦ **Total Break Time:** `").append(totalBreakTimeStr).append("` *[")
                .append(String.format("%.2f", breakSecs / 3600.0)).append(" hours]*\n\n")

                .append("**Progression & Experience**\n")
                .append("✦ **Final Semester Level:** `").append(currentSemester.getSemesterLevel()).append("`\n")
                .append("✦ **Total XP Earned:** `").append(String.format("%,d", totalSemesterXP)).append("`\n")
                .append("✦ **Career RP Earned:** `+").append(String.format("%,d RP", convertedXP)).append("`\n");

        cachedTimerData.getAccount()
                .setLifetimeTime(cachedTimerData.getAccount().getLifetimeTime() + currentSemester.getSemesterTime());
        org.bunnys.nexus.timers.engine.LevelEngine.RankResult rankCheck = org.bunnys.nexus.timers.engine.LevelEngine
                .checkRank(cachedUser.getRank(), cachedUser.getRp(), convertedXP);

        if (rankCheck.hasRankedUp()) {
            cachedUser.setRank(cachedUser.getRank() + rankCheck.addedLevels());
            cachedUser.setRp((int) rankCheck.remainingRP());
            recap.append("\n**👑 ADVANCEMENT**\n✦ You have been promoted to Account Rank `")
                    .append(cachedUser.getRank()).append("`.\n");

            event.getJDA().getEventManager().handle(new org.bunnys.nexus.events.custom.AccountLevelUpEvent(
                    event.getJDA(), event, rankCheck.addedLevels(), rankCheck.remainingRP(), cachedUser));
        } else {
            cachedUser.setRp(cachedUser.getRp() + (int) convertedXP);
        }

        // Wipe the active semester slot
        cachedTimerData.setCurrentSemester(new org.bunnys.database.models.timers.Semester());

        org.bunnys.handler.database.DB.save(org.bunnys.database.models.user.BunnyUser.class, "BunnyUsers",
                com.mongodb.client.model.Filters.eq("userID", userId), cachedUser);
        org.bunnys.handler.database.DB.save(org.bunnys.database.models.timers.TimerData.class, "TimerData",
                com.mongodb.client.model.Filters.eq("account.userID", userId), cachedTimerData);

        return recap.toString();
    }

    public MessageEmbed refreshMainEmbed(MessageEmbed currentEmbed) {
        checkSemester();
        org.bunnys.database.models.timers.Session session = cachedTimerData.getSessionData();
        if (session.getSessionStartTime() == null)
            return currentEmbed;

        long now = System.currentTimeMillis();
        long startMs = session.getSessionStartTime().getTime();

        double storedBreakSecs = session.getSessionBreaks() != null ? session.getSessionBreaks().getSessionBreakTime()
                : 0.0;
        double activeBreakSecs = (session.getSessionBreaks() != null
                && session.getSessionBreaks().getSessionBreakStart() != null)
                        ? (now - session.getSessionBreaks().getSessionBreakStart().getTime()) / 1000.0
                        : 0.0;

        double totalBreakSecs = storedBreakSecs + activeBreakSecs;
        double totalElapsedSecs = (now - startMs) / 1000.0;
        double activeStudySecs = Math.max(0, totalElapsedSecs - totalBreakSecs);

        String topic = session.getSessionTopic();
        String cleanTopic = topic != null ? topic.split("-")[0].trim().toUpperCase() : "UNKNOWN";

        return buildActiveSessionEmbed(cleanTopic, null, activeStudySecs * 1000, totalBreakSecs * 1000, true);
    }

    /**
     * Unified embed builder for both Start Session and Refresh Main Embed to keep
     * the logic DRY.
     */
    private MessageEmbed buildActiveSessionEmbed(String cleanTopic, String objective, double additionalStudyMs,
            double additionalBreakMs, boolean isRefresh) {
        TimerStats stats = new TimerStats(cachedTimerData, cachedUser);
        org.bunnys.database.models.timers.Session session = cachedTimerData.getSessionData();

        double semesterMs = stats.getSemesterTime() + additionalStudyMs;
        double semesterBreakMs = stats.getBreakTime() + additionalBreakMs;

        int timesStudied = 0;
        double subjectTimeMs = additionalStudyMs;

        if (cachedTimerData.getCurrentSemester().getSemesterSubjects() != null) {
            var subOpt = cachedTimerData.getCurrentSemester().getSemesterSubjects().stream()
                    .filter(s -> s.getSubjectCode().equalsIgnoreCase(cleanTopic))
                    .findFirst();

            if (subOpt.isPresent()) {
                timesStudied = subOpt.get().getTimesStudied();
                subjectTimeMs += (subOpt.get().getTotalStudyTime() != null ? subOpt.get().getTotalStudyTime() : 0.0)
                        * 1000;
            }
        }

        String userName = interaction.getUser().getEffectiveName();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(MIAMI_CYAN);
        eb.setTimestamp(Instant.now());

        if (!isRefresh) {
            eb.setFooter("🌴 Session timer is live", interaction.getUser().getEffectiveAvatarUrl());
        }

        String displayTopic = session.getSessionTopic() != null ? session.getSessionTopic() : cleanTopic;
        eb.setTitle("🌴 " + TimerQuotes.getRandomGreeting(userName) + " | " + displayTopic);

        if (objective != null && !objective.trim().isEmpty()) {
            eb.setDescription("> **Mission Objective:** " + objective);
        }

        String recordSb = "✦ Semester Focus: `" + formatMs(semesterMs) + "` *[" + formatHoursAsNum(semesterMs)
                + " hours]*\n" +
                "✦ Total Sessions: `" + stats.getSessionCount() + "`\n" +
                "✦ Average Session Time: `" + formatMs(stats.getAverageSessionTime()) + "`\n\u200B";
        eb.addField("🍸 Executive Study Record", recordSb, false);

        int currentBreaks = session.getNumberOfBreaks();
        String breakSb = "✦ Semester Break Time: `" + formatMs(semesterBreakMs) + "`\n" +
                "✦ Total Breaks: `" + (stats.getBreakCount() + currentBreaks) + "`\n" +
                "✦ Average Break Time: `" + formatMs(stats.getAverageBreakTime()) + "`\n\u200B";
        eb.addField("🏖️ Leisure & Recovery", breakSb, false);

        String subSb = "✦ Active Module: **" + cleanTopic + "**\n" +
                "✦ Study Instances: `" + timesStudied + "`\n" +
                "✦ Total Time Logged: `" + formatMs(subjectTimeMs) + "` *[" + formatHoursAsNum(subjectTimeMs)
                + " hours]*\n\u200B";
        eb.addField(AppDesign.Emojis.WHITE_HEART_SPIN + " Module Telemetry", subSb, false);

        return eb.build();
    }

    public MessageEmbed statDisplay() {
        checkSemester();

        TimerStats stats = new TimerStats(cachedTimerData, cachedUser);
        String userName = interaction.getUser().getEffectiveName();
        String avatarUrl = interaction.getUser().getEffectiveAvatarUrl();
        String semName = cachedTimerData.getCurrentSemester().getSemesterName();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(MIAMI_CYAN);
        embed.setTimestamp(Instant.now());
        embed.setFooter(userName + "'s BunnyTimer Data", avatarUrl);

        String greetingText = TimerQuotes.getRandomGreeting(userName);
        embed.setTitle("🌴 " + greetingText + " — " + semName);

        StringBuilder recordSb = new StringBuilder();
        double lifetimeMs = stats.getTotalStudyTime();
        recordSb.append("✦ Lifetime Focus: `").append(formatMs(lifetimeMs)).append("` *[")
                .append(formatHoursAsNum(lifetimeMs)).append(" hours]*\n");

        double semesterMs = stats.getSemesterTime();
        recordSb.append("✦ Semester Focus: `").append(formatMs(semesterMs)).append("` *[")
                .append(formatHoursAsNum(semesterMs)).append(" hours]*\n");

        recordSb.append("✦ Average Session Time: `").append(formatMs(stats.getAverageSessionTime())).append("`\n");
        recordSb.append("✦ Longest Session: `").append(formatMs(stats.getLongestSessionTime())).append("`\n");
        recordSb.append("✦ Total Sessions: `").append(stats.getSessionCount()).append("`\n\u200B");

        embed.addField("🍸 Executive Study Record", recordSb.toString(), false);

        String avgStartTime = "`Not Enough Data`";
        List<Long> startTimes = cachedTimerData.getCurrentSemester().getSessionStartTimes();
        if (startTimes != null && !startTimes.isEmpty()) {
            long sumMillisOfDay = 0;
            for (Long timestamp : startTimes) {
                ZonedDateTime zdt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC"));
                sumMillisOfDay += zdt.toLocalTime().toNanoOfDay() / 1_000_000;
            }
            long avgMillisOfDay = sumMillisOfDay / startTimes.size();
            long todayMidnightUTC = Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli();
            long discordTimestamp = (todayMidnightUTC + avgMillisOfDay) / 1000;
            avgStartTime = "<t:" + discordTimestamp + ":t>";
        }

        String streakSb = "✦ Active Streak: `" + stats.getCurrentStreak() + " Days` 🔥\n" +
                "✦ Longest Streak: `" + stats.getLongestStreak() + " Days` 🔥\n" +
                "✦ Average Start Time: " + avgStartTime + "\n\u200B";
        embed.addField("✦ Streaks & Habits", streakSb, false);

        String breakSb = "✦ Total Recovery Time: `" + formatMs(stats.getBreakTime()) + "`\n" +
                "✦ Break Count: `" + stats.getBreakCount() + "`\n" +
                "✦ Average Break Time: `" + formatMs(stats.getAverageBreakTime()) + "`\n" +
                "✦ Sustained Focus (Time Between Breaks): `" + formatMs(stats.getAverageTimeBetweenBreaks())
                + "`\n\u200B";
        embed.addField("🏖️ Leisure & Recovery", breakSb, false);

        var subjects = stats.getSubjectsInOrder();
        int totalInstances = subjects.stream().mapToInt(Subject::getTimesStudied).sum();

        StringBuilder portfolioHeader = new StringBuilder();
        portfolioHeader.append("✦ Total Subjects: `").append(subjects.size()).append("`\n")
                .append("✦ Total Subject Instances: `").append(totalInstances).append("`\n\n")
                .append(AppDesign.Emojis.WHITE_HEART_SPIN).append(" **Active Modules**\n");

        if (subjects.isEmpty()) {
            portfolioHeader.append("> *No courses registered this semester.*\n\u200B");
            embed.addField(AppDesign.Emojis.DIAMOND_SPIN + " Academic Portfolio", portfolioHeader.toString(), false);
        } else {
            subjects.sort((s1, s2) -> {
                double t1 = s1.getTotalStudyTime() != null ? s1.getTotalStudyTime() : 0.0;
                double t2 = s2.getTotalStudyTime() != null ? s2.getTotalStudyTime() : 0.0;
                return Double.compare(t2, t1);
            });

            StringBuilder currentField = new StringBuilder(portfolioHeader);
            int fieldIndex = 1;

            for (int i = 0; i < subjects.size(); i++) {
                var sub = subjects.get(i);
                int credits = sub.getCreditHours();
                String creditStr = credits > 0 ? " (`" + credits + "`)" : "";
                double subMs = (sub.getTotalStudyTime() != null ? sub.getTotalStudyTime() : 0.0) * 1000;
                double hours = subMs / 3_600_000.0;

                String entry;
                if (i < 5) {
                    String prefix = switch (i) {
                        case 0 -> "🥇";
                        case 1 -> "🥈";
                        case 2 -> "🥉";
                        default -> "•";
                    };

                    // Clean 2-line format for Top 5
                    entry = prefix + " **" + sub.getSubjectCode().toUpperCase() + "**" + creditStr + " — *"
                            + sub.getSubjectName() + "*\n" +
                            "  ↳ Instances: `" + sub.getTimesStudied() + "` | Accumulated: `" + formatMs(subMs) + "` *["
                            + String.format("%,.2f hours", hours) + "]*\n\n";
                } else {
                    if (i == 5)
                        currentField.append("**Additional Modules**\n");
                    entry = "• **" + sub.getSubjectCode().toUpperCase() + "**" + creditStr + " — *"
                            + sub.getSubjectName() + "* | `" + sub.getTimesStudied() + "` | `"
                            + String.format("%,.2f hours", hours) + "`\n";
                }

                if (currentField.length() + entry.length() > 950) {
                    embed.addField(fieldIndex == 1 ? AppDesign.Emojis.DIAMOND_SPIN + " Academic Portfolio"
                            : "Academic Portfolio (Cont.)", currentField.toString(), false);
                    currentField = new StringBuilder();
                    fieldIndex++;
                }
                currentField.append(entry);
            }

            double avgSubMs = semesterMs / subjects.size();
            currentField.append("\n✦ Average Study Time Per Subject: `").append(formatMs(avgSubMs)).append("`\n\u200B");

            embed.addField(fieldIndex == 1 ? AppDesign.Emojis.DIAMOND_SPIN + " Academic Portfolio"
                    : "Academic Portfolio (Cont.)", currentField.toString(), false);
        }

        StringBuilder progSb = new StringBuilder();
        int semLevel = stats.getSemesterLevel();
        long semXpReq = LevelEngine.xpRequired(semLevel + 1);

        progSb.append(LevelEngine.rankUpEmoji(semLevel)).append(" **Level ").append(semLevel).append("**\n")
                .append("✦ XP to next level: `").append(String.format("%,.0f", stats.getSemesterXP())).append(" / ")
                .append(String.format("%,d", semXpReq)).append("`\n")
                .append("  ").append(stats.generateProgressBar(stats.percentageToNextLevel())).append(" `[")
                .append(stats.percentageToNextLevel()).append("%]`\n")
                .append("✦ Est. time to level up: `").append(formatMs(stats.getMsToNextLevel())).append("`\n\n");

        int accLevel = stats.getAccountLevel();
        long accRpReq = LevelEngine.rpRequired(accLevel + 1);

        progSb.append(LevelEngine.rankUpEmoji(accLevel)).append(" **Rank ").append(accLevel).append("**\n")
                .append("✦ RP to next rank: `").append(String.format("%,d", stats.getAccountRP())).append(" / ")
                .append(String.format("%,d", accRpReq)).append("`\n")
                .append("  ").append(stats.generateProgressBar(stats.percentageToNextRank())).append(" `[")
                .append(stats.percentageToNextRank()).append("%]`\n")
                .append("✦ Est. time to rank up: `").append(formatMs(stats.getMsToNextRank())).append("`\n");

        embed.addField(AppDesign.Emojis.DIAMOND_SPIN + " Progression & Status", progSb.toString(), false);

        return embed.build();
    }

    public List<MessageEmbed> buildGPAMenu() {
        checkUser();

        List<Subject> accountSubjects = new ArrayList<>(
                cachedUser.getSubjects() != null ? cachedUser.getSubjects() : Collections.emptyList());
        List<Subject> semesterSubjects = new ArrayList<>();

        if (cachedTimerData.getCurrentSemester() != null
                && cachedTimerData.getCurrentSemester().getSemesterSubjects() != null)
            semesterSubjects.addAll(cachedTimerData.getCurrentSemester().getSemesterSubjects());

        accountSubjects.sort((s1, s2) -> {
            double gpa1 = s1.getGradeEnum() != null ? s1.getGradeEnum().getGpaValue() : -1.0;
            double gpa2 = s2.getGradeEnum() != null ? s2.getGradeEnum().getGpaValue() : -1.0;

            int gradeCompare = Double.compare(gpa2, gpa1);
            if (gradeCompare != 0)
                return gradeCompare;
            return Integer.compare(s2.getCreditHours(), s1.getCreditHours());
        });

        semesterSubjects.sort((s1, s2) -> Integer.compare(s2.getCreditHours(), s1.getCreditHours()));

        List<MessageEmbed> pages = new ArrayList<>();
        String userName = interaction.getUser().getEffectiveName();
        String avatarUrl = interaction.getUser().getEffectiveAvatarUrl();

        if (accountSubjects.isEmpty() && semesterSubjects.isEmpty()) {
            EmbedBuilder emptyEmbed = new EmbedBuilder()
                    .setColor(MIAMI_PINK)
                    .setAuthor("Academic Record — " + userName, null, avatarUrl)
                    .setDescription("> *No subjects found in your permanent record or current semester.*")
                    .setFooter("Page 1 of 1");

            pages.add(emptyEmbed.build());
            return pages;
        }

        int itemsPerPage = 5;
        double cumulativeGpa = cachedUser.calculateCumulativeGPA();

        if (!semesterSubjects.isEmpty()) {
            int semPages = (int) Math.ceil((double) semesterSubjects.size() / itemsPerPage);
            for (int i = 0; i < semPages; i++) {
                int start = i * itemsPerPage;
                int end = Math.min(start + itemsPerPage, semesterSubjects.size());
                List<Subject> chunk = semesterSubjects.subList(start, end);

                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(MIAMI_CYAN)
                        .setAuthor("Current Semester — " + userName, null, avatarUrl);

                StringBuilder sb = new StringBuilder();
                sb.append(String.format(AppDesign.Emojis.VERIFY + " **Cumulative GPA:** `%.3f`\n\n", cumulativeGpa));
                sb.append("**🌴 Active Courses (In Progress)**\n\n");

                for (Subject sub : chunk) {
                    sb.append("• **").append(sub.getSubjectCode().toUpperCase()).append("** — *")
                            .append(sub.getSubjectName()).append("*\n")
                            .append("  ↳ Grade: **In Progress** | Credits: `").append(sub.getCreditHours())
                            .append(" CH`\n\n");
                }

                embed.setDescription(sb.toString().trim());
                pages.add(embed.build());
            }
        }

        if (!accountSubjects.isEmpty()) {
            int accPages = (int) Math.ceil((double) accountSubjects.size() / itemsPerPage);
            for (int i = 0; i < accPages; i++) {
                int start = i * itemsPerPage;
                int end = Math.min(start + itemsPerPage, accountSubjects.size());
                List<Subject> chunk = accountSubjects.subList(start, end);

                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(MIAMI_PINK)
                        .setAuthor("Academic Record — " + userName, null, avatarUrl);

                StringBuilder sb = new StringBuilder();
                sb.append(String.format(AppDesign.Emojis.VERIFY + " **Cumulative GPA:** `%.3f`\n\n", cumulativeGpa));
                sb.append(AppDesign.Emojis.DIAMOND_SPIN + " **Completed Courses**\n\n");

                for (Subject sub : chunk) {
                    String gradeStr = sub.getGrade() != null ? sub.getGrade() : "N/A";
                    sb.append("• **").append(sub.getSubjectCode().toUpperCase()).append("** — *")
                            .append(sub.getSubjectName()).append("*\n")
                            .append("  ↳ Grade: **").append(gradeStr).append("** | Credits: `")
                            .append(sub.getCreditHours()).append("`\n\n");
                }

                embed.setDescription(sb.toString().trim());
                pages.add(embed.build());
            }
        }

        int totalPages = pages.size();
        for (int i = 0; i < totalPages; i++) {
            MessageEmbed original = pages.get(i);
            EmbedBuilder builder = new EmbedBuilder(original);
            builder.setFooter(String.format("Page %d of %d", i + 1, totalPages));
            pages.set(i, builder.build());
        }

        return pages;
    }

    private String formatMs(double ms) {
        return ms > 0 ? Utils.msToTime((long) ms).orElse("0s") : "0s";
    }

    private String formatHoursAsNum(double ms) {
        return ms > 0 ? String.format("%,.2f", ms / 3_600_000.0) : "0.00";
    }

    private static String getCurrentSemester() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        Month month = now.getMonth();

        if (month.getValue() <= 5)
            return "Spring " + year;
        else if (month.getValue() <= 8)
            return "Summer " + year;
        else
            return "Fall " + year;
    }
}