package org.bunnys.database.models.timer;

import java.util.Objects;

@SuppressWarnings("unused")
public class Account {
    private String userID;
    private double lifetimeTime;
    private Semester longestSemester;

    public Account() {
    }

    public Account(String userID, double lifetimeTime, Semester longestSemester) {
        this.userID = userID;
        this.lifetimeTime = lifetimeTime;
        this.longestSemester = longestSemester;
    }

    // Getters
    public String getUserID() {
        return userID;
    }

    public double getLifetimeTime() {
        return lifetimeTime;
    }

    public Semester getLongestSemester() {
        return longestSemester;
    }

    // Setters with validation
    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setLifetimeTime(double lifetimeTime) {
        if (lifetimeTime < 0) {
            throw new IllegalArgumentException("Lifetime time cannot be negative");
        }
        this.lifetimeTime = lifetimeTime;
    }

    public void setLongestSemester(Semester longestSemester) {
        this.longestSemester = longestSemester;
    }

    // Utility methods
    public void addTime(double timeToAdd) {
        if (timeToAdd > 0) {
            this.lifetimeTime += timeToAdd;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Account account = (Account) o;
        return Double.compare(account.lifetimeTime, lifetimeTime) == 0 &&
                Objects.equals(userID, account.userID) &&
                Objects.equals(longestSemester, account.longestSemester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, lifetimeTime, longestSemester);
    }

    @Override
    public String toString() {
        return String.format("Account{userID='%s', lifetimeTime=%.2f, longestSemester=%s}",
                userID, lifetimeTime, longestSemester);
    }
}