package org.bunnys.database.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bunnys.database.entities.Entity;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.eq;

public abstract class BaseMongoRepository<T extends Entity<String>> implements Repository<T, String> {
    protected final MongoCollection<Document> collection;
    protected final String collectionName;

    protected BaseMongoRepository(MongoDatabase database, String collectionName) {
        this.collectionName = collectionName;
        this.collection = database.getCollection(collectionName);
    }

    protected abstract T createEntity();

    // Helper method to run operations asynchronously
    protected <R> CompletableFuture<R> runAsync(Supplier<R> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.get();
            } catch (Exception e) {
                Logger.error("Database operation failed in " + collectionName + ": " + e.getMessage());
                throw new RuntimeException("Database operation failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<T> save(T entity) {
        return runAsync(() -> {
            entity.validate();

            Document doc = entity.toDocument();

            if (entity.getId() == null) {
                // Insert new document
                collection.insertOne(doc);
                ObjectId insertedId = doc.getObjectId("_id");
                entity.setId(insertedId.toHexString());
            } else {
                // Replace existing document
                ObjectId objectId = new ObjectId(entity.getId());
                ReplaceOptions options = new ReplaceOptions().upsert(true);
                collection.replaceOne(eq("_id", objectId), doc, options);
            }

            Logger.debug(() -> "Saved entity to " + collectionName + ": " + entity.getId());
            return entity;
        });
    }

    @Override
    public CompletableFuture<List<T>> saveAll(List<T> entities) {
        return runAsync(() -> {
            entities.forEach(Entity::validate);

            List<Document> documents = new ArrayList<>();
            for (T entity : entities) {
                documents.add(entity.toDocument());
            }

            collection.insertMany(documents);

            // Set IDs for new entities
            for (int i = 0; i < entities.size(); i++) {
                if (entities.get(i).getId() == null) {
                    ObjectId insertedId = documents.get(i).getObjectId("_id");
                    entities.get(i).setId(insertedId.toHexString());
                }
            }

            Logger.debug(() -> "Saved " + entities.size() + " entities to " + collectionName);
            return entities;
        });
    }

    @Override
    public CompletableFuture<Optional<T>> findById(String id) {
        return runAsync(() -> {
            Document doc = collection.find(eq("_id", id)).first(); // no ObjectId conversion
            if (doc == null)
                return Optional.empty();
            T entity = createEntity();
            entity.fromDocument(doc);
            return Optional.of(entity);
        });
    }

    @Override
    public CompletableFuture<List<T>> findAll() {
        return runAsync(() -> {
            List<T> results = new ArrayList<>();

            for (Document doc : collection.find()) {
                T entity = createEntity();
                entity.fromDocument(doc);
                results.add(entity);
            }

            return results;
        });
    }

    @Override
    public CompletableFuture<List<T>> findBy(Document filter) {
        return runAsync(() -> {
            List<T> results = new ArrayList<>();

            for (Document doc : collection.find(filter)) {
                T entity = createEntity();
                entity.fromDocument(doc);
                results.add(entity);
            }

            return results;
        });
    }

    @Override
    public CompletableFuture<Optional<T>> findOne(Document filter) {
        return runAsync(() -> {
            Document doc = collection.find(filter).first();

            if (doc == null) {
                return Optional.empty();
            }

            T entity = createEntity();
            entity.fromDocument(doc);
            return Optional.of(entity);
        });
    }

    @Override
    public CompletableFuture<Long> count() {
        return runAsync(collection::countDocuments);
    }

    @Override
    public CompletableFuture<Long> count(Document filter) {
        return runAsync(() -> collection.countDocuments(filter));
    }

    @Override
    public CompletableFuture<Boolean> exists(String id) {
        return runAsync(() -> {
            ObjectId objectId = new ObjectId(id);
            return collection.countDocuments(eq("_id", objectId)) > 0;
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Document filter) {
        return runAsync(() -> collection.countDocuments(filter) > 0);
    }

    @Override
    public CompletableFuture<T> update(T entity) {
        return save(entity); // Same as save for MongoDB
    }

    @Override
    public CompletableFuture<Long> updateMany(Document filter, Document update) {
        return runAsync(() -> {
            UpdateResult result = collection.updateMany(filter, update);
            Logger.debug(() -> "Updated " + result.getModifiedCount() + " documents in " + collectionName);
            return result.getModifiedCount();
        });
    }

    @Override
    public CompletableFuture<Optional<T>> findAndUpdate(Document filter, Document update) {
        return runAsync(() -> {
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER);

            Document doc = collection.findOneAndUpdate(filter, update, options);

            if (doc == null) {
                return Optional.empty();
            }

            T entity = createEntity();
            entity.fromDocument(doc);
            return Optional.of(entity);
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteById(String id) {
        return runAsync(() -> {
            ObjectId objectId = new ObjectId(id);
            DeleteResult result = collection.deleteOne(eq("_id", objectId));
            boolean deleted = result.getDeletedCount() > 0;

            if (deleted) {
                Logger.debug(() -> "Deleted entity from " + collectionName + ": " + id);
            }

            return deleted;
        });
    }

    @Override
    public CompletableFuture<Long> deleteMany(Document filter) {
        return runAsync(() -> {
            DeleteResult result = collection.deleteMany(filter);
            Logger.debug(() -> "Deleted " + result.getDeletedCount() + " documents from " + collectionName);
            return result.getDeletedCount();
        });
    }

    @Override
    public CompletableFuture<Optional<T>> findAndDelete(Document filter) {
        return runAsync(() -> {
            Document doc = collection.findOneAndDelete(filter);

            if (doc == null) {
                return Optional.empty();
            }

            T entity = createEntity();
            entity.fromDocument(doc);
            return Optional.of(entity);
        });
    }

    @Override
    public CompletableFuture<Void> deleteAll() {
        return runAsync(() -> {
            DeleteResult result = collection.deleteMany(new Document());
            Logger.debug(() -> "Deleted all " + result.getDeletedCount() + " documents from " + collectionName);
            return null;
        });
    }
}