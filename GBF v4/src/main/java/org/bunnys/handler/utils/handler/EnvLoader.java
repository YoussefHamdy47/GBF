package org.bunnys.handler.utils.handler;

import org.bunnys.handler.utils.handler.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class EnvLoader {
    private static final HashMap<String, String> envVars = new HashMap<>();

    static {
        try (InputStream inputStream = EnvLoader.class.getClassLoader()
                .getResourceAsStream(".env")) {
            if (inputStream == null)
                throw new RuntimeException("No config file found");

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("=", 2);

                if (parts.length == 2)
                    envVars.put(parts[0].trim(), parts[1].trim());
            }
        } catch (Exception err) {
            Logger.error("Error loading .env file\n" + err.getMessage());
        }
    }

    public static String get(String key) {
        return envVars.get(key);
    }
}