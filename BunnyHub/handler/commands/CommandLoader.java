package org.bunnys.handler.commands;

import org.bunnys.handler.BunnyHub;
import org.bunnys.utils.BunnyLog;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class CommandLoader {

    public static void loadCommands(BunnyHub client, CommandRegistry registry, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            BunnyLog.warning("No command package specified. Skipping command auto-loading.");
            return;
        }

        BunnyLog.info("Scanning command package: " + packageName + "...");

        // Constrained classpath scan
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackage(packageName)
                .filterInputsBy(new FilterBuilder().includePackage(packageName))
                .setScanners(Scanners.SubTypes));

        Set<Class<? extends BunnyCommand>> commandClasses = reflections.getSubTypesOf(BunnyCommand.class);

        int loadedCount = 0;

        for (Class<? extends BunnyCommand> clazz : commandClasses) {
            try {
                BunnyCommand cmdInstance = clazz.getDeclaredConstructor(BunnyHub.class).newInstance(client);
                registry.registerCommand(cmdInstance);
                loadedCount++;

            } catch (NoSuchMethodException e) {
                BunnyLog.warning("Class '" + clazz.getSimpleName() + "' lacks the BunnyHub constructor. Ignored.");
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                BunnyLog.error(
                        "Failed to compile/initialize command: " + clazz.getSimpleName() + "\n" + e.getMessage());
            }
        }

        BunnyLog.success("Successfully loaded " + loadedCount + " commands into memory.");
    }
}