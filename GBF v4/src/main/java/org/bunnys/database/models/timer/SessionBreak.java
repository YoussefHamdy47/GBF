package org.bunnys.database.models.timer;

import java.util.Date;
import java.util.Objects;

@SuppressWarnings("unused")
public class SessionBreak {
    private double sessionBreakTime;
    private Date sessionBreakStart;

    public SessionBreak() {
    }

    public SessionBreak(double sessionBreakTime, Date sessionBreakStart) {
        setSessionBreakTime(sessionBreakTime);
        setSessionBreakStart(sessionBreakStart);
    }

    // Getters with defensive copying
    public double getSessionBreakTime() {
        return sessionBreakTime;
    }

    public Date getSessionBreakStart() {
        return sessionBreakStart != null ? new Date(sessionBreakStart.getTime()) : null;
    }

    // Setters with validation
    public void setSessionBreakTime(double sessionBreakTime) {
        if (sessionBreakTime < 0) {
            throw new IllegalArgumentException("Session break time cannot be negative");
        }
        this.sessionBreakTime = sessionBreakTime;
    }

    public void setSessionBreakStart(Date sessionBreakStart) {
        this.sessionBreakStart = sessionBreakStart != null ? new Date(sessionBreakStart.getTime()) : null;
    }

    // Utility methods
    public boolean isActive() {
        return sessionBreakStart != null && sessionBreakTime == 0;
    }

    public long getBreakDurationMillis() {
        return sessionBreakStart != null ? System.currentTimeMillis() - sessionBreakStart.getTime() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SessionBreak that = (SessionBreak) o;
        return Double.compare(that.sessionBreakTime, sessionBreakTime) == 0 &&
                Objects.equals(sessionBreakStart, that.sessionBreakStart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionBreakTime, sessionBreakStart);
    }
}