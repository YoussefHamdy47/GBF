package org.bunnys.handler.utils;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.bunnys.utils.BunnyLog;

public class TokenLoader {
    private static Dotenv dotenv;

    private static void init() {
        if (dotenv == null) {
            try {
                dotenv = Dotenv.load();
            } catch (DotenvException e) {
                BunnyLog.warning("No .env file found. Searching system environment variables...");
                dotenv = Dotenv.configure().ignoreIfMissing().load();
            }
        }
    }

    public static String getEnv(String key) {
        init();
        String value = dotenv.get(key);

        if (value == null || value.trim().isEmpty()) {
            BunnyLog.warning("Environment variable '" + key + "' not found.");
            return null;
        }
        return value;
    }

    public static String getToken(String tokenKey) {
        init();

        String keyToSearch = (tokenKey != null && !tokenKey.isEmpty()) ? tokenKey : "DISCORD_TOKEN";

        String token = dotenv.get(keyToSearch);

        if (token == null || token.trim().isEmpty()) {
            BunnyLog.error("Failed to load token. Key '" + keyToSearch + "' not found.");
            throw new IllegalArgumentException("Missing Discord Token");
        }

        BunnyLog.info("Discord token retrieved successfully using key: " + keyToSearch);
        return token;
    }
}