package org.bunnys.database.models.timer;

import org.bunnys.executors.timer.engine.GradeEngine;
import java.util.Objects;

@SuppressWarnings("unused")
public class Subject {
    private String subjectName;
    private String subjectCode;
    private int timesStudied;
    private Integer creditHours;
    private GradeEngine.Grade grade;
    private int marksLost;

    public Subject() {
    }

    public Subject(String subjectName, String subjectCode) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.timesStudied = 0;
        this.marksLost = 0;
    }

    // Getters
    public String getSubjectName() {
        return subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public int getTimesStudied() {
        return timesStudied;
    }

    public Integer getCreditHours() {
        return creditHours;
    }

    public GradeEngine.Grade getGrade() {
        return grade;
    }

    public int getMarksLost() {
        return marksLost;
    }

    // Setters with validation
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public void setTimesStudied(int timesStudied) {
        if (timesStudied < 0) {
            throw new IllegalArgumentException("Times studied cannot be negative");
        }
        this.timesStudied = timesStudied;
    }

    public void setCreditHours(Integer creditHours) {
        if (creditHours != null && creditHours < 0) {
            throw new IllegalArgumentException("Credit hours cannot be negative");
        }
        this.creditHours = creditHours;
    }

    public void setGrade(GradeEngine.Grade grade) {
        this.grade = grade;
    }

    public void setMarksLost(int marksLost) {
        if (marksLost < 0) {
            throw new IllegalArgumentException("Marks lost cannot be negative");
        }
        this.marksLost = marksLost;
    }

    // Utility methods
    public void incrementTimesStudied() {
        this.timesStudied++;
    }

    public void addMarksLost(int marks) {
        if (marks > 0) {
            this.marksLost += marks;
        }
    }

    public boolean hasGrade() {
        return grade != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Subject subject = (Subject) o;
        return Objects.equals(subjectName, subject.subjectName) &&
                Objects.equals(subjectCode, subject.subjectCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectName, subjectCode);
    }

    @Override
    public String toString() {
        return String.format("Subject{name='%s', code='%s', studied=%d times, grade=%s}",
                subjectName, subjectCode, timesStudied, grade);
    }
}