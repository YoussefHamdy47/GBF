package org.bunnys.handler.events.defaults;

import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.Event;

import java.util.function.Function;

public enum DefaultEvents {
    CLIENT_READY(ClientReady::new),
    MESSAGE_CREATE(MessageCreate::new),
    INTERACTION_CREATE(InteractionCreate::new),

    ALL(client -> {
        throw new UnsupportedOperationException("ALL is not instantiable");
    });

    private final Function<BunnyNexus, Event> factory;

    DefaultEvents(Function<BunnyNexus, Event> factory) {
        this.factory = factory;
    }

    public Event create(BunnyNexus client) {
        if (this == ALL)
            throw new UnsupportedOperationException("Cannot create event for ALL");
        return this.factory.apply(client);
    }
}
