package org.bunnys.database.models.timer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@SuppressWarnings("unused")
public class Semester {
    private String semesterName;
    private int semesterLevel;
    private int semesterXP;
    private double semesterTime;
    private List<Subject> semesterSubjects;
    private List<Long> sessionStartTimes;
    private double totalBreakTime;
    private int breakCount;
    private double longestSession;
    private int longestStreak;
    private int streak;
    private Date lastStreakUpdate;

    public Semester() {
        this.semesterSubjects = new ArrayList<>();
        this.sessionStartTimes = new ArrayList<>();
    }

    // Getters - return defensive copies for collections
    public String getSemesterName() {
        return semesterName;
    }

    public int getSemesterLevel() {
        return semesterLevel;
    }

    public int getSemesterXP() {
        return semesterXP;
    }

    public double getSemesterTime() {
        return semesterTime;
    }

    public List<Subject> getSemesterSubjects() {
        return semesterSubjects != null ? new ArrayList<>(semesterSubjects) : new ArrayList<>();
    }

    public List<Long> getSessionStartTimes() {
        return sessionStartTimes != null ? new ArrayList<>(sessionStartTimes) : new ArrayList<>();
    }

    public double getTotalBreakTime() {
        return totalBreakTime;
    }

    public int getBreakCount() {
        return breakCount;
    }

    public double getLongestSession() {
        return longestSession;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public int getStreak() {
        return streak;
    }

    public Date getLastStreakUpdate() {
        return lastStreakUpdate != null ? new Date(lastStreakUpdate.getTime()) : null;
    }

    // Setters with validation
    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public void setSemesterLevel(int semesterLevel) {
        if (semesterLevel < 0) {
            throw new IllegalArgumentException("Semester level cannot be negative");
        }
        this.semesterLevel = semesterLevel;
    }

    public void setSemesterXP(int semesterXP) {
        if (semesterXP < 0) {
            throw new IllegalArgumentException("Semester XP cannot be negative");
        }
        this.semesterXP = semesterXP;
    }

    public void setSemesterTime(double semesterTime) {
        if (semesterTime < 0) {
            throw new IllegalArgumentException("Semester time cannot be negative");
        }
        this.semesterTime = semesterTime;
    }

    public void setSemesterSubjects(List<Subject> semesterSubjects) {
        this.semesterSubjects = semesterSubjects != null ? new ArrayList<>(semesterSubjects) : new ArrayList<>();
    }

    public void setSessionStartTimes(List<Long> sessionStartTimes) {
        this.sessionStartTimes = sessionStartTimes != null ? new ArrayList<>(sessionStartTimes) : new ArrayList<>();
    }

    public void setTotalBreakTime(double totalBreakTime) {
        if (totalBreakTime < 0) {
            throw new IllegalArgumentException("Total break time cannot be negative");
        }
        this.totalBreakTime = totalBreakTime;
    }

    public void setBreakCount(int breakCount) {
        if (breakCount < 0) {
            throw new IllegalArgumentException("Break count cannot be negative");
        }
        this.breakCount = breakCount;
    }

    public void setLongestSession(double longestSession) {
        if (longestSession < 0) {
            throw new IllegalArgumentException("Longest session cannot be negative");
        }
        this.longestSession = longestSession;
    }

    public void setLongestStreak(int longestStreak) {
        if (longestStreak < 0) {
            throw new IllegalArgumentException("Longest streak cannot be negative");
        }
        this.longestStreak = longestStreak;
    }

    public void setStreak(int streak) {
        if (streak < 0) {
            throw new IllegalArgumentException("Streak cannot be negative");
        }
        this.streak = streak;
    }

    public void setLastStreakUpdate(Date lastStreakUpdate) {
        this.lastStreakUpdate = lastStreakUpdate != null ? new Date(lastStreakUpdate.getTime()) : null;
    }

    // Utility methods
    public void addSubject(Subject subject) {
        if (subject != null && this.semesterSubjects != null) {
            this.semesterSubjects.add(subject);
        }
    }

    public void addSessionTime(long startTime) {
        if (this.sessionStartTimes != null) {
            this.sessionStartTimes.add(startTime);
        }
    }

    public void addXP(int xp) {
        if (xp > 0) {
            this.semesterXP += xp;
        }
    }

    public void addTime(double time) {
        if (time > 0) {
            this.semesterTime += time;
            if (time > this.longestSession) {
                this.longestSession = time;
            }
        }
    }

    public boolean isStreakCurrent() {
        if (lastStreakUpdate == null)
            return false;
        LocalDate today = LocalDate.now();
        LocalDate lastUpdate = lastStreakUpdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return today.equals(lastUpdate) || today.equals(lastUpdate.plusDays(1));
    }

    public void updateStreak() {
        LocalDate today = LocalDate.now();
        if (lastStreakUpdate != null) {
            LocalDate lastUpdate = lastStreakUpdate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (today.equals(lastUpdate.plusDays(1))) {
                this.streak++;
                if (this.streak > this.longestStreak) {
                    this.longestStreak = this.streak;
                }
            } else if (!today.equals(lastUpdate)) {
                this.streak = 1;
            }
        } else {
            this.streak = 1;
        }
        this.lastStreakUpdate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}