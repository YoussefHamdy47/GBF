package org.bunnys.handler.utils.handler;

import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Collection;
import java.util.EnumSet;

public class IntentHandler {
    /**
     * Convert a raw bitfield into an EnumSet of GatewayIntents
     */
    public static EnumSet<GatewayIntent> fromRaw(int bitfield) {
        EnumSet<GatewayIntent> enabled = EnumSet.noneOf(GatewayIntent.class);
        for (GatewayIntent intent : GatewayIntent.values())
            if ((bitfield & intent.getRawValue()) != 0)
                enabled.add(intent);
        return enabled;
    }

    /**
     * Wrap an existing EnumSet (or Collection) of GatewayIntents
     * This allows passing things like GatewayIntent.ALL_INTENTS directly
     */
    public static EnumSet<GatewayIntent> fromRaw(java.util.Collection<GatewayIntent> intents) {
        return intents.isEmpty() ? EnumSet.noneOf(GatewayIntent.class) : EnumSet.copyOf(intents);
    }

    /**
     * Convert an EnumSet of GatewayIntents into a raw bitfield
     */
    public static int toRaw(Collection<GatewayIntent> intents) {
        int bitfield = 0;
        for (GatewayIntent intent : intents)
            bitfield |= intent.getRawValue();
        return bitfield;
    }
}
