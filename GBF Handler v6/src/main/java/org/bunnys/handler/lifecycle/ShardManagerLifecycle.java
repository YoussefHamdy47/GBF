package org.bunnys.handler.lifecycle;

import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.utils.handler.logging.Logger;

/**
 * A static utility class managing the lifecycle of the JDA {@link net.dv8tion.jda.api.sharding.ShardManager}.
 * <p>
 * This class provides a centralized, robust API for key lifecycle operations, including graceful
 * shutdown and re-initialization of the shard manager. Its methods ensure that the transition
 * between states is handled safely, preventing resource leaks and ensuring a consistent state.
 * </p>
 *
 * @author Bunny
 */
public class ShardManagerLifecycle {

    /**
     * Gracefully shuts down the existing {@link net.dv8tion.jda.api.sharding.ShardManager} and
     * reconnects with a new token
     * <p>
     * This method orchestrates the full sequence required to replace a bot's connection token
     * without a full application restart. The process involves:
     * <ol>
     * <li>Logging a warning to alert of the impending state change</li>
     * <li>Initiating a graceful shutdown of the current shard manager via {@link #shutdown(BunnyNexus)}.</li>
     * <li>Updating the bot's configuration with the new token</li>
     * <li>Re-initializing a new shard manager using {@link ShardManagerInitializer#initShardManager(org.bunnys.handler.Config)}.</li>
     * <li>Re-registering all events with the new shard manager</li>
     * </ol>
     * This sequence ensures a seamless transition with minimal downtime
     * </p>
     *
     * @param bunnyNexus The main bot client instance whose shard manager will be updated
     * @param newToken   The new bot token to use for reconnection
     */
    public static void reconnect(BunnyNexus bunnyNexus, String newToken) {
        Logger.warning("Attempting to reconnect with a new token...");
        shutdown(bunnyNexus);
        bunnyNexus.updateToken(newToken);
        bunnyNexus.setShardManager(ShardManagerInitializer.initShardManager(bunnyNexus.getConfig()));
        EventLifecycle.loadAndRegisterEvents(bunnyNexus.getConfig(), bunnyNexus, bunnyNexus.getShardManager());
    }

    /**
     * Gracefully shuts down the current {@link net.dv8tion.jda.api.sharding.ShardManager}
     * <p>
     * This method attempts to perform a clean shutdown, allowing JDA to close all active gateway
     * connections and release resources. It includes robust error handling to log any issues
     * during the shutdown process, and it sets the shard manager reference to {@code null}
     * in a {@code finally} block to prevent dangling references
     * </p>
     *
     * @param bunnyNexus The main bot client instance whose shard manager will be shut down
     */
    public static void shutdown(BunnyNexus bunnyNexus) {
        if (bunnyNexus.getShardManager() != null) {
            try {
                bunnyNexus.getShardManager().shutdown();
                Logger.info("ShardManager shutdown complete.");
            } catch (Exception e) {
                Logger.error("Error during ShardManager shutdown: " + e.getMessage(), e);
            } finally {
                bunnyNexus.setShardManager(null);
            }
        } else
            Logger.debug(() -> "Shutdown requested but ShardManager was null.");
    }
}