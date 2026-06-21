package org.bunnys.handler.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bunnys.utils.BunnyLog;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoManager {
    private MongoClient client;
    private MongoDatabase database;
    private boolean connected = false;

    public MongoManager(String uri, String databaseName) {
        if (uri == null) {
            BunnyLog.error("[Database] No MongoDB URI provided.");
            return;
        }

        try {
            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            this.client = MongoClients.create(settings);
            this.database = this.client.getDatabase(databaseName);

            this.database.runCommand(new Document("ping", 1));
            this.connected = true;
            BunnyLog.success("[Database] Successfully connected to MongoDB cluster!");

        } catch (Exception e) {
            BunnyLog.error("[Database] Critical failure establishing connection: " + e.getMessage());
            this.connected = false;
        }
    }

    public <T> MongoCollection<T> getCollection(Class<T> clazz, String collectionName) {
        return database.getCollection(collectionName, clazz);
    }

    public MongoDatabase getDatabase() {
        return this.database;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void disconnect() {
        if (this.client != null) {
            this.client.close();
            BunnyLog.info("[Database] Connection pool closed cleanly.");
        }
    }
}