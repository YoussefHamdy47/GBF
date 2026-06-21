package org.bunnys.handler.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

public class DB {
    private static MongoManager manager;

    public static void init(MongoManager mongoManager) {
        manager = mongoManager;
    }

    private static void checkConnection() {
        if (manager == null || manager.getDatabase() == null)
            throw new IllegalStateException(
                    "[Database] Critical: Attempted an operation before MongoDB was initialized!");
    }

    public static <T> MongoCollection<T> getCollection(Class<T> clazz, String collectionName) {
        checkConnection();
        return manager.getCollection(clazz, collectionName);
    }

    /**
     * Find a single document by its explicit String ID.
     * Mongoose: Model.findById(id)
     */
    public static <T> T findById(Class<T> clazz, String collectionName, String id) {
        checkConnection();
        return getCollection(clazz, collectionName).find(Filters.eq("_id", id)).first();
    }

    /**
     * Find a single document matching a custom Bson filter queries.
     * Mongoose: Model.findOne({ status: "active" })
     */
    public static <T> T findOne(Class<T> clazz, String collectionName, Bson filter) {
        checkConnection();
        return getCollection(clazz, collectionName).find(filter).first();
    }

    /**
     * Find multiple documents matching a custom filter queries.
     * Mongoose: Model.find({ type: "timer" })
     */
    public static <T> List<T> findMany(Class<T> clazz, String collectionName, Bson filter) {
        checkConnection();
        List<T> results = new ArrayList<>();
        getCollection(clazz, collectionName).find(filter).into(results);
        return results;
    }

    /**
     * Saves or Completely Replaces a POJO document by ID (Upsert).
     * Mongoose: doc.save()
     */
    public static <T> void save(Class<T> clazz, String collectionName, String id, T document) {
        checkConnection();
        getCollection(clazz, collectionName).replaceOne(
                Filters.eq("_id", id),
                document,
                new ReplaceOptions().upsert(true));
    }

    /**
     * Saves or Completely Replaces a POJO document using a custom Bson filter
     * (Upsert).
     * Mongoose: Model.updateOne({ userID: "123" }, doc, { upsert: true })
     */
    public static <T> void save(Class<T> clazz, String collectionName, Bson filter, T document) {
        checkConnection();
        getCollection(clazz, collectionName).replaceOne(
                filter,
                document,
                new ReplaceOptions().upsert(true));
    }

    /**
     * Delete a single document by its unique ID.
     */
    public static <T> void deleteById(Class<T> clazz, String collectionName, String id) {
        checkConnection();
        getCollection(clazz, collectionName).deleteOne(Filters.eq("_id", id));
    }

    /**
     * Delete multiple documents matching a filter statement.
     * Mongoose: Model.deleteMany({ expired: true })
     */
    public static <T> long deleteMany(Class<T> clazz, String collectionName, Bson filter) {
        checkConnection();
        return getCollection(clazz, collectionName).deleteMany(filter).getDeletedCount();
    }
}