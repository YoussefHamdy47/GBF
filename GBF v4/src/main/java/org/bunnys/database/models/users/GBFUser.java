package org.bunnys.database.models.users;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Document(collection = "GBF Users")
public class GBFUser {

    @Id
    private String id;

    @Field("userID")
    private String userID;

    @NotNull
    @Min(0)
    @Field("Rank")
    private Integer rank = 0;

    @NotNull
    @Min(0)
    private Integer RP = 0;

    private boolean privateProfile = false;

    private List<String> friends = new ArrayList<>();

    @Field("Subjects")
    private List<Subject> subjects = new ArrayList<>();

    // ===== Inner Subject class =====
    public static class Subject {
        @NotBlank(message = "Subject name is required")
        @Size(max = 100, message = "Subject name too long")
        private String subjectName;

        @NotBlank(message = "Grade is required")
        private String grade;

        @NotBlank(message = "Subject code is required")
        private String subjectCode;

        @NotNull
        @Min(value = 1, message = "Credit hours must be at least 1")
        private Integer creditHours;

        public Subject() {
        }

        public Subject(String subjectName, String grade, String subjectCode, Integer creditHours) {
            this.subjectName = subjectName;
            this.grade = grade;
            this.subjectCode = subjectCode;
            this.creditHours = creditHours;
        }

        // Getters and Setters
        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public void setSubjectCode(String subjectCode) {
            this.subjectCode = subjectCode;
        }

        public Integer getCreditHours() {
            return creditHours;
        }

        public void setCreditHours(Integer creditHours) {
            this.creditHours = creditHours;
        }
    }

    public GBFUser() {
    }

    public GBFUser(String userID) {
        this.userID = userID;
    }

    // ===== Getters and Setters =====
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Integer getRP() {
        return RP;
    }

    public void setRP(Integer RP) {
        this.RP = RP;
    }

    public boolean isPrivateProfile() {
        return privateProfile;
    }

    public void setPrivateProfile(boolean privateProfile) {
        this.privateProfile = privateProfile;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }
}
