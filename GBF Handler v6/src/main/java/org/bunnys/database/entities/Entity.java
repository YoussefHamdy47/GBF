package org.bunnys.database.entities;

import org.bson.Document;

public interface Entity<ID> {
    ID getId();

    void setId(ID id);

    Document toDocument();

    void fromDocument(Document document);

    void validate();
}
