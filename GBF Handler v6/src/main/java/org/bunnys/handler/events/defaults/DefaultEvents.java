package org.bunnys.handler.events.defaults;

import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.Event;

import java.util.function.Function;

/**
 * An enumeration of default bot events
 *
 * <p>
 * This enum provides a centralized and type-safe way to define and instantiate
 * the core events that the bot needs to function, such as handling bot
 * readiness
 * and user interactions
 * </p>
 */
public enum DefaultEvents {
    CLIENT_READY(ClientReady::new),
    /**
     * A special entry representing all events
     * <p>
     * This is not a real event and cannot be instantiated.
     * It is used for
     * configuration and logical purposes only
     * </p>
     */
    ALL(client -> {
        throw new UnsupportedOperationException("ALL is not instantiable");
    });

    /** A factory function to create a new instance of the event */
    private final Function<BunnyNexus, Event> factory;

    /**
     * Constructs a {@code DefaultEvents} enum entry
     *
     * @param factory The factory function to create the event instance
     */
    DefaultEvents(Function<BunnyNexus, Event> factory) {
        this.factory = factory;
    }

    /**
     * Creates a new instance of the event associated with this enum entry
     *
     * @param client The {@link BunnyNexus} client instance to be passed to the
     *               event's constructor
     * @return A new {@link Event} instance
     * @throws UnsupportedOperationException If this method is called on the
     *                                       {@code ALL} entry
     */
    public Event create(BunnyNexus client) {
        if (this == ALL)
            throw new UnsupportedOperationException("Cannot create event for ALL");
        return this.factory.apply(client);
    }
}