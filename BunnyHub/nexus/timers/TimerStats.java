package org.bunnys.nexus.timers;

import org.bunnys.database.models.timers.Semester;
import org.bunnys.database.models.timers.Subject;
import org.bunnys.database.models.timers.TimerData;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.nexus.timers.engine.LevelEngine;
import org.bunnys.utils.AppDesign;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record TimerStats(TimerData timerData, BunnyUser userData) {

    public double getTotalStudyTime() {
        if (timerData.getAccount() == null)
            return 0;
        return timerData.getAccount().getLifetimeTime() > 0 ? timerData.getAccount().getLifetimeTime() * 1000 : 0;
    }

    public double getSemesterTime() {
        if (timerData.getCurrentSemester() == null)
            return 0;
        return timerData.getCurrentSemester().getSemesterTime() > 0
                ? timerData.getCurrentSemester().getSemesterTime() * 1000
                : 0;
    }

    public int getSessionCount() {
        if (timerData.getCurrentSemester() == null || timerData.getCurrentSemester().getSessionStartTimes() == null)
            return 0;
        return timerData.getCurrentSemester().getSessionStartTimes().size();
    }

    public double getAverageSessionTime() {
        double semTime = getSemesterTime();
        int count = getSessionCount();
        return (semTime > 0 && count > 0) ? semTime / count : 0;
    }

    public double getLongestSessionTime() {
        if (timerData.getCurrentSemester() == null)
            return 0;
        return timerData.getCurrentSemester().getLongestSession() > 0
                ? timerData.getCurrentSemester().getLongestSession() * 1000
                : 0;
    }

    public Semester getLongestSemester() {
        return timerData.getAccount() != null ? timerData.getAccount().getLongestSemester() : null;
    }

    public double getBreakTime() {
        if (timerData.getCurrentSemester() == null)
            return 0;
        return timerData.getCurrentSemester().getTotalBreakTime() > 0
                ? timerData.getCurrentSemester().getTotalBreakTime() * 1000
                : 0;
    }

    public int getBreakCount() {
        return timerData.getCurrentSemester() != null ? timerData.getCurrentSemester().getBreakCount() : 0;
    }

    public double getAverageBreakTime() {
        double bTime = getBreakTime();
        int bCount = getBreakCount();
        return (bTime > 0 && bCount > 0) ? bTime / bCount : 0;
    }

    public double getAverageTimeBetweenBreaks() {
        double sTime = getSemesterTime();
        int bCount = getBreakCount();
        return (sTime > 0 && bCount > 0) ? sTime / bCount : 0;
    }

    public int getSubjectCount() {
        if (timerData.getCurrentSemester() == null || timerData.getCurrentSemester().getSemesterSubjects() == null)
            return 0;
        return timerData.getCurrentSemester().getSemesterSubjects().size();
    }

    public List<Subject> getSubjectsInOrder() {
        if (getSubjectCount() == 0)
            return Collections.emptyList();

        return timerData.getCurrentSemester().getSemesterSubjects().stream()
                .sorted(Comparator.comparingInt(Subject::getTimesStudied).reversed())
                .collect(Collectors.toList());
    }

    public int getTotalTimesStudied() {
        if (getSubjectCount() == 0)
            return 0;
        return timerData.getCurrentSemester().getSemesterSubjects().stream()
                .mapToInt(Subject::getTimesStudied)
                .sum();
    }

    public double getAverageStudyTimePerSubject() {
        int totalTimes = getTotalTimesStudied();
        return totalTimes > 0 ? getSemesterTime() / totalTimes : 0;
    }

    public Long getAverageStartTimeUNIX() {
        if (getSessionCount() == 0)
            return null;

        List<Long> startTimes = timerData.getCurrentSemester().getSessionStartTimes();
        long sum = startTimes.stream().mapToLong(Long::longValue).sum();

        return (sum / startTimes.size()) / 1000L;
    }

    public int getSemesterLevel() {
        return timerData.getCurrentSemester() != null ? timerData.getCurrentSemester().getSemesterLevel() : 0;
    }

    public double getSemesterXP() {
        return timerData.getCurrentSemester() != null ? timerData.getCurrentSemester().getSemesterXP() : 0;
    }

    public int getAccountLevel() {
        return userData != null ? userData.getRank() : 0;
    }

    public int getAccountRP() {
        return userData != null ? userData.getRp() : 0;
    }

    public int percentageToNextRank() {
        if (userData == null)
            return 0;
        return (int) Math.round(((double) userData.getRp() / LevelEngine.rpRequired(userData.getRank())) * 100.0);
    }

    public int percentageToNextLevel() {
        if (timerData.getCurrentSemester() == null)
            return 0;
        double currentXP = timerData.getCurrentSemester().getSemesterXP();
        double requiredXP = LevelEngine.xpRequired(timerData.getCurrentSemester().getSemesterLevel() + 1);
        return (int) Math.round((currentXP / requiredXP) * 100.0);
    }

    public double getMsToNextLevel() {
        if (timerData.getCurrentSemester() == null)
            return 0;
        double xpLeft = LevelEngine.xpRequired(timerData.getCurrentSemester().getSemesterLevel() + 1)
                - timerData.getCurrentSemester().getSemesterXP();
        return LevelEngine.hoursRequired((long) xpLeft) * 60 * 60 * 1000;
    }

    public double getMsToNextRank() {
        if (userData == null)
            return 0;
        double rpLeft = LevelEngine.rpRequired(userData.getRank() + 1) - userData.getRp();
        return LevelEngine.hoursRequired((long) rpLeft) * 60 * 60 * 1000;
    }

    public double GPA() {
        return userData != null ? userData.calculateCumulativeGPA() : 0.0;
    }

    public int getCurrentStreak() {
        return timerData.getCurrentSemester() != null ? timerData.getCurrentSemester().getStreak() : 0;
    }

    public int getLongestStreak() {
        return timerData.getCurrentSemester() != null ? timerData.getCurrentSemester().getLongestStreak() : 0;
    }

    public String generateProgressBar(int percentageComplete) {
        int totalSegments = 3;
        int clampedPercentage = Math.min(Math.max(percentageComplete, 0), 100);
        int filledSegments = (int) Math.round((clampedPercentage / 100.0) * totalSegments);

        String left = filledSegments >= 1 ? AppDesign.Emojis.PROGRESS_BAR_LEFT_FULL
                : AppDesign.Emojis.PROGRESS_BAR_LEFT_EMPTY;
        String middle = filledSegments >= 2 ? AppDesign.Emojis.PROGRESS_BAR_MIDDLE_FULL
                : AppDesign.Emojis.PROGRESS_BAR_MIDDLE_EMPTY;
        String right = filledSegments >= 3 ? AppDesign.Emojis.PROGRESS_BAR_RIGHT_FULL
                : AppDesign.Emojis.PROGRESS_BAR_RIGHT_EMPTY;

        return left + middle + right;
    }
}