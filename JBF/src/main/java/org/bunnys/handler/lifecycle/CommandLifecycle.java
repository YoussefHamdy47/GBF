package org.bunnys.handler.lifecycle;

import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.CommandLoader;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.commands.message.MessageCommandConfig;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A static utility class responsible for orchestrating the complete lifecycle
 * of command management
 * <p>
 * This includes the discovery, instantiation, internal registration, and
 * external deployment
 * of both message-based and slash commands It serves as the primary entry point
 * for
 * preparing the command set during application startup
 * </p>
 *
 * @author Bunny
 */
public final class CommandLifecycle {

    /**
     * Discovers, loads, and registers all message and slash commands
     * <p>
     * This method performs the following steps in sequence:
     * <ol>
     * <li>Instantiates a {@link CommandLoader} to find and create command objects
     * from the configured package</li>
     * <li>Iterates through all discovered {@link MessageCommand} instances,
     * initializing them and registering
     * them with the provided {@link CommandRegistry}</li>
     * <li>Iterates through all discovered {@link SlashCommand} instances,
     * initializing them and registering
     * them with the registry</li>
     * <li>For slash commands, it further deploys them to Discord by updating all
     * guilds via the
     * {@link net.dv8tion.jda.api.sharding.ShardManager}</li>
     * </ol>
     * The process is designed to be resilient; if a command fails to initialize or
     * register,
     * the error is logged, and the process continues with the next command
     * </p>
     *
     * @param config     The application's {@link Config} instance, providing the
     *                   command package to scan
     * @param bunnyNexus The main bot client instance, used for JDA interaction and
     *                   dependency injection
     * @param registry   The {@link CommandRegistry} where the commands will be
     *                   stored for runtime access
     */
    public static void loadAndRegisterCommands(Config config, BunnyNexus bunnyNexus, CommandRegistry registry) {
        if (config.commandsPackage() == null || config.commandsPackage().isBlank())
            return;

        CommandLoader loader = new CommandLoader(config.commandsPackage(), bunnyNexus);

        // --- Load message commands ---
        List<MessageCommand> messageCommands = new ArrayList<>(loader.loadMessageCommands());

        int successMsg = 0, failedMsg = 0;
        for (MessageCommand cmd : messageCommands) {
            try {
                MessageCommandConfig cfg = cmd.initAndGetConfig();
                registry.registerMessageCommand(cmd, cfg);
                successMsg++;
            } catch (Throwable t) {
                failedMsg++;
                Logger.error("[BunnyNexus] Failed to register message command: " + cmd.getClass().getName(), t);
            }
        }
        int finalSuccessMsg = successMsg;
        int finalFailedMsg = failedMsg;
        Logger.debug(() -> String.format("[BunnyNexus] Loaded %d message command(s), %d failed to register",
                finalSuccessMsg, finalFailedMsg));

        // --- Load slash commands ---
        List<SlashCommand> slashCommands = new ArrayList<>(loader.loadSlashCommands());
        int successSlash = 0, failedSlash = 0;

        for (SlashCommand cmd : slashCommands) {
            try {
                SlashCommandConfig cfg = cmd.initAndGetConfig();
                registry.registerSlashCommand(cmd, cfg);
                successSlash++;
            } catch (Throwable t) {
                failedSlash++;
                Logger.error("[BunnyNexus] Failed to register slash command: " + cmd.getClass().getName(), t);
            }
        }
        int finalSuccessSlash = successSlash;
        int finalFailedSlash = failedSlash;
        Logger.debug(() -> String.format("[BunnyNexus] Loaded %d slash command(s), %d failed to register",
                finalSuccessSlash, finalFailedSlash));

        // --- Load context commands (User & Message) ---
        List<ContextCommand> contextCommands = new ArrayList<>(loader.loadContextCommands());
        int successContext = 0, failedContext = 0;

        for (ContextCommand cmd : contextCommands) {
            try {
                ContextCommandConfig cfg = cmd.initAndGetConfig();
                registry.registerContextCommand(cmd, cfg);
                successContext++;
            } catch (Throwable t) {
                failedContext++;
                Logger.error("[BunnyNexus] Failed to register context command: " + cmd.getClass().getName(), t);
            }
        }
        int finalSuccessContext = successContext;
        int finalFailedContext = failedContext;
        Logger.debug(() -> String.format(
                "[BunnyNexus] Loaded %d context command(s), %d failed to register",
                finalSuccessContext, finalFailedContext));

    }
}