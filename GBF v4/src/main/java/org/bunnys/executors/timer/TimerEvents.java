package org.bunnys.executors.timer;

import org.bunnys.database.models.timer.*;
import org.bunnys.database.models.users.GBFUser;
import org.bunnys.database.services.TimerDataService;
import org.bunnys.database.services.GBFUserService;
import org.bunnys.executors.timer.engine.LevelEngine;
import org.bunnys.executors.timer.engine.TimerHelper.*;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.bunnys.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A component class responsible for handling all core timer-related events and
 * business logic. Optimized to work with the cached LevelEngine for improved
 * performance in XP/RP calculations and level progression.
 * </p>
 *
 * <p>
 * This class is designed to be a singleton Spring component, leveraging
 * dependency injection for its required services. All methods are designed
 * to be robust and handle various edge cases gracefully.
 * </p>
 */
@Component
@SuppressWarnings("unused")
public class TimerEvents {

    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);

    private final TimerDataService timerDataService;
    private final GBFUserService userService;
    private final TimerEventPublisher eventPublisher;

    @Autowired
    public TimerEvents(TimerDataService timerDataService,
            GBFUserService userService,
            TimerEventPublisher eventPublisher) {
        this.timerDataService = timerDataService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public record TimerContext(TimerData timerData, GBFUser userData, String userId, String guildId, String channelId,
            String messageId) {
    }

    public record TimerEventResult<T>(boolean success, String message, T data, TimerEventsReturns eventType) {

        public static <T> TimerEventResult<T> success(T data, TimerEventsReturns eventType) {
            String message = (eventType != null) ? eventType.getMessage() : null;
            return new TimerEventResult<>(true, message, data, eventType);
        }

        public static <T> TimerEventResult<T> failure(String message) {
            return new TimerEventResult<>(false, message, null, null);
        }

        public static <T> TimerEventResult<T> failure(TimerEventsReturns eventType) {
            return new TimerEventResult<>(false, eventType.getMessage(), null, eventType);
        }
    }

    public TimerEventResult<TimerData> handleStart(TimerContext context) {
        try {
            TimerData timerData = context.timerData();

            if (timerData.getSessionData().getSessionStartTime() != null) {
                eventPublisher.publishButtonStateChange(context, createEnabledButtons(
                        TimerButtonID.Info, TimerButtonID.Pause, TimerButtonID.Stop), false);
                return TimerEventResult.failure(TimerEventsReturns.TimerAlreadyRunning);
            }

            Date currentTime = new Date();
            Session sessionData = timerData.getSessionData();
            Semester currentSemester = timerData.getCurrentSemester();

            currentSemester.addSessionTime(currentTime.getTime());

            sessionData.setSessionStartTime(currentTime);
            sessionData.setLastSessionDate(currentTime);
            sessionData.setSessionBreaks(new SessionBreak());
            sessionData.setNumberOfBreaks(0);
            sessionData.setSessionTime(0.0);
            sessionData.setGuildID(context.guildId());
            sessionData.setChannelID(context.channelId());
            sessionData.setMessageID(context.messageId());

            handleSubjectTracking(sessionData, currentSemester);

            TimerData savedData = timerDataService.save(timerData);
            return TimerEventResult.success(savedData, TimerEventsReturns.TimerStarted);

        } catch (Exception e) {
            Logger.error("Error starting timer for user: " + context.userId(), e);
            return TimerEventResult.failure("Failed to start timer: " + e.getMessage());
        }
    }

    public TimerEventResult<String> handleTimerInfo(TimerContext context) {
        try {
            TimerData timerData = context.timerData();
            Session sessionData = timerData.getSessionData();

            if (sessionData.getSessionStartTime() == null)
                return TimerEventResult.failure("No active session found");

            StringBuilder stats = new StringBuilder();

            SessionBreak sessionBreak = sessionData.getSessionBreaks();
            if (sessionBreak != null && sessionBreak.getSessionBreakStart() != null) {
                long activeBreakTime = System.currentTimeMillis() -
                        sessionBreak.getSessionBreakStart().getTime();
                stats.append("• Active Break Time: ")
                        .append(Utils.formatDuration(activeBreakTime))
                        .append("\n\n");
            }

            long sessionStartMs = sessionData.getSessionStartTime().getTime();
            long breakTimeMs = sessionBreak != null ? (long) (sessionBreak.getSessionBreakTime() * 1000) : 0;
            long timeElapsed = (System.currentTimeMillis() - sessionStartMs - breakTimeMs) / 1000;

            stats.append("• Time Elapsed: ").append(Utils.formatDuration(timeElapsed * 1000)).append("\n")
                    .append("• Break Time: ").append(Utils.formatDuration(breakTimeMs)).append("\n")
                    .append("• Number of Breaks: ").append(sessionData.getNumberOfBreaks()).append("\n\n")
                    .append("• Start Time: ").append(Utils.formatTimestamp(sessionData.getSessionStartTime()));

            return TimerEventResult.success(stats.toString(), null);

        } catch (Exception e) {
            Logger.error("Error getting timer info for user: " + context.userId(), e);
            return TimerEventResult.failure("Failed to get timer info: " + e.getMessage());
        }
    }

    public TimerEventResult<TimerData> handlePause(TimerContext context) {
        try {
            TimerData timerData = context.timerData();
            Session sessionData = timerData.getSessionData();

            if (sessionData.getSessionStartTime() == null) {
                eventPublisher.publishButtonStateChange(context, Collections.emptyList(), false);
                return TimerEventResult.failure(TimerEventsReturns.TimerNotStarted);
            }

            SessionBreak sessionBreak = sessionData.getSessionBreaks();
            if (sessionBreak != null && sessionBreak.getSessionBreakStart() != null) {
                eventPublisher.publishButtonStateChange(context,
                        createEnabledButtons(TimerButtonID.Unpause, TimerButtonID.Info), true);
                return TimerEventResult.failure(TimerEventsReturns.TimerAlreadyPaused);
            }

            if (sessionBreak == null) {
                sessionBreak = new SessionBreak();
                sessionData.setSessionBreaks(sessionBreak);
            }

            sessionBreak.setSessionBreakStart(new Date());
            sessionData.setNumberOfBreaks(sessionData.getNumberOfBreaks() + 1);
            timerData.getCurrentSemester().setBreakCount(
                    timerData.getCurrentSemester().getBreakCount() + 1);

            TimerData savedData = timerDataService.save(timerData);

            eventPublisher.publishButtonStateChange(context,
                    createEnabledButtons(TimerButtonID.Unpause, TimerButtonID.Info), true);

            return TimerEventResult.success(savedData, null);

        } catch (Exception e) {
            Logger.error("Error pausing timer for user: " + context.userId(), e);
            return TimerEventResult.failure("Failed to pause timer: " + e.getMessage());
        }
    }

    public TimerEventResult<Long> handleUnpause(TimerContext context) {
        try {
            TimerData timerData = context.timerData();
            Session sessionData = timerData.getSessionData();

            if (sessionData.getSessionStartTime() == null) {
                eventPublisher.publishButtonStateChange(context, Collections.emptyList(), false);
                return TimerEventResult.failure(TimerEventsReturns.TimerNotStarted);
            }

            SessionBreak sessionBreak = sessionData.getSessionBreaks();
            if (sessionBreak == null || sessionBreak.getSessionBreakStart() == null) {
                eventPublisher.publishButtonStateChange(context,
                        createEnabledButtons(TimerButtonID.Pause, TimerButtonID.Info, TimerButtonID.Stop), false);
                return TimerEventResult.failure(TimerEventsReturns.TimerNotPaused);
            }

            long breakDuration = System.currentTimeMillis() -
                    sessionBreak.getSessionBreakStart().getTime();
            long breakDurationSeconds = breakDuration / 1000;

            sessionBreak.setSessionBreakTime(
                    sessionBreak.getSessionBreakTime() + breakDurationSeconds);
            sessionBreak.setSessionBreakStart(null);

            TimerData savedData = timerDataService.save(timerData);

            eventPublisher.publishButtonStateChange(context,
                    createEnabledButtons(TimerButtonID.Pause, TimerButtonID.Info, TimerButtonID.Stop), false);

            return TimerEventResult.success(breakDurationSeconds, null);

        } catch (Exception e) {
            Logger.error("Error unpausing timer for user: " + context.userId(), e);
            return TimerEventResult.failure("Failed to unpause timer: " + e.getMessage());
        }
    }

    /**
     * Optimized stop handler that leverages LevelEngine's new Result records
     * and caching system for improved performance.
     */
    public TimerEventResult<String> handleStop(TimerContext context) {
        try {
            TimerData timerData = context.timerData();
            GBFUser userData = context.userData();
            Session sessionData = timerData.getSessionData();

            if (sessionData.getSessionStartTime() == null) {
                eventPublisher.publishButtonStateChange(context, Collections.emptyList(), false);
                return TimerEventResult.failure(TimerEventsReturns.TimerNotStarted);
            }

            SessionBreak sessionBreak = sessionData.getSessionBreaks();
            if (sessionBreak != null && sessionBreak.getSessionBreakStart() != null) {
                eventPublisher.publishButtonStateChange(context,
                        createEnabledButtons(TimerButtonID.Unpause, TimerButtonID.Info), true);
                return TimerEventResult.failure(TimerEventsReturns.CannotStopPaused);
            }

            // Calculate session metrics
            SessionMetrics metrics = calculateSessionMetrics(sessionData);
            String endMessage = buildEndMessage(sessionData, metrics);

            // Update semester and account data
            updateSemesterData(timerData.getCurrentSemester(), metrics);
            updateAccountData(timerData.getAccount(), metrics);

            // Optimized XP calculation using LevelEngine
            int xpEarned = LevelEngine.calculateXP((int) (metrics.timeElapsed / 60));
            endMessage += "\n• XP & RP Earned: " + String.format("%,d", xpEarned);

            // Handle progression using new Result records
            handleLevelingOptimized(context, userData, timerData, xpEarned);

            updateStreaks(timerData.getCurrentSemester());
            checkForRecords(context, timerData.getCurrentSemester(), metrics);
            resetSessionData(sessionData);

            // Batch save operations
            timerDataService.save(timerData);
            userService.saveUser(userData);

            eventPublisher.publishButtonStateChange(context, Collections.emptyList(), false);

            return TimerEventResult.success(endMessage, null);

        } catch (Exception e) {
            Logger.error("Error stopping timer for user: " + context.userId(), e);
            return TimerEventResult.failure("Failed to stop timer: " + e.getMessage());
        }
    }

    // Helper classes and methods

    private record SessionMetrics(long timeElapsed, long totalTime, long breakTimeMs, double averageBreakTime) {
    }

    private SessionMetrics calculateSessionMetrics(Session sessionData) {
        long sessionStartMs = sessionData.getSessionStartTime().getTime();
        SessionBreak sessionBreak = sessionData.getSessionBreaks();
        long breakTimeMs = sessionBreak != null ? (long) (sessionBreak.getSessionBreakTime() * 1000) : 0;

        long timeElapsed = Math.max(0,
                (System.currentTimeMillis() - sessionStartMs - breakTimeMs) / 1000);
        long totalTime = timeElapsed + (breakTimeMs / 1000);

        double averageBreakTime = (breakTimeMs > 0 && sessionData.getNumberOfBreaks() > 0)
                ? (double) breakTimeMs / sessionData.getNumberOfBreaks()
                : 0;

        return new SessionMetrics(timeElapsed, totalTime, breakTimeMs, averageBreakTime);
    }

    private String buildEndMessage(Session sessionData, SessionMetrics metrics) {
        StringBuilder message = new StringBuilder();

        message.append("• Start Time: ").append(Utils.formatTimestamp(sessionData.getSessionStartTime()))
                .append("\n• Time Elapsed: ").append(Utils.formatDuration(metrics.totalTime * 1000))
                .append("\n• Session Time: ").append(Utils.formatDuration(metrics.timeElapsed * 1000))
                .append("\n\n• Average Break Time: ");

        if (metrics.averageBreakTime > 0) {
            message.append(Utils.formatDuration((long) metrics.averageBreakTime));
        } else {
            message.append("No Breaks Taken");
        }

        message.append("\n• Total Break Time: ");
        if (metrics.breakTimeMs > 0)
            message.append(Utils.formatDuration(metrics.breakTimeMs));
        else
            message.append("No Breaks Taken");

        message.append("\n• Number of Breaks: ").append(sessionData.getNumberOfBreaks());

        List<String> subjectsStudied = sessionData.getSubjectsStudied();
        if (!subjectsStudied.isEmpty())
            message.append("\n• Studied Subjects: ").append(String.join(", ", subjectsStudied));
        else
            message.append("\n• No subjects studied this session.");

        return message.toString();
    }

    private void updateSemesterData(Semester semester, SessionMetrics metrics) {
        semester.setTotalBreakTime(semester.getTotalBreakTime() + metrics.breakTimeMs / 1000.0);
        semester.setSemesterTime(semester.getSemesterTime() + metrics.timeElapsed);

        if (metrics.timeElapsed > semester.getLongestSession())
            semester.setLongestSession(metrics.timeElapsed);
    }

    private void updateAccountData(Account account, SessionMetrics metrics) {
        account.setLifetimeTime(account.getLifetimeTime() + metrics.timeElapsed);
    }

    /**
     * Optimized leveling handler using LevelEngine's new Result records
     * for cleaner code and better performance.
     */
    private void handleLevelingOptimized(TimerContext context, GBFUser userData, TimerData timerData, int xpEarned) {
        // Handle rank progression with new Result record
        LevelEngine.RankResult rankResult = LevelEngine.checkRank(
                userData.getRank(), userData.getRP(), xpEarned);

        if (rankResult.hasRankedUp()) {
            userData.setRank(userData.getRank() + rankResult.addedRanks());
            userData.setRP(rankResult.remainingRP());
            eventPublisher.publishRankUp(context, rankResult.addedRanks(), rankResult.remainingRP());
        } else {
            userData.setRP(userData.getRP() + xpEarned);
        }

        // Handle semester level progression with new Result record
        Semester semester = timerData.getCurrentSemester();
        LevelEngine.LevelResult levelResult = LevelEngine.checkLevel(
                semester.getSemesterLevel(), semester.getSemesterXP(), xpEarned);

        if (levelResult.hasLeveledUp()) {
            semester.setSemesterLevel(semester.getSemesterLevel() + levelResult.addedLevels());
            semester.setSemesterXP(levelResult.remainingXP());
            eventPublisher.publishLevelUp(context, levelResult.addedLevels(), levelResult.remainingXP());
        } else {
            semester.setSemesterXP(semester.getSemesterXP() + xpEarned);
        }
    }

    private void updateStreaks(Semester semester) {
        Date now = new Date();
        Date lastUpdate = semester.getLastStreakUpdate();

        if (lastUpdate == null || (now.getTime() - lastUpdate.getTime()) >= ONE_DAY_MS) {
            semester.setStreak(semester.getStreak() + 1);
            semester.setLastStreakUpdate(now);

            if (semester.getStreak() > semester.getLongestStreak())
                semester.setLongestStreak(semester.getStreak());
        }
    }

    private void checkForRecords(TimerContext context, Semester semester, SessionMetrics metrics) {
        if (metrics.timeElapsed > semester.getLongestSession()) {
            eventPublisher.publishRecordBroken(context, ActivityType.SESSION, metrics.timeElapsed);
        }
    }

    private void handleSubjectTracking(Session sessionData, Semester semester) {
        String sessionTopic = sessionData.getSessionTopic();
        if (sessionTopic == null)
            return;

        String subjectCode = extractSubjectCode(sessionTopic);
        List<Subject> subjects = semester.getSemesterSubjects();

        subjects.stream()
                .filter(subject -> subject.getSubjectCode().trim().equalsIgnoreCase(subjectCode.trim()))
                .findFirst()
                .ifPresent(subject -> {
                    sessionData.addSubjectStudied(subject.getSubjectCode());
                    subject.incrementTimesStudied();
                });
    }

    private String extractSubjectCode(String sessionTopic) {
        if (sessionTopic.contains(" - ")) {
            return sessionTopic.split(" - ")[0].trim();
        }
        return sessionTopic.trim();
    }

    private void resetSessionData(Session sessionData) {
        sessionData.setSubjectsStudied(new ArrayList<>());
        sessionData.setSessionStartTime(null);
        sessionData.setChannelID(null);
        sessionData.setMessageID(null);
        sessionData.setGuildID(null);
        sessionData.setNumberOfBreaks(0);
        sessionData.setSessionTime(0.0);

        SessionBreak sessionBreak = sessionData.getSessionBreaks();
        if (sessionBreak != null) {
            sessionBreak.setSessionBreakTime(0.0);
            sessionBreak.setSessionBreakStart(null);
        }

        sessionData.setLastSessionTopic(sessionData.getSessionTopic());
    }

    private List<TimerButtonID> createEnabledButtons(TimerButtonID... buttons) {
        return Arrays.asList(buttons);
    }
}