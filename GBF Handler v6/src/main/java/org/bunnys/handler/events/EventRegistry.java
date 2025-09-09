package org.bunnys.handler.events;

import net.dv8tion.jda.api.sharding.ShardManager;
import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe registry for managing and registering event handlers
 * <p>
 * This class serves as a central hub for storing {@link Event} instances
 * It provides methods for adding events, checking their status, and, most
 * importantly,
 * registering them with a JDA {@link ShardManager}
 * The registry handles two types of events: those that implement JDA's
 * {@link net.dv8tion.jda.api.hooks.EventListener} for global registration and
 * those
 * that require per-shard registration via their custom {@code register} method
 * </p>
 *
 * @author Bunny
 */
public final class EventRegistry {
    private final ConcurrentHashMap<String, Event> events = new ConcurrentHashMap<>();
    private final Config config;
    private final Set<String> globalListeners = ConcurrentHashMap.newKeySet();

    /**
     * Constructs an {@code EventRegistry}
     *
     * @param client The {@link BunnyNexus} client instance, used for configuration
     *               access
     */
    public EventRegistry(BunnyNexus client) {
        this.config = client.getConfig();
    }

    /**
     * Adds an event to the registry
     * <p>
     * This operation is thread-safe If an event with the same class name is already
     * present, it will be overridden, and a warning will be logged
     * </p>
     *
     * @param event The {@link Event} instance to add
     */
    public void add(Event event) {
        String eventName = event.getClass().getName();
        Event previous = this.events.put(eventName, event);
        if (previous != null)
            Logger.warning("[EventRegistry] Overriding existing event: " + eventName);
        else
            Logger.debug(() -> "[EventRegistry] Added event: " + eventName);
    }

    /**
     * Checks if the registry is empty
     *
     * @return {@code true} if no events are registered, otherwise {@code false}
     */
    public boolean isEmpty() {
        return this.events.isEmpty();
    }

    /**
     * Returns the number of events currently in the registry
     *
     * @return The number of registered events
     */
    public int size() {
        return this.events.size();
    }

    /**
     * Returns an unmodifiable snapshot of the events currently in the registry
     *
     * @return A {@code Collection} of registered {@link Event} instances
     */
    public Collection<Event> snapshot() {
        return List.copyOf(this.events.values());
    }

    /**
     * Registers all managed events with the provided JDA {@link ShardManager}
     * <p>
     * This method iterates through the event snapshot and intelligently registers
     * them
     * Events that implement JDA's {@code EventListener} are registered once
     * globally
     * per shard manager Others are registered per-shard by invoking their custom
     * {@code register} method
     * All registration is handled with robust error logging to ensure that a
     * single failure does not prevent other events from being registered
     * </p>
     *
     * @param shardManager The {@link ShardManager} instance to register events on
     */
    public void registerAll(ShardManager shardManager) {
        final var shards = List.copyOf(shardManager.getShards());
        final boolean dbg = config.debug();

        if (dbg) {
            Logger.info("[EventRegistry] Registering " + size() + " event"
                    + (size() > 1 ? "s" : "") + " across " + shards.size() + " shard"
                    + (shards.size() > 1 ? "s" : "") + "...");
        }

        for (Event event : snapshot()) {
            try {
                if (event instanceof net.dv8tion.jda.api.hooks.EventListener listener) {
                    if (this.globalListeners.add(event.getClass().getName())) {
                        shardManager.addEventListener(listener);
                        if (dbg)
                            Logger.info("[EventRegistry] Registered globally: " + event.getClass().getName());
                    }
                } else {
                    for (var jda : shards) {
                        try {
                            event.register(jda);
                            if (dbg)
                                Logger.debug(() -> "[EventRegistry] Registered " + event.getClass().getName()
                                        + " on shard " + jda.getShardInfo());
                        } catch (Exception e) {
                            if (dbg)
                                Logger.error("[EventRegistry] Failed to register "
                                        + event.getClass().getName() + " on shard " + jda.getShardInfo(), e);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error("[EventRegistry] Registration error for " + event.getClass().getName(), e);
            }
        }

        if (dbg)
            Logger.success("[EventRegistry] Registration complete");
        Logger.debug(() -> "[EventRegistry] Final event count: " + size());
    }
}