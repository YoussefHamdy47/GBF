package org.bunnys.handler.lifecycle;

import net.dv8tion.jda.api.sharding.ShardManager;
import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.events.EventLoader;
import org.bunnys.handler.events.EventRegistry;
import org.bunnys.handler.events.defaults.DefaultEvents;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.handler.logging.Logger;

/**
 * A static utility class responsible for orchestrating the complete lifecycle
 * of event management
 * <p>
 * This includes the discovery, instantiation, internal registration, and
 * deployment of both
 * custom and default event handlers to the JDA {@link ShardManager}.
 * It provides a single,
 * robust entry point for preparing all event listeners during application
 * startup.
 * </p>
 *
 * @author Bunny
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class EventLifecycle {

    /**
     * Loads and registers all custom and default event handlers
     * <p>
     * This method orchestrates the event loading process in two main phases:
     * <ol>
     * <li><b>Custom Event Loading:</b> If a custom events package is configured, it
     * uses an
     * {@link EventLoader} to dynamically discover and instantiate all custom event
     * handlers,
     * which are then added to the {@link EventRegistry}</li>
     * <li><b>Default Event Loading:</b> It iterates through the pre-defined set of
     * {@link DefaultEvents} and registers any that have not been explicitly
     * disabled in the
     * application's configuration</li>
     * </ol>
     * After loading, all collected events are registered with the provided
     * {@link ShardManager}.
     * The process is designed to be resilient, logging errors for any failing
     * events and continuing
     * with the next one
     * </p>
     *
     * @param config       The application's {@link Config} instance, which dictates
     *                     which events to load and enable
     * @param bunnyNexus   The main bot client instance, used for dependency
     *                     injection and logging
     * @param shardManager The {@link ShardManager} instance with which to register
     *                     the loaded events
     */
    public static void loadAndRegisterEvents(Config config, BunnyNexus bunnyNexus, ShardManager shardManager) {
        EventRegistry registry = new EventRegistry(bunnyNexus);

        if (config.debug())
            Logger.info("[BunnyNexus] Loading events from config package: " + config.eventsPackage());

        if (config.eventsPackage() != null && !config.eventsPackage().isBlank()) {
            EventLoader loader = new EventLoader(config.eventsPackage(), bunnyNexus);
            loader.loadEvents().forEach(registry::add);
        }

        if (config.debug())
            Logger.info("[BunnyNexus] Loading default events...");

        for (var def : DefaultEvents.values()) {
            if (config.disabledDefaults().contains(DefaultEvents.ALL)) {
                Logger.debug(() -> "[BunnyNexus] All default events are disabled.");
                break;
            }
            if (def == DefaultEvents.ALL)
                continue;

            if (!config.disabledDefaults().contains(def)) {
                try {
                    Event event = def.create(bunnyNexus);
                    registry.add(event);
                    Logger.debug(() -> "[BunnyNexus] Registered default event: " + def.name());
                } catch (Exception e) {
                    Logger.error("[BunnyNexus] Failed to initialize default event: " + def.name(), e);
                }
            } else
                Logger.debug(() -> "[BunnyNexus] Skipped disabled default event: " + def.name());
        }

        if (registry.isEmpty()) {
            Logger.warning("[BunnyNexus] No events were loaded; skipping registration.");
            return;
        }

        registry.registerAll(shardManager);
        Logger.success("[BunnyNexus] Loaded " + registry.size() + " event" +
                (registry.size() == 1 ? "" : "s") + ".");
    }
}