package org.bunnys.handler.router.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.bunnys.handler.BunnyHub;
import org.bunnys.utils.BunnyLog;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ButtonRouter {

    private static final Map<String, BunnyButton> HANDLERS = new ConcurrentHashMap<>();

    private ButtonRouter() {}

    public static void loadButtons(String packageName) {
        BunnyLog.info("Scanning button package: " + packageName + "...");

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackage(packageName)
                .filterInputsBy(new FilterBuilder().includePackage(packageName))
                .setScanners(Scanners.SubTypes));

        Set<Class<? extends BunnyButton>> buttonClasses = reflections.getSubTypesOf(BunnyButton.class);

        int loadedCount = 0;

        for (Class<? extends BunnyButton> clazz : buttonClasses) {
            try {
                BunnyButton buttonInstance = clazz.getDeclaredConstructor().newInstance();
                HANDLERS.put(buttonInstance.getPrefix(), buttonInstance);
                loadedCount++;
            } catch (Exception e) {
                BunnyLog.error("Failed to load button: " + clazz.getSimpleName() + "\n" + e.getMessage());
            }
        }

        BunnyLog.success("Successfully loaded " + loadedCount + " button handlers.");
    }

    public static void handle(BunnyHub client, ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.isBlank())
            return;

        String[] parts = componentId.split(":");

        if (parts.length == 0)
            return;

        String prefix = parts[0];
        BunnyButton handler = HANDLERS.get(prefix);

        if (handler == null)
            return;

        try {
            handler.execute(client, event, parts);
        } catch (Exception err) {
            BunnyLog.error("Button Interaction Exception (" + componentId + "): " + err.getMessage());
            err.printStackTrace();

            if (!event.isAcknowledged()) {
                event.reply("> ❌ **Action failed:** Something went wrong while processing this button.")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}