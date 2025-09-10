package org.bunnys;

import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.Config;
import org.bunnys.handler.database.configs.MongoConfig;
import org.bunnys.handler.utils.handler.EnvLoader;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bunnys.handler.utils.handler.IntentHandler;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "org.bunnys.database.repositories")
public class BotConfig {

    @Bean
    public Config config() {
        return Config.builder()
                .version("4.0.0")
                .debug(true)
                .intents(IntentHandler.fromRaw(GatewayIntent.ALL_INTENTS))
                .prefix(EnvLoader.get("PREFIX"))
                .developers(EnvLoader.get("DEVELOPERS").split(","))
                .testServers(EnvLoader.get("TEST_SERVERS").split(","))
                .eventsPackage("org.bunnys.events")
                .commandsPackage("org.bunnys.commands")
                .connectToDatabase(true)
                .mongo("MongoURI", "GBF")
                .build();
    }

    @Bean
    public MongoConfig mongoConfig() {
        return MongoConfig.builder()
                .envKey("MongoURI")
                .databaseName("GBF")
                .build();
    }

    @Bean
    public BunnyNexus bunnyNexus(Config config, ApplicationContext applicationContext,
            AutowireCapableBeanFactory beanFactory) {
        return new BunnyNexus(config, applicationContext, beanFactory);
    }
}