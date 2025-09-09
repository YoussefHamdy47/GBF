package org.bunnys.handler.lifecycle;

import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bunnys.handler.Config;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.EnumSet;

/**
 * A static utility class responsible for initializing a {@link ShardManager}.
 * <p>
 * This class encapsulates the complex process of configuring JDA's sharded
 * connection to Discord. It uses
 * an application's {@link Config} object to build a robust and efficient
 * {@link ShardManager}, ensuring
 * proper setup of gateway intents, cache policies, and shard counts. The
 * fail-fast design ensures that
 * any critical configuration error prevents the application from starting.
 * </p>
 *
 * @author Bunny
 */
public class ShardManagerInitializer {

    /**
     * Initializes and builds a new {@link ShardManager} instance based on the
     * provided configuration
     * <p>
     * This method serves as the primary entry point for establishing a sharded
     * connection to Discord
     * It configures a {@link DefaultShardManagerBuilder} using the application's
     * settings, logs the
     * initialization process, and returns a fully constructed {@link ShardManager}
     * </p>
     *
     * @param config The application's {@link Config} instance containing token,
     *               shard, and intent settings
     * @return A fully configured and built {@link ShardManager} object
     * @throws IllegalStateException If the {@code ShardManager} fails to initialize
     *                               due to an invalid token or
     *                               other critical configuration error
     */
    public static ShardManager initShardManager(Config config) {
        try {
            Logger.debug(() -> "Initializing ShardManager...");

            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(config.token());
            configureShards(builder, config);
            ShardManager sm = builder.build();

            if (config.debug())
                Logger.success("ShardManager initialized. Total shards (gateway): " + sm.getShardsTotal());

            return sm;
        } catch (Exception e) {
            Logger.error("Failed to initialize ShardManager: " + e.getMessage(), e);
            throw new IllegalStateException("Unable to initialize ShardManager", e);
        }
    }

    /**
     * Applies all configuration settings to the {@link DefaultShardManagerBuilder}
     * <p>
     * This private helper method contains the core logic for customizing the JDA
     * connection. It manages
     * shard count, enables specified gateway intents, and disables unnecessary
     * cache flags and policies
     * to optimize memory usage
     * </p>
     *
     * @param builder The {@link DefaultShardManagerBuilder} to be configured
     * @param config  The {@link Config} instance providing the configuration values
     */
    private static void configureShards(DefaultShardManagerBuilder builder, Config config) {
        if (config.shardCount() > 0) {
            builder.setShardsTotal(config.shardCount());
            Logger.debug(() -> "Using manual shard count: " + config.shardCount());
        } else
            Logger.debug(() -> "Using auto-sharding (JDA determines shard count)");

        if (!config.intents().isEmpty())
            builder.enableIntents(config.intents());

        if (!config.intents().contains(GatewayIntent.GUILD_MEMBERS)) {
            builder.setChunkingFilter(ChunkingFilter.NONE);
            builder.setMemberCachePolicy(MemberCachePolicy.NONE);
            Logger.debug(() -> "GUILD_MEMBERS intent not enabled. Disabling chunking and member cache.");
        }

        EnumSet<CacheFlag> toDisable = EnumSet.noneOf(CacheFlag.class);

        if (!config.intents().contains(GatewayIntent.GUILD_PRESENCES))
            toDisable.add(CacheFlag.ACTIVITY);

        if (!config.intents().contains(GatewayIntent.GUILD_VOICE_STATES))
            toDisable.add(CacheFlag.VOICE_STATE);

        if (!toDisable.isEmpty())
            builder.disableCache(toDisable);

        builder.setEventManagerProvider(shardId -> new net.dv8tion.jda.api.hooks.InterfacedEventManager());
    }
}