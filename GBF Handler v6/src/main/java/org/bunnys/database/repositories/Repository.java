package org.bunnys.database.repositories;

import org.bson.Document;
import org.bunnys.database.entities.Entity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Repository<T extends Entity<ID>, ID> {
    // Create
    CompletableFuture<T> save(T entity);

    CompletableFuture<List<T>> saveAll(List<T> entities);

    // Read
    CompletableFuture<Optional<T>> findById(ID id);

    CompletableFuture<List<T>> findAll();

    CompletableFuture<List<T>> findBy(Document filter);

    CompletableFuture<Optional<T>> findOne(Document filter);

    CompletableFuture<Long> count();

    CompletableFuture<Long> count(Document filter);

    CompletableFuture<Boolean> exists(ID id);

    CompletableFuture<Boolean> exists(Document filter);

    // Update
    CompletableFuture<T> update(T entity);

    CompletableFuture<Long> updateMany(Document filter, Document update);

    CompletableFuture<Optional<T>> findAndUpdate(Document filter, Document update);

    // Delete
    CompletableFuture<Boolean> deleteById(ID id);

    CompletableFuture<Long> deleteMany(Document filter);

    CompletableFuture<Optional<T>> findAndDelete(Document filter);

    CompletableFuture<Void> deleteAll();
}