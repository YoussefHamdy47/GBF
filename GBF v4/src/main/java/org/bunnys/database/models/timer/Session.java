package org.bunnys.database.models.timer;

import java.util.*;

@SuppressWarnings("unused")
public class Session {
    private String guildID;
    private String channelID;
    private String messageID;
    private Date sessionStartTime;
    private String sessionTopic;
    private double sessionTime;
    private int numberOfBreaks;
    private SessionBreak sessionBreaks;
    private String lastSessionTopic;
    private Date lastSessionDate;
    private List<String> subjectsStudied;

    public Session() {
        this.subjectsStudied = new ArrayList<>();
    }

    // Getters with defensive copying
    public String getGuildID() {
        return guildID;
    }

    public String getChannelID() {
        return channelID;
    }

    public String getMessageID() {
        return messageID;
    }

    public Date getSessionStartTime() {
        return sessionStartTime != null ? new Date(sessionStartTime.getTime()) : null;
    }

    public String getSessionTopic() {
        return sessionTopic;
    }

    public double getSessionTime() {
        return sessionTime;
    }

    public int getNumberOfBreaks() {
        return numberOfBreaks;
    }

    public SessionBreak getSessionBreaks() {
        return sessionBreaks;
    }

    public String getLastSessionTopic() {
        return lastSessionTopic;
    }

    public Date getLastSessionDate() {
        return lastSessionDate != null ? new Date(lastSessionDate.getTime()) : null;
    }

    public List<String> getSubjectsStudied() {
        return subjectsStudied != null ? new ArrayList<>(subjectsStudied) : new ArrayList<>();
    }

    // Setters with validation
    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public void setSessionStartTime(Date sessionStartTime) {
        this.sessionStartTime = sessionStartTime != null ? new Date(sessionStartTime.getTime()) : null;
    }

    public void setSessionTopic(String sessionTopic) {
        this.sessionTopic = sessionTopic;
    }

    public void setSessionTime(double sessionTime) {
        if (sessionTime < 0) {
            throw new IllegalArgumentException("Session time cannot be negative");
        }
        this.sessionTime = sessionTime;
    }

    public void setNumberOfBreaks(int numberOfBreaks) {
        if (numberOfBreaks < 0) {
            throw new IllegalArgumentException("Number of breaks cannot be negative");
        }
        this.numberOfBreaks = numberOfBreaks;
    }

    public void setSessionBreaks(SessionBreak sessionBreaks) {
        this.sessionBreaks = sessionBreaks;
    }

    public void setLastSessionTopic(String lastSessionTopic) {
        this.lastSessionTopic = lastSessionTopic;
    }

    public void setLastSessionDate(Date lastSessionDate) {
        this.lastSessionDate = lastSessionDate != null ? new Date(lastSessionDate.getTime()) : null;
    }

    public void setSubjectsStudied(List<String> subjectsStudied) {
        this.subjectsStudied = subjectsStudied != null ? new ArrayList<>(subjectsStudied) : new ArrayList<>();
    }

    // Utility methods
    public void addSubjectStudied(String subject) {
        if (subject != null && !subject.trim().isEmpty()) {
            if (this.subjectsStudied == null) {
                this.subjectsStudied = new ArrayList<>();
            }
            if (!this.subjectsStudied.contains(subject)) {
                this.subjectsStudied.add(subject);
            }
        }
    }

    public boolean isActive() {
        return sessionStartTime != null && sessionTime == 0;
    }

    public long getSessionDurationMillis() {
        return sessionStartTime != null ? System.currentTimeMillis() - sessionStartTime.getTime() : 0;
    }
}