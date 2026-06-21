package org.bunnys.nexus.timers.services;

import org.bunnys.database.models.timers.Grade;
import org.bunnys.database.models.timers.Semester;
import org.bunnys.database.models.timers.Subject;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.nexus.timers.engine.LevelEngine;
import org.bunnys.nexus.timers.TimerStats;
import org.bunnys.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class TimerDisplayService {

        public static String buildGPAMenu(BunnyUser user) {
                List<Subject> subjects = user.getSubjects();
                if (subjects == null || subjects.isEmpty()) {
                        return "No subjects registered.";
                }

                StringBuilder sb = new StringBuilder();
                Map<String, Integer> gradeCounts = new HashMap<>();

                List<Subject> sortedSubjects = subjects.stream()
                                .sorted((s1, s2) -> {
                                        double gpa1 = s1.getGradeEnum() != null ? s1.getGradeEnum().getGpaValue()
                                                        : -1.0;
                                        double gpa2 = s2.getGradeEnum() != null ? s2.getGradeEnum().getGpaValue()
                                                        : -1.0;
                                        return Double.compare(gpa2, gpa1);
                                })
                                .toList();

                for (Subject subject : sortedSubjects) {
                        String gradeStr = subject.getGrade() != null ? subject.getGrade() : "N/A";
                        sb.append("• ").append(subject.getSubjectName()).append(" - ").append(gradeStr).append("\n");

                        if (subject.getGradeEnum() != null) {
                                gradeCounts.put(gradeStr, gradeCounts.getOrDefault(gradeStr, 0) + 1);
                        }
                }

                sb.append(String.format("• GPA: %.3f\n", user.calculateCumulativeGPA()));

                String summary = gradeCounts.entrySet().stream()
                                .sorted((e1, e2) -> {
                                        double gpa1 = Objects.requireNonNull(Grade.fromString(e1.getKey()))
                                                        .getGpaValue();
                                        double gpa2 = Objects.requireNonNull(Grade.fromString(e2.getKey()))
                                                        .getGpaValue();
                                        return Double.compare(gpa2, gpa1);
                                })
                                .map(e -> e.getValue() + " " + e.getKey())
                                .collect(Collectors.joining(", "));

                if (!summary.isEmpty()) {
                        sb.append("• ").append(summary);
                }

                return sb.toString();
        }

        public static String buildStatDisplay(TimerStats stats) {
                StringBuilder sb = new StringBuilder();
                String gap = "\n\n";

                double lifetimeTimeMs = stats.getTotalStudyTime();
                sb.append("• Lifetime Study Time: ")
                                .append(lifetimeTimeMs > 0 ? Utils.msToTime((long) lifetimeTimeMs).orElse("0s") +
                                                " [" + Utils.secondsToHours(lifetimeTimeMs / 1000) + "]" : "0s")
                                .append("\n");

                if (stats.timerData().getCurrentSemester() != null
                                && stats.timerData().getCurrentSemester().getSemesterName() != null) {
                        double semTimeMs = stats.getSemesterTime();
                        sb.append("• Semester Study Time: ")
                                        .append(semTimeMs > 0 ? Utils.msToTime((long) semTimeMs).orElse("0s") +
                                                        " [" + Utils.secondsToHours(semTimeMs / 1000) + "]" : "0s")
                                        .append("\n")
                                        .append("• Average Session Time: ")
                                        .append(stats.getAverageSessionTime() > 0 ? Utils
                                                        .msToTime((long) stats.getAverageSessionTime()).orElse("0s")
                                                        : "0s")
                                        .append("\n")
                                        .append("• Total Sessions: ").append(stats.getSessionCount());
                        sb.append(gap);

                        int numberOfWeeks = stats.getSessionCount() / 7;
                        double averageTimePerWeek = numberOfWeeks == 0 ? 0 : stats.getSemesterTime() / numberOfWeeks;

                        sb.append("• Average Session Time / 7 Sessions: ")
                                        .append(averageTimePerWeek > 0
                                                        ? Utils.msToTime((long) averageTimePerWeek).orElse("0s") +
                                                                        " ["
                                                                        + Utils.secondsToHours(
                                                                                        averageTimePerWeek / 1000)
                                                                        + "]"
                                                        : "0s")
                                        .append("\n");
                }

                sb.append("• Study Streak: ").append(stats.getCurrentStreak()).append(" 🔥\n")
                                .append("• Longest Study Streak: ").append(stats.getLongestStreak()).append(" 🔥");
                sb.append(gap);

                if (stats.timerData().getCurrentSemester().getSemesterName() != null) {
                        sb.append("• Longest Session: ")
                                        .append(stats.getLongestSessionTime() > 0 ? Utils
                                                        .msToTime((long) stats.getLongestSessionTime()).orElse("0s")
                                                        : "0s")
                                        .append("\n");
                }

                Semester longestSemester = stats.getLongestSemester();
                if (longestSemester != null) {
                        sb.append("• Longest Semester: ")
                                        .append(Utils.msToTime((long) (longestSemester.getSemesterTime() * 1000))
                                                        .orElse("0s"))
                                        .append(" [").append(Utils.secondsToHours(longestSemester.getSemesterTime()))
                                        .append("] - [")
                                        .append(longestSemester.getSemesterName()).append("]");
                }
                sb.append(gap);

                if (stats.timerData().getCurrentSemester().getSemesterName() != null) {
                        sb.append("• Semester Break Time: ")
                                        .append(stats.getBreakTime() > 0
                                                        ? Utils.msToTime((long) stats.getBreakTime()).orElse("0s")
                                                        : "0s")
                                        .append("\n")
                                        .append("• Total Breaks: ").append(stats.getBreakCount()).append("\n")
                                        .append("• Average Break Time: ")
                                        .append(stats.getAverageBreakTime() > 0 ? Utils
                                                        .msToTime((long) stats.getAverageBreakTime()).orElse("0s")
                                                        : "0s")
                                        .append("\n")
                                        .append("• Average Start Time: ")
                                        .append(stats.getAverageStartTimeUNIX() != null
                                                        ? "<t:" + stats.getAverageStartTimeUNIX() + ":t>"
                                                        : "N/A");
                        sb.append(gap);

                        sb.append("• Total Subjects: ").append(stats.getSubjectCount()).append("\n")
                                        .append("• Total study instances across all subjects: ")
                                        .append(stats.getTotalTimesStudied()).append("\n\n");

                        List<Subject> orderedSubjects = stats.getSubjectsInOrder();
                        if (!orderedSubjects.isEmpty()) {
                                sb.append("**Subject Stats**\n");
                                for (Subject sub : orderedSubjects) {
                                        double subTimeMs = sub.getTotalStudyTime() != null
                                                        ? sub.getTotalStudyTime() * 1000.0
                                                        : 0;
                                        sb.append("• ").append(sub.getSubjectCode()).append(" - ")
                                                        .append(sub.getSubjectName())
                                                        .append(" [Instances: ").append(sub.getTimesStudied())
                                                        .append("] | Time: ")
                                                        .append(subTimeMs > 0
                                                                        ? Utils.msToTime((long) subTimeMs).orElse("0s")
                                                                        : "0s")
                                                        .append(" (").append(Utils.secondsToHours(subTimeMs / 1000))
                                                        .append(")\n");
                                }
                        } else
                                sb.append("**Subject Stats**\nN/A\n");

                        sb.append("• Average Study Time Per Subject: ")
                                        .append(stats.getAverageStudyTimePerSubject() > 0
                                                        ? Utils.msToTime((long) stats.getAverageStudyTimePerSubject())
                                                                        .orElse("0s")
                                                        : "N/A");
                        sb.append(gap);

                        int currentSemLevel = stats.getSemesterLevel();
                        long xpToNext = LevelEngine.xpRequired(currentSemLevel + 1);

                        sb.append(LevelEngine.rankUpEmoji(currentSemLevel)).append(" Semester Level: ")
                                        .append(currentSemLevel).append("\n")
                                        .append("• XP to reach level ").append(currentSemLevel + 1).append(": ")
                                        .append((long) stats.getSemesterXP()).append("/").append(xpToNext).append("\n")
                                        .append(stats.generateProgressBar(stats.percentageToNextLevel())).append(" [")
                                        .append(stats.percentageToNextLevel()).append("%]\n")
                                        .append("• Time until level ").append(currentSemLevel + 1).append(": ")
                                        .append(Utils.msToTime((long) stats.getMsToNextLevel()).orElse("0s"))
                                        .append("\n\n");

                        int currentAccLevel = stats.getAccountLevel();
                        long rpToNext = LevelEngine.rpRequired(currentAccLevel + 1);

                        sb.append(LevelEngine.rankUpEmoji(currentAccLevel)).append(" Account Level: ")
                                        .append(currentAccLevel).append("\n")
                                        .append("• RP to reach level ").append(currentAccLevel + 1).append(": ")
                                        .append(stats.getAccountRP()).append("/").append(rpToNext).append("\n")
                                        .append(stats.generateProgressBar(stats.percentageToNextRank())).append(" [")
                                        .append(stats.percentageToNextRank()).append("%]\n")
                                        .append("• Time until level ").append(currentAccLevel + 1).append(": ")
                                        .append(Utils.msToTime((long) stats.getMsToNextRank()).orElse("0s"))
                                        .append("\n\n")
                                        .append(String.format("• GPA: %.3f", stats.GPA()));
                }

                return sb.toString();
        }
}