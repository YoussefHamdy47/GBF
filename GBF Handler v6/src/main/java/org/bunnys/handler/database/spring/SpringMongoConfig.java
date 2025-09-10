package org.bunnys.handler.database.spring;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bunnys.handler.database.configs.MongoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class SpringMongoConfig {

    @Autowired
    private MongoConfig config;

    @Bean
    public MongoClient mongoClient() {
        String mongoUri = config.URI();
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        String databaseName = config.databaseName();
        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalStateException("Database name must be specified in MongoConfig");
        }
        return new MongoTemplate(mongoClient, databaseName);
    }
}