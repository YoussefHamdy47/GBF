package org.bunnys.database.entities;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Objects;

public abstract class BaseEntity implements Entity<String> {
    protected String id;
    protected Instant createdAt;
    protected Instant updatedAt;

    protected BaseEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @Override
    public String getId() { return id; }

    @Override
    public void setId(String id) { this.id = id; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    protected void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    @Override
    public Document toDocument() {
        Document doc = new Document();
        if (id != null) doc.put("_id", new ObjectId(id));
        doc.put("createdAt", createdAt);
        doc.put("updatedAt", updatedAt);
        return doc;
    }

    @Override
    public void fromDocument(Document document) {
        ObjectId objectId = document.getObjectId("_id");
        this.id = objectId != null ? objectId.toHexString() : null;
        this.createdAt = document.get("createdAt", Instant.now());
        this.updatedAt = document.get("updatedAt", Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
