package org.bunnys.database;

import com.mongodb.client.MongoDatabase;
import org.bunnys.database.models.User;
import org.bunnys.database.repositories.MongoRepository;

public class RepositoryFactory {
    private final MongoDatabase db;

    public RepositoryFactory(MongoDatabase db) {
        this.db = db;
    }

    public MongoRepository<User> users() {
        return new MongoRepository<>(db, "users", User::new);
    }
}
