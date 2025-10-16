package org.bunnys.executors.timer.engine;

import org.bunnys.database.models.timer.*;
import org.bunnys.database.models.users.GBFUser;
import org.bunnys.handler.utils.handler.Emojis;
import org.bunnys.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized TimerStats with better performance, caching, and cleaner API
 * design.
 * Immutable after construction with lazy-loaded calculations.
 */
@SuppressWarnings("unused")
public final class TimerStats {
    private static final long MS_PER_SECOND = 1000L;
    private static final double HOURS_PER_MS = 1.0 / (60.0 * 60.0 * 1000.0);

    private final TimerData timerData;
    private final GBFUser userData;

    // Cached calculations (lazy-loaded)
    private Long totalStudyTimeMs;
    private Long semesterTimeMs;
    private Integer sessionCount;
    private LevelCalculation levelCalc;
    private RankCalculation rankCalc;

    // Internal calculation holders
    private record LevelCalculation(
            int storedLevel, int storedXP, int actualLevel, int actualXP,
            int nextLevelXP, int percentToNext, long msToNext) {
    }

    private record RankCalculation(
            int storedRank, int storedRP, int actualRank, int actualRP,
            int nextRankRP, int percentToNext, long msToNext) {
    }

    public TimerStats(TimerData timerData, GBFUser userData) {
        this.timerData = Objects.requireNonNull(timerData, "timerData cannot be null");
        this.userData = Objects.requireNonNull(userData, "userData cannot be null");
    }

    // === Time Calculations (with caching) ===
    public long totalStudyTimeMs() {
        if (totalStudyTimeMs == null) {
            totalStudyTimeMs = Optional.ofNullable(timerData.getAccount())
                    .map(Account::getLifetimeTime)
                    .map(seconds -> Math.round(seconds * MS_PER_SECOND))
                    .orElse(0L);
        }
        return totalStudyTimeMs;
    }

    public long semesterTimeMs() {
        if (semesterTimeMs == null) {
            semesterTimeMs = Optional.ofNullable(timerData.getCurrentSemester())
                    .map(Semester::getSemesterTime)
                    .map(seconds -> Math.round(seconds * MS_PER_SECOND))
                    .orElse(0L);
        }
        return semesterTimeMs;
    }

    public int sessionCount() {
        if (sessionCount == null) {
            sessionCount = Optional.ofNullable(timerData.getCurrentSemester())
                    .map(Semester::getSessionStartTimes)
                    .map(List::size)
                    .orElse(0);
        }
        return sessionCount;
    }

    public long averageSessionTimeMs() {
        int count = sessionCount();
        return count == 0 ? 0L : semesterTimeMs() / count;
    }

    // === Level/Rank Calculations (cached) ===
    private LevelCalculation getLevelCalculation() {
        if (levelCalc == null) {
            int storedLevel = Optional.ofNullable(timerData.getCurrentSemester())
                    .map(Semester::getSemesterLevel)
                    .orElse(0);
            int storedXP = Optional.ofNullable(timerData.getCurrentSemester())
                    .map(Semester::getSemesterXP)
                    .orElse(0);

            LevelEngine.LevelResult result = LevelEngine.checkLevel(storedLevel, storedXP, 0);
            int actualLevel = storedLevel + result.addedLevels();
            int actualXP = result.remainingXP();
            int nextLevelXP = LevelEngine.xpRequired(actualLevel + 1);
            int percentToNext = LevelEngine.safePercentage(actualXP, nextLevelXP);
            long msToNext = Math.round(LevelEngine.hoursRequired(nextLevelXP - actualXP) * 3_600_000);

            levelCalc = new LevelCalculation(storedLevel, storedXP, actualLevel, actualXP,
                    nextLevelXP, percentToNext, msToNext);
        }
        return levelCalc;
    }

    private RankCalculation getRankCalculation() {
        if (rankCalc == null) {
            int storedRank = Optional.ofNullable(userData.getRank()).orElse(0);
            int storedRP = Optional.ofNullable(userData.getRP()).orElse(0);

            LevelEngine.RankResult result = LevelEngine.checkRank(storedRank, storedRP, 0);
            int actualRank = storedRank + result.addedRanks();
            int actualRP = result.remainingRP();
            int nextRankRP = LevelEngine.rpRequired(actualRank + 1);
            int percentToNext = LevelEngine.safePercentage(actualRP, nextRankRP);
            long msToNext = Math.round(LevelEngine.hoursRequired(nextRankRP - actualRP) * 3_600_000);

            rankCalc = new RankCalculation(storedRank, storedRP, actualRank, actualRP,
                    nextRankRP, percentToNext, msToNext);
        }
        return rankCalc;
    }

    // === Public Level/Rank API ===
    public int actualSemesterLevel() {
        return getLevelCalculation().actualLevel;
    }

    public int actualSemesterXP() {
        return getLevelCalculation().actualXP;
    }

    public int xpToNextLevel() {
        return getLevelCalculation().nextLevelXP;
    }

    public int percentageToNextLevel() {
        return getLevelCalculation().percentToNext;
    }

    public long msToNextLevel() {
        return getLevelCalculation().msToNext;
    }

    public int actualAccountLevel() {
        return getRankCalculation().actualRank;
    }

    public int actualAccountRP() {
        return getRankCalculation().actualRP;
    }

    public int rpToNextRank() {
        return getRankCalculation().nextRankRP;
    }

    public int percentageToNextRank() {
        return getRankCalculation().percentToNext;
    }

    public long msToNextRank() {
        return getRankCalculation().msToNext;
    }

    // === Subject Statistics ===
    public int subjectCount() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getSemesterSubjects)
                .map(List::size)
                .orElse(0);
    }

    public List<Subject> topSubjects(int limit) {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getSemesterSubjects)
                .orElse(Collections.emptyList())
                .stream()
                .sorted(Comparator.comparingInt(Subject::getTimesStudied).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }

    public List<Subject> subjectsByTimesStudied() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getSemesterSubjects)
                .orElse(Collections.emptyList())
                .stream()
                .sorted(Comparator.comparingInt(Subject::getTimesStudied).reversed())
                .collect(Collectors.toList());
    }

    public int totalTimesStudied() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getSemesterSubjects)
                .orElse(Collections.emptyList())
                .stream()
                .mapToInt(Subject::getTimesStudied)
                .sum();
    }

    public long averageStudyTimePerOccurrenceMs() {
        int totalOccurrences = totalTimesStudied();
        return totalOccurrences == 0 ? 0L : semesterTimeMs() / totalOccurrences;
    }

    // === Break Statistics ===
    public long breakTimeMs() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getTotalBreakTime)
                .map(seconds -> Math.round(seconds * MS_PER_SECOND))
                .orElse(0L);
    }

    public int breakCount() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getBreakCount)
                .orElse(0);
    }

    public long averageBreakTimeMs() {
        int count = breakCount();
        return count == 0 ? 0L : breakTimeMs() / count;
    }

    public long averageTimeBetweenBreaksMs() {
        int bc = breakCount();
        long stm = semesterTimeMs();
        return bc == 0 ? stm : stm / (bc + 1);
    }

    // === Streak Information ===
    public int currentStreak() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getStreak)
                .orElse(0);
    }

    public int longestStreak() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getLongestStreak)
                .orElse(0);
    }

    // === Records ===
    public long longestSessionMs() {
        return Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getLongestSession)
                .map(seconds -> Math.round(seconds * MS_PER_SECOND))
                .orElse(0L);
    }

    public Optional<Semester> longestSemester() {
        return Optional.ofNullable(timerData.getAccount())
                .map(Account::getLongestSemester);
    }

    // === Timing Utilities ===
    public OptionalLong averageStartTimeUnixSeconds() {
        List<Long> starts = Optional.ofNullable(timerData.getCurrentSemester())
                .map(Semester::getSessionStartTimes)
                .orElse(Collections.emptyList());

        if (starts.isEmpty())
            return OptionalLong.empty();

        long avgMillis = (long) starts.stream().mapToLong(Long::longValue).average().orElse(0.0);
        return OptionalLong.of(avgMillis / MS_PER_SECOND);
    }

    // === GPA ===
    public GradeEngine.GpaResult gpaResult() {
        return GradeEngine.calculateGPAFromGBF(userData.getSubjects());
    }

    // === Progress Bars ===
    public String progressBarForLevel() {
        return generateProgressBar(percentageToNextLevel(), 3);
    }

    public String progressBarForRank() {
        return generateProgressBar(percentageToNextRank(), 3);
    }

    public static String generateProgressBar(int percentage, int segments) {
        int clamped = Math.min(Math.max(percentage, 0), 100);
        int filled = Math.round((clamped / 100.0f) * segments);

        if (segments == 3) {
            return (filled >= 1 ? Emojis.PROGRESS_BAR_LEFT_FULL : Emojis.PROGRESS_BAR_LEFT_EMPTY) +
                    (filled >= 2 ? Emojis.PROGRESS_BAR_MIDDLE_FULL : Emojis.PROGRESS_BAR_MIDDLE_EMPTY) +
                    (filled >= 3 ? Emojis.PROGRESS_BAR_RIGHT_FULL : Emojis.PROGRESS_BAR_RIGHT_EMPTY);
        }

        StringBuilder sb = getStringBuilder(segments, filled);
        return sb.toString();
    }

    @NotNull
    private static StringBuilder getStringBuilder(int segments, int filled) {
        StringBuilder sb = new StringBuilder(segments);
        for (int i = 0; i < segments; i++) {
            if (i == 0) {
                sb.append(i < filled ? Emojis.PROGRESS_BAR_LEFT_FULL : Emojis.PROGRESS_BAR_LEFT_EMPTY);
            } else if (i == segments - 1) {
                sb.append(i < filled ? Emojis.PROGRESS_BAR_RIGHT_FULL : Emojis.PROGRESS_BAR_RIGHT_EMPTY);
            } else {
                sb.append(i < filled ? Emojis.PROGRESS_BAR_MIDDLE_FULL : Emojis.PROGRESS_BAR_MIDDLE_EMPTY);
            }
        }
        return sb;
    }

    // === Formatting Utilities ===
    public String humanDuration(long ms) {
        return Utils.formatDuration(ms);
    }

    // === Summary DTO (optimized) ===
    public record OptimizedSummary(
            // Time metrics
            long totalStudyTimeMs, long semesterTimeMs, int sessionCount, long avgSessionMs,
            long longestSessionMs, long breakTimeMs, int breakCount, long avgBreakMs,

            // Subject metrics
            int subjectCount, int totalTimesStudied,

            // Level/Rank metrics (actual calculated values)
            int semesterLevel, int semesterXP, int semesterXPRequired, int semesterPercent,
            int accountLevel, int accountRP, int accountRPRequired, int accountPercent,

            // Timing
            OptionalLong avgStartTimeUnixSec) {
    }

    public OptimizedSummary snapshot() {
        LevelCalculation level = getLevelCalculation();
        RankCalculation rank = getRankCalculation();

        return new OptimizedSummary(
                totalStudyTimeMs(), semesterTimeMs(), sessionCount(), averageSessionTimeMs(),
                longestSessionMs(), breakTimeMs(), breakCount(), averageBreakTimeMs(),
                subjectCount(), totalTimesStudied(),
                level.actualLevel, level.actualXP, level.nextLevelXP, level.percentToNext,
                rank.actualRank, rank.actualRP, rank.nextRankRP, rank.percentToNext,
                averageStartTimeUnixSeconds());
    }
}