package org.bunnys.handler.lifecycle;

import org.bunnys.handler.Config;
import org.bunnys.handler.utils.handler.logging.Logger;

public class StartupLogger {
    public static void logStartupInfo(Config config) {
        Logger.debug(() -> "BunnyNexus Handler is starting... Shard Mode: " +
                (config.shardCount() > 0
                        ? "Manual (" + config.shardCount() +
                                (config.shardCount() > 1 ? " shards)" : " shard)")
                        : "Auto-sharding"));
    }
}
