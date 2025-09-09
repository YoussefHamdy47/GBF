package org.bunnys;

import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.utils.handler.EnvLoader;
import org.bunnys.handler.utils.handler.IntentHandler;

@SuppressWarnings("unused")
public class Main {
    public static void main(String[] args) {
        Config config = Config.builder()
                .version("2.0.0")
                .debug(true)
                .intents(IntentHandler.fromRaw(GatewayIntent.ALL_INTENTS))
                .prefix(EnvLoader.get("PREFIX"))
                .developers(EnvLoader.get("DEVELOPERS").split(","))
                .testServers(EnvLoader.get("TEST_SERVERS").split(","))
                .eventsPackage("org.bunnys.events")
                .commandsPackage("org.bunnys.commands")
                .connectToDatabase(true)
                .mongo("MongoURI")
                .build();

        BunnyNexus client = new BunnyNexus(config);
    }
}