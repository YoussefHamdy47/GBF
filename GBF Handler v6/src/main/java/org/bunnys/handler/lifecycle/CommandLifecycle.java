package org.bunnys.handler.lifecycle;

import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.CommandLoader;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CommandLifecycle {

    public static void loadAndRegisterCommands(Config config, BunnyNexus bunnyNexus, CommandRegistry registry) {
        if (config.commandsPackage() == null || config.commandsPackage().isBlank())
            return;

        CommandLoader loader = new CommandLoader(config.commandsPackage(), bunnyNexus);

        registerCommandType(
                "message",
                loader::loadMessageCommands,
                MessageCommand::initAndGetConfig,
                registry::registerMessageCommand);

        registerCommandType(
                "slash",
                loader::loadSlashCommands,
                SlashCommand::initAndGetConfig,
                registry::registerSlashCommand);

        registerCommandType(
                "context",
                loader::loadContextCommands,
                ContextCommand::initAndGetConfig,
                registry::registerContextCommand);
    }

    private static <T, C> void registerCommandType(
            String typeName,
            Supplier<List<T>> loader,
            Function<T, C> configExtractor,
            BiConsumer<T, C> registrator) {

        List<T> commands = loader.get();
        int success = 0, failed = 0;

        for (T command : commands) {
            try {
                C config = configExtractor.apply(command);
                registrator.accept(command, config);
                success++;
            } catch (Throwable t) {
                failed++;
                Logger.error("[BunnyNexus] Failed to register " + typeName + " command: "
                        + command.getClass().getName(), t);
            }
        }

        final int finalSuccess = success;
        final int finalFailed = failed;
        Logger.debug(() -> String.format("[BunnyNexus] Loaded %d %s command(s), %d failed to register",
                finalSuccess, typeName, finalFailed));
    }
}