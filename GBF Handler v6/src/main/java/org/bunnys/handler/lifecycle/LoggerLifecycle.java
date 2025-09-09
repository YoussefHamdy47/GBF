package org.bunnys.handler.lifecycle;

import org.bunnys.handler.Config;
import org.bunnys.handler.utils.handler.logging.Logger;

public class LoggerLifecycle {
    public static void attachLogger(Config config) {
        Logger.attachConfig(config);
    }
}
