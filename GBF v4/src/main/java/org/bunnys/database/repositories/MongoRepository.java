package org.bunnys.database.repositories;

import com.mongodb.client.MongoDatabase;
import org.bunnys.database.entities.Entity;

import java.util.function.Supplier;

public class MongoRepository<T extends Entity<String>> extends BaseMongoRepository<T> {
    private final Supplier<T> factory;

    public MongoRepository(MongoDatabase db, String collectionName, Supplier<T> factory) {
        super(db, collectionName);
        this.factory = factory;
    }

    @Override
    protected T createEntity() {
        return factory.get();
    }
}
