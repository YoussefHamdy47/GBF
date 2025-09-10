package org.bunnys.handler.lifecycle;

import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.CommandLoader;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Manages the lifecycle of bot commands, including loading and registration
 *
 * <p>
 * This utility class is responsible for discovering, initializing, and
 * registering all command types (slash, message, and context commands) found
 * within a specified package. It ensures that commands are properly prepared
 * for use by the bot's {@link CommandRegistry}
 * </p>
 */
public final class CommandLifecycle {

    /**
     * Loads and registers all commands from the configured package
     *
     * <p>
     * This method orchestrates the entire command loading process. It uses a
     * {@link CommandLoader} to find command classes and then delegates the
     * registration of each command type to a private helper method. This
     * method is a no-op if the commands package is not specified in the
     * {@link Config}
     * </p>
     *
     * @param config             The bot's configuration, containing the commands
     *                           package
     * @param bunnyNexus         The central {@link BunnyNexus} client instance
     * @param registry           The {@link CommandRegistry} where commands will be
     *                           registered
     * @param applicationContext The Spring ApplicationContext for dependency
     *                           injection
     * @param beanFactory        The AutowireCapableBeanFactory for wiring
     *                           dependencies
     */
    public static void loadAndRegisterCommands(Config config, BunnyNexus bunnyNexus, CommandRegistry registry,
            ApplicationContext applicationContext, AutowireCapableBeanFactory beanFactory) {
        if (config.commandsPackage() == null || config.commandsPackage().isBlank())
            return;

        CommandLoader loader = new CommandLoader(config.commandsPackage(), bunnyNexus, applicationContext, beanFactory);

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

    /**
     * Generic helper method for registering a specific type of command
     *
     * <p>
     * This method abstracts the command registration logic, allowing it to be
     * reused for all command types. It handles the discovery of commands,
     * extracts their configuration, and registers them with the provided
     * {@link BiConsumer} while logging success and failure counts
     * </p>
     *
     * @param typeName        The user-friendly name for the command type (e.g.,
     *                        "slash")
     * @param loader          A {@link Supplier} that provides a list of command
     *                        instances
     * @param configExtractor A {@link Function} to extract the configuration from a
     *                        command instance
     * @param registrator     A {@link BiConsumer} that performs the actual
     *                        registration of the command
     *                        and its configuration
     * @param <T>             The type of the command class (e.g.,
     *                        {@link SlashCommand})
     * @param <C>             The type of the command's configuration (e.g.,
     *                        {@link org.bunnys.handler.commands.slash.SlashCommandConfig})
     */
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