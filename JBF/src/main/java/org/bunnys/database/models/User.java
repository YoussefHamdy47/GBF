package org.bunnys.database.models;

import org.bson.Document;
import org.bunnys.database.entities.BaseEntity;

public class User extends BaseEntity {
    private String userId;
    private String username;

    public User() {
    }

    public User(String userId, String username) {
        this.userId = userId;
        this.username = username;
        validate();
    }

    @Override
    public Document toDocument() {
        Document doc = super.toDocument();
        doc.put("userId", userId);
        doc.put("username", username);
        return doc;
    }

    @Override
    public void fromDocument(Document document) {
        super.fromDocument(document);
        this.userId = document.getString("userId");
        this.username = document.getString("username");
    }

    @Override
    public void validate() {
        if (userId == null || userId.trim().isEmpty())
            throw new IllegalArgumentException("User ID cannot be null or empty");
        if (username == null || username.trim().isEmpty())
            throw new IllegalArgumentException("Username cannot be null or empty");
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
