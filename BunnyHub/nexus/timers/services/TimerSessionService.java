package org.bunnys.nexus.timers.services;

import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.bunnys.database.models.timers.Session;
import org.bunnys.database.models.timers.Subject;
import org.bunnys.database.models.timers.TimerData;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.handler.database.DB;
import org.bunnys.nexus.events.custom.AccountLevelUpEvent;
import org.bunnys.nexus.events.custom.RecordBrokenEvent;
import org.bunnys.nexus.events.custom.SemesterLevelUpEvent;
import org.bunnys.nexus.timers.engine.LevelEngine;
import org.bunnys.utils.Utils;

import java.util.*;

@SuppressWarnings("unused")
public class TimerSessionService {

    private static final String TIMER_COLLECTION = "TimerData";
    private static final String USER_COLLECTION = "BunnyUsers";
    private static final String TIMER_ID_FIELD = "account.userID";
    private static final String USER_ID_FIELD = "userID";
    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;
    private static final long TWO_DAYS_MS = 2 * ONE_DAY_MS;

    public static void startSession(String userId, String messageId, String channelId,
            String guildId, String topic) {
        TimerData timerData = getTimerDataOrThrow(userId);
        Session session = timerData.getSessionData();

        if (session.getSessionStartTime() != null)
            throw new IllegalStateException("You already have an active session running.");

        long now = System.currentTimeMillis();
        timerData.getCurrentSemester().getSessionStartTimes().add(now);
        resetSessionFields(session, now, topic, messageId, channelId, guildId);

        if (hasTopic(topic)) {
            String code = extractSubjectCode(topic);
            findSubject(timerData, code).ifPresent(subject -> {
                safeListAdd(session.getSubjectsStudied(), subject.getSubjectCode());
                subject.setTimesStudied(subject.getTimesStudied() + 1);
            });
        }

        saveTimerData(userId, timerData);
    }

    public static void pauseSession(String userId) {
        TimerData timerData = getTimerDataOrThrow(userId);
        Session session = timerData.getSessionData();

        requireActiveSession(session);
        if (session.getSessionBreaks().getSessionBreakStart() != null)
            throw new IllegalStateException("The timer is already paused.");

        session.getSessionBreaks().setSessionBreakStart(new Date(System.currentTimeMillis()));
        session.setNumberOfBreaks(session.getNumberOfBreaks() + 1);
        timerData.getCurrentSemester().setBreakCount(timerData.getCurrentSemester().getBreakCount() + 1);

        saveTimerData(userId, timerData);
    }

    public static long unpauseSession(String userId) {
        TimerData timerData = getTimerDataOrThrow(userId);
        Session session = timerData.getSessionData();

        requireActiveSession(session);
        if (session.getSessionBreaks().getSessionBreakStart() == null)
            throw new IllegalStateException("The timer is not currently paused.");

        long breakStartMs = session.getSessionBreaks().getSessionBreakStart().getTime();
        long elapsedBreakMs = System.currentTimeMillis() - breakStartMs;

        session.getSessionBreaks().setSessionBreakTime(
                session.getSessionBreaks().getSessionBreakTime() + (elapsedBreakMs / 1000.0));
        session.getSessionBreaks().setSessionBreakStart(null);

        saveTimerData(userId, timerData);
        return elapsedBreakMs;
    }

    public static String getSessionInfo(String userId) {
        TimerData timerData = getTimerDataOrThrow(userId);
        Session session = timerData.getSessionData();

        if (session.getSessionStartTime() == null)
            return "> *No active telemetry feed.*";

        long now = System.currentTimeMillis();
        long startMs = session.getSessionStartTime().getTime();

        double storedBreakSecs = session.getSessionBreaks().getSessionBreakTime();
        double activeBreakSecs = (session.getSessionBreaks().getSessionBreakStart() != null)
                ? (now - session.getSessionBreaks().getSessionBreakStart().getTime()) / 1000.0
                : 0.0;

        double totalBreakSecs = storedBreakSecs + activeBreakSecs;
        double totalElapsedSecs = (now - startMs) / 1000.0;
        double activeStudySecs = Math.max(0, totalElapsedSecs - totalBreakSecs);

        StringBuilder info = new StringBuilder();

        info.append("✦ Start Time: <t:").append(startMs / 1000).append(":f>\n");
        info.append("✦ Current Uptime: <t:").append(startMs / 1000).append(":R>\n\n");

        info.append("✦ Net Study Time: `").append(formatSecs(activeStudySecs)).append("`\n");
        info.append("✦ Total Break Time: `").append(formatSecs(totalBreakSecs)).append("`\n");
        info.append("✦ Break Count: `").append(session.getNumberOfBreaks()).append("`\n");

        if (activeBreakSecs > 0)
            info.append("\n> ⏸ *Currently on a break for ").append(formatSecs(activeBreakSecs)).append(".*");

        return info.toString();
    }

    public static net.dv8tion.jda.api.entities.MessageEmbed getTelemetryEmbed(String userId) {
        TimerData timerData = getTimerDataOrThrow(userId);
        Session session = timerData.getSessionData();

        net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
        eb.setColor(org.bunnys.utils.AppDesign.ColorCodes.CYAN);

        if (session.getSessionStartTime() == null) {
            eb.setTitle("💎 Live Telemetry");
            eb.setDescription("> *No active telemetry feed.*");
            return eb.build();
        }

        String topic = session.getSessionTopic();
        String cleanTopic = topic != null ? topic.split("-")[0].trim().toUpperCase() : "UNKNOWN";
        eb.setTitle("💎 Live Telemetry | " + cleanTopic);

        long now = System.currentTimeMillis();
        long startMs = session.getSessionStartTime().getTime();

        double storedBreakSecs = session.getSessionBreaks().getSessionBreakTime();
        double activeBreakSecs = (session.getSessionBreaks().getSessionBreakStart() != null)
                ? (now - session.getSessionBreaks().getSessionBreakStart().getTime()) / 1000.0
                : 0.0;

        double totalBreakSecs = storedBreakSecs + activeBreakSecs;
        double totalElapsedSecs = (now - startMs) / 1000.0;
        double activeStudySecs = Math.max(0, totalElapsedSecs - totalBreakSecs);

        StringBuilder info = new StringBuilder();
        info.append("✦ **Start Time:** <t:").append(startMs / 1000).append(":f>\n");
        info.append("✦ **Current Uptime:** <t:").append(startMs / 1000).append(":R>\n\n");

        info.append("✦ **Net Study Time:** `").append(formatSecs(activeStudySecs)).append("`\n");
        info.append("✦ **Total Break Time:** `").append(formatSecs(totalBreakSecs)).append("`\n");
        info.append("✦ **Break Count:** `").append(session.getNumberOfBreaks()).append("`\n");

        if (activeBreakSecs > 0)
            info.append("\n> ⏸ *Currently on a break. Duration: ").append(formatSecs(activeBreakSecs)).append(".*");
        else
            info.append("\n> ▶ *Telemetry feed active and recording.*");

        eb.setDescription(info.toString());
        eb.setTimestamp(java.time.Instant.now());
        return eb.build();
    }

    /**
     * Stops the current session, persists all stat updates, fires events, and
     * returns a formatted recap string.
     *
     * <p>
     * Key ordering:
     * <ol>
     * <li>Validate session state (before fetching BunnyUser to avoid wasted DB
     * calls).</li>
     * <li>Fetch BunnyUser with a null guard.</li>
     * <li>Compute all derived values.</li>
     * <li>Mutate both documents in memory.</li>
     * <li>Persist both documents.</li>
     * <li>Fire events (after persistence so handlers see fresh DB state).</li>
     * </ol>
     */
    public static String stopSession(String userId, IReplyCallback interaction) {
        TimerData timerData = getTimerDataOrThrow(userId);
        Session session = timerData.getSessionData();

        requireActiveSession(session);
        if (session.getSessionBreaks().getSessionBreakStart() != null)
            throw new IllegalStateException(
                    "You cannot end the session while the timer is paused. Please unpause first.");

        BunnyUser userData = DB.findOne(BunnyUser.class, USER_COLLECTION,
                Filters.eq(USER_ID_FIELD, userId));
        if (userData == null)
            throw new IllegalStateException("User account not found. Please register first.");

        long now = System.currentTimeMillis();
        long startMs = session.getSessionStartTime().getTime();
        double totalBreakSecs = session.getSessionBreaks().getSessionBreakTime();
        double totalElapsedSecs = (now - startMs) / 1000.0;
        double activeStudySecs = Math.max(0, totalElapsedSecs - totalBreakSecs);

        List<String> subjectsStudied = session.getSubjectsStudied();
        int numberOfBreaks = session.getNumberOfBreaks();

        double previouslyAllocatedSecs = session.getSessionTime();
        double finalSubjectTime = Math.max(0, activeStudySecs - previouslyAllocatedSecs);

        String currentTopic = session.getSessionTopic();
        if (currentTopic != null && !currentTopic.trim().isEmpty()) {
            String code = currentTopic.split("-")[0].trim().toUpperCase();
            timerData.getCurrentSemester().getSemesterSubjects().stream()
                    .filter(s -> s.getSubjectCode().equalsIgnoreCase(code))
                    .findFirst()
                    .ifPresent(subject -> {
                        double current = subject.getTotalStudyTime() != null ? subject.getTotalStudyTime() : 0.0;
                        subject.setTotalStudyTime(current + finalSubjectTime);
                    });
        }

        updateSemesterStats(timerData, totalBreakSecs, activeStudySecs);

        boolean brokeRecord = timerData.getCurrentSemester().getLongestSession() < activeStudySecs;
        if (brokeRecord)
            timerData.getCurrentSemester().setLongestSession(activeStudySecs);

        long pointsEarned = LevelEngine.calculateXP(activeStudySecs / 60.0);

        LevelEngine.RankResult rankResult = applyRankProgress(userData, pointsEarned);
        LevelEngine.LevelResult levelResult = applyLevelProgress(timerData, pointsEarned);
        updateStreak(timerData, now);

        String recap = buildRecap(startMs, totalElapsedSecs, activeStudySecs,
                totalBreakSecs, numberOfBreaks, subjectsStudied, pointsEarned);

        clearSessionFields(session);

        saveTimerData(userId, timerData);
        DB.save(BunnyUser.class, USER_COLLECTION, Filters.eq(USER_ID_FIELD, userId), userData);

        if (brokeRecord)
            interaction.getJDA().getEventManager().handle(new RecordBrokenEvent(
                    interaction.getJDA(), interaction,
                    RecordBrokenEvent.RecordType.SESSION, activeStudySecs, null));

        if (rankResult.hasRankedUp())
            interaction.getJDA().getEventManager().handle(new AccountLevelUpEvent(
                    interaction.getJDA(), interaction,
                    rankResult.addedLevels(), rankResult.remainingRP(), userData));

        if (levelResult.hasLeveledUp())
            interaction.getJDA().getEventManager().handle(new SemesterLevelUpEvent(
                    interaction.getJDA(), interaction,
                    levelResult.addedLevels(), levelResult.remainingXP(), timerData));

        return recap;
    }

    /**
     * Throws if no session is currently active.
     */
    private static void requireActiveSession(Session session) {
        if (session.getSessionStartTime() == null)
            throw new IllegalStateException("You don't have an active session.");
    }

    /**
     * Initializes all session fields for a fresh start.
     */
    private static void resetSessionFields(Session session, long now, String topic,
            String messageId, String channelId, String guildId) {
        session.setSessionStartTime(new Date(now));
        session.setLastSessionDate(new Date(now));
        session.setSessionTopic(topic);
        session.setMessageID(messageId);
        session.setChannelID(channelId);
        session.setGuildID(guildId);
        session.setNumberOfBreaks(0);
        session.setSessionTime(0.0);
        session.getSessionBreaks().setSessionBreakStart(null);
        session.getSessionBreaks().setSessionBreakTime(0.0);
        safeListClear(session.getSubjectsStudied());
    }

    /**
     * Wipes all transient session state after a session ends.
     */
    private static void clearSessionFields(Session session) {
        session.setLastSessionTopic(session.getSessionTopic());
        session.setSessionStartTime(null);
        session.setChannelID(null);
        session.setMessageID(null);
        session.setGuildID(null);
        session.setSessionTopic(null);
        session.setNumberOfBreaks(0);
        session.setSessionTime(0.0);
        session.getSessionBreaks().setSessionBreakTime(0.0);
        session.getSessionBreaks().setSessionBreakStart(null);
        safeListClear(session.getSubjectsStudied());
    }

    /**
     * Distributes the active study time equally across all studied subjects.
     *
     * <p>
     * Builds a lookup map first to bring the complexity from O(n×m) down to
     * O(n+m) — no nested stream scans.
     */
    private static void updateSubjectStudyTimes(TimerData timerData,
            List<String> subjectsStudied,
            double activeStudySecs) {
        if (subjectsStudied == null || subjectsStudied.isEmpty())
            return;

        // Build map once — avoids O(n×m) stream scans in the loop below.
        Map<String, Subject> subjectMap = new HashMap<>();
        timerData.getCurrentSemester().getSemesterSubjects()
                .forEach(s -> subjectMap.put(s.getSubjectCode().toLowerCase(), s));

        double timePerSubject = activeStudySecs / subjectsStudied.size();

        for (String code : subjectsStudied) {
            Subject subject = subjectMap.get(code.toLowerCase());
            if (subject == null)
                continue;
            double current = subject.getTotalStudyTime() != null ? subject.getTotalStudyTime() : 0.0;
            subject.setTotalStudyTime(current + timePerSubject);
        }
    }

    /**
     * Adds this session's time to semester and lifetime accumulators.
     */
    private static void updateSemesterStats(TimerData timerData,
            double totalBreakSecs,
            double activeStudySecs) {
        var semester = timerData.getCurrentSemester();
        semester.setTotalBreakTime(semester.getTotalBreakTime() + totalBreakSecs);
        semester.setSemesterTime(semester.getSemesterTime() + activeStudySecs);
        timerData.getAccount().setLifetimeTime(timerData.getAccount().getLifetimeTime() + activeStudySecs);
    }

    /**
     * Applies earned points to the account RP track and returns the result.
     */
    private static LevelEngine.RankResult applyRankProgress(BunnyUser userData, long pointsEarned) {
        long currentRP = userData.getRp();
        LevelEngine.RankResult result = LevelEngine.checkRank(userData.getRank(), currentRP, pointsEarned);

        if (result.hasRankedUp()) {
            userData.setRank(userData.getRank() + Math.max(1, result.addedLevels()));
            userData.setRp((int) result.remainingRP());
        } else {
            userData.setRp((int) (currentRP + pointsEarned));
        }
        return result;
    }

    /**
     * Applies earned points to the semester XP track and returns the result.
     */
    private static LevelEngine.LevelResult applyLevelProgress(TimerData timerData, long pointsEarned) {
        var semester = timerData.getCurrentSemester();
        long currentXP = (long) semester.getSemesterXP();
        LevelEngine.LevelResult result = LevelEngine.checkLevel(
                semester.getSemesterLevel(), currentXP, pointsEarned);

        if (result.hasLeveledUp()) {
            semester.setSemesterLevel(semester.getSemesterLevel() + Math.max(1, result.addedLevels()));
            semester.setSemesterXP(result.remainingXP());
        } else {
            semester.setSemesterXP(currentXP + pointsEarned);
        }
        return result;
    }

    /**
     * Updates the daily study streak with correct break-detection logic.
     *
     * <ul>
     * <li>No prior update → first session, streak = 1.</li>
     * <li>≥ 2 days elapsed → missed at least one day; streak resets to 1.</li>
     * <li>≥ 1 day and &lt; 2 days elapsed → consecutive day; streak advances.</li>
     * <li>&lt; 1 day elapsed → already counted today; no change.</li>
     * </ul>
     */
    private static void updateStreak(TimerData timerData, long now) {
        var semester = timerData.getCurrentSemester();
        Date lastUpdate = semester.getLastStreakUpdate();

        if (lastUpdate == null) {
            semester.setStreak(1);
            semester.setLastStreakUpdate(new Date(now));
        } else {
            long elapsed = now - lastUpdate.getTime();

            if (elapsed >= TWO_DAYS_MS) {
                semester.setStreak(1);
                semester.setLastStreakUpdate(new Date(now));
            } else if (elapsed >= ONE_DAY_MS) {
                semester.setStreak(semester.getStreak() + 1);
                semester.setLastStreakUpdate(new Date(now));
            }
        }

        if (semester.getStreak() > semester.getLongestStreak())
            semester.setLongestStreak(semester.getStreak());
    }

    /**
     * Builds the session end recap string.
     */
    private static String buildRecap(long startMs, double totalElapsedSecs,
            double activeStudySecs, double totalBreakSecs,
            int numberOfBreaks, List<String> subjectsStudied,
            long pointsEarned) {
        StringBuilder recap = new StringBuilder();

        recap.append("• Start Time: <t:").append(startMs / 1000).append(":F>\n")
                .append("• Total Time Elapsed: ").append(formatSecs(totalElapsedSecs)).append("\n")
                .append("• Net Study Time:     ").append(formatSecs(activeStudySecs)).append("\n\n");

        double avgBreakSecs = (totalBreakSecs > 0 && numberOfBreaks > 0)
                ? totalBreakSecs / numberOfBreaks
                : 0.0;

        recap.append("• Total Break Time:   ")
                .append(totalBreakSecs > 0 ? formatSecs(totalBreakSecs) : "No Breaks Taken").append("\n")
                .append("• Average Break Time: ")
                .append(avgBreakSecs > 0 ? formatSecs(avgBreakSecs) : "N/A").append("\n")
                .append("• Number of Breaks:   ").append(numberOfBreaks).append("\n");

        if (subjectsStudied != null && !subjectsStudied.isEmpty())
            recap.append("\n• Studied Subjects: ").append(String.join(", ", subjectsStudied));
        else
            recap.append("\n• No subjects studied this session.");

        recap.append("\n\n• XP & RP Earned: ").append(String.format("%,d", pointsEarned));

        return recap.toString();
    }

    private static boolean hasTopic(String topic) {
        return topic != null && !topic.trim().isEmpty();
    }

    /**
     * Extracts the subject code from a topic string of the form "CODE-description".
     * Uses a limit of 2 so that codes containing hyphens (e.g. "CS-101") are
     * handled gracefully, and so that "-".split("-", 2) returns ["", ""] instead
     * of [] (which would cause an ArrayIndexOutOfBoundsException with no limit).
     */
    private static String extractSubjectCode(String topic) {
        return topic.split("-", 2)[0].trim();
    }

    private static Optional<Subject> findSubject(TimerData timerData, String subjectCode) {
        if (subjectCode.isEmpty())
            return Optional.empty();
        return timerData.getCurrentSemester().getSemesterSubjects().stream()
                .filter(s -> s.getSubjectCode().equalsIgnoreCase(subjectCode))
                .findFirst();
    }

    /** Null-safe list add. */
    private static void safeListAdd(List<String> list, String value) {
        if (list != null)
            list.add(value);
    }

    /** Null-safe list clear. */
    private static void safeListClear(List<String> list) {
        if (list != null)
            list.clear();
    }

    /** Formats a duration in seconds to a human-readable string. */
    private static String formatSecs(double seconds) {
        return Utils.msToTime((long) (seconds * 1000)).orElse("0s");
    }

    /**
     * Loads and validates TimerData for a given user.
     */
    public static TimerData getTimerDataOrThrow(String userId) {
        TimerData timerData = DB.findOne(TimerData.class, TIMER_COLLECTION,
                Filters.eq(TIMER_ID_FIELD, userId));
        if (timerData == null)
            throw new IllegalStateException("Timer account not found. Please register first.");
        if (timerData.getCurrentSemester() == null
                || timerData.getCurrentSemester().getSemesterName() == null)
            throw new IllegalStateException("No active semester found. Start a semester first.");
        return timerData;
    }

    private static void saveTimerData(String userId, TimerData data) {
        DB.save(TimerData.class, TIMER_COLLECTION, Filters.eq(TIMER_ID_FIELD, userId), data);
    }
}