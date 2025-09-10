package org.bunnys.database.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String ID;

    @NotNull
    @NotBlank
    @Indexed(unique = true)
    private String userID;

    @NotNull
    @NotBlank
    private String username;

    // Optional fields
    private String messageID;
    private String guildID;

    public User() {
    }

    public User(String userID, String username) {
        this.userID = userID;
        this.username = username;
    }

    public String getId() {
        return ID;
    }

    public void setId(String id) {
        this.ID = id;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getGuildID() {
        return guildID;
    }

    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + ID + '\'' +
                ", userID='" + userID + '\'' +
                ", username='" + username + '\'' +
                ", guildID='" + guildID + '\'' +
                '}';
    }
}