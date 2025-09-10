package org.bunnys.handler.lifecycle;

import net.dv8tion.jda.api.sharding.ShardManager;
import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.events.EventLoader;
import org.bunnys.handler.events.EventRegistry;
import org.bunnys.handler.events.defaults.DefaultEvents;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the centralized lifecycle for loading and registering event handlers
 *
 * <p>
 * This class orchestrates the discovery, instantiation, and registration
 * of all bot events from three distinct sources: core events, default built-in
 * events, and developer-provided custom events. It provides a single, cohesive
 * entry point for preparing all event listeners during the bot's startup
 * process
 * </p>
 */
public final class EventLifecycle {

    /** The package containing mandatory, core events */
    private static final String CORE_EVENTS_PACKAGE = "org.bunnys.handler.events.core";

    private EventLifecycle() {
        // utility
    }

    /**
     * Loads and registers all events from all available sources
     *
     * <p>
     * This public method serves as the main entry point for the event lifecycle.
     * It loads events from core, default, and custom packages, adds them to an
     * {@link EventRegistry}, and then registers them with the provided
     * {@link ShardManager}. If no events are found, it logs a warning and
     * gracefully skips registration
     * </p>
     *
     * @param config       The bot's configuration
     * @param bunnyNexus   The central {@link BunnyNexus} client instance
     * @param shardManager The {@link ShardManager} to which the events will be
     *                     registered
     */
    public static void loadAndRegisterEvents(Config config, BunnyNexus bunnyNexus, ShardManager shardManager) {
        EventRegistry registry = new EventRegistry(bunnyNexus);
        bunnyNexus.setEventRegistry(registry);

        // Load from all sources
        List<Event> coreEvents = loadCoreEvents(bunnyNexus);
        List<Event> defaultEvents = loadDefaultEvents(bunnyNexus, config);
        List<Event> customEvents = loadCustomEvents(bunnyNexus, config);

        coreEvents.forEach(registry::add);
        defaultEvents.forEach(registry::add);
        customEvents.forEach(registry::add);

        if (registry.isEmpty()) {
            Logger.warning("[BunnyNexus] No events were loaded; skipping registration");
            return;
        }

        registry.registerAll(shardManager);
        Logger.success("[BunnyNexus] Loaded " + registry.size() + " event" +
                (registry.size() == 1 ? "" : "s"));
    }

    /**
     * Loads mandatory core events
     *
     * <p>
     * These events are essential for the bot's operation and are always loaded,
     * regardless of configuration settings. The method logs debug information if
     * debugging is enabled in the config
     * </p>
     *
     * @param bunnyNexus The central {@link BunnyNexus} client instance
     * @return A list of instantiated core events
     */
    private static List<Event> loadCoreEvents(BunnyNexus bunnyNexus) {
        List<Event> events = new EventLoader(CORE_EVENTS_PACKAGE, bunnyNexus).loadEvents();
        logEvents(events, "core");
        return events;
    }

    /**
     * Loads built-in default events
     *
     * <p>
     * This method loads events defined in the {@link DefaultEvents} enumeration,
     * respecting the `disabledDefaults` list in the configuration. It gracefully
     * handles events that are configured to be disabled
     * </p>
     *
     * @param bunnyNexus The central {@link BunnyNexus} client instance
     * @param config     The bot's configuration
     * @return A list of instantiated default events that are not disabled
     */
    private static List<Event> loadDefaultEvents(BunnyNexus bunnyNexus, Config config) {
        if (config.disabledDefaults().contains(DefaultEvents.ALL)) {
            return Collections.emptyList();
        }

        return java.util.Arrays.stream(DefaultEvents.values())
                .filter(def -> def != DefaultEvents.ALL)
                .filter(def -> !config.disabledDefaults().contains(def))
                .map(def -> {
                    try {
                        Event e = def.create(bunnyNexus);
                        Logger.debug(() -> "[BunnyNexus] Registered default event: " + def.name());
                        return e;
                    } catch (Exception ex) {
                        Logger.error("[BunnyNexus] Failed to initialize default event: " + def.name(), ex);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Loads developer-specified custom events
     *
     * <p>
     * This method loads events from the custom events package specified in the
     * configuration. It returns an empty list if no custom package is provided,
     * gracefully handling an optional component of the bot's setup
     * </p>
     *
     * @param bunnyNexus The central {@link BunnyNexus} client instance
     * @param config     The bot's configuration
     * @return A list of instantiated custom events
     */
    private static List<Event> loadCustomEvents(BunnyNexus bunnyNexus, Config config) {
        String pkg = config.eventsPackage();
        if (pkg == null || pkg.isBlank()) {
            Logger.debug(() -> "[BunnyNexus] No custom events package specified");
            return Collections.emptyList();
        }

        List<Event> events = new EventLoader(pkg, bunnyNexus).loadEvents();
        logEvents(events, "custom");
        return events;
    }

    /**
     * A shared utility method for logging the registration of events
     *
     * <p>
     * This helper method provides consistent logging for all event types
     * after they have been successfully loaded from their respective sources
     * </p>
     *
     * @param events The list of events that were loaded
     * @param label  A descriptive label for the event type (e.g., "core", "custom")
     */
    private static void logEvents(List<Event> events, String label) {
        if (events.isEmpty())
            return;

        events.forEach(e -> Logger
                .debug(() -> "[BunnyNexus] Registered " + label + " event: " + e.getClass().getSimpleName()));

        Logger.info("[BunnyNexus] Loaded " + events.size() + " " + label + " event" +
                (events.size() == 1 ? "" : "s"));
    }
}