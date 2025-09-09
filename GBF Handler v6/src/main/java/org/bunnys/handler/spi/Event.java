package org.bunnys.handler.spi;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;

/**
 * A Service Provider Interface (SPI) that defines the contract for all
 * dynamically loaded event handlers
 * <p>
 * Classes that implement this interface are designed to be discovered and
 * instantiated by the
 * {@link org.bunnys.handler.events.EventLoader} and registered with JDA This
 * contract provides
 * a clean separation between the event handler's core logic and the framework's
 * registration mechanism
 * </p>
 * <p>
 * This interface is distinct from JDA's built-in
 * {@link net.dv8tion.jda.api.hooks.EventListener}, as it
 * supports a custom registration method, allowing for greater flexibility, such
 * as per-shard logic
 * that cannot be handled by a global listener
 * </p>
 *
 * @author Bunny
 * @see org.bunnys.handler.events.EventLoader
 * @see org.bunnys.handler.events.EventRegistry
 */
@SuppressWarnings("unused")
public interface Event {

    /**
     * Registers the event handler with a specific {@link JDA} instance
     * <p>
     * This method is invoked by the event lifecycle manager for each shard,
     * providing
     * the implementor with a clean hook to register listeners or other JDA-specific
     * components tailored to that shard
     * </p>
     *
     * @param jda The {@link JDA} instance of a specific shard
     */
    void register(JDA jda);
}