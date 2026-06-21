package org.bunnys.handler.router.modals;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.bunnys.handler.BunnyHub;
import org.bunnys.utils.BunnyLog;
import org.bunnys.utils.AppDesign;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class ModalRouter {
    private static final Map<String, BunnyModal> MODALS = new HashMap<>();

    private ModalRouter() {}

    public static void loadModals(String packageName) {
        try {
            Reflections reflections = new Reflections(packageName);
            Set<Class<? extends BunnyModal>> classes = reflections.getSubTypesOf(BunnyModal.class);

            for (Class<? extends BunnyModal> clazz : classes) {
                try {
                    BunnyModal modal = clazz.getDeclaredConstructor().newInstance();
                    MODALS.put(modal.getPrefix().toLowerCase(), modal);
                } catch (Exception e) {
                    BunnyLog.error("[ModalRouter] Failed to instantiate modal: " + clazz.getName() + " - " + e.getMessage());
                }
            }
            BunnyLog.info("[ModalRouter] Successfully loaded " + MODALS.size() + " modals.");
        } catch (Exception e) {
            BunnyLog.error("[ModalRouter] Critical failure loading modals from package: " + packageName + " - " + e.getMessage());
        }
    }

    public static void handle(BunnyHub client, ModalInteractionEvent event) {
        try {
            String[] args = event.getModalId().split(":");
            BunnyModal modal = MODALS.get(args[0].toLowerCase());

            if (modal != null) {
                modal.execute(client, event, args);
            } else {
                // Prevent Discord timeout by actively rejecting unregistered modals
                BunnyLog.error("[ModalRouter] Unregistered modal triggered: " + args[0]);
                event.reply("> " + AppDesign.Emojis.ERROR + " **System Error:** Modal handler not found (`" + args[0] + "`).")
                        .setEphemeral(true)
                        .queue();
            }
        } catch (Exception e) {
            BunnyLog.error("[ModalRouter] Unhandled exception processing modal ID: " + event.getModalId() + " - " + e.getMessage());
            if (!event.isAcknowledged()) {
                event.reply("> " + AppDesign.Emojis.ERROR + " **Critical Error:** Something went wrong while processing this modal.")
                        .setEphemeral(true).queue();
            }
        }
    }
}