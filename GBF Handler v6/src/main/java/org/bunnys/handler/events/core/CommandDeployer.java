package org.bunnys.handler.events.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.events.defaults.DefaultEvents;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the deployment of all bot commands to Discord
 *
 * <p>
 * This utility class is responsible for retrieving command data from the
 * {@link CommandRegistry}, partitioning them into global and guild-specific
 * commands, and then deploying them to Discord via the {@link ShardManager}.
 * It handles both slash commands and context menu commands
 * </p>
 */
@SuppressWarnings("unused")
public final class CommandDeployer extends ListenerAdapter implements Event {
    /** The central client instance */
    private final BunnyNexus client;

    /**
     * Constructs the command deployer
     *
     * @param client The {@link BunnyNexus} client instance, cannot be null
     */
    public CommandDeployer(BunnyNexus client) {
        this.client = Objects.requireNonNull(client);
    }

    /**
     * This event handler does not need to register with JDA directly
     * as its functionality is a one-time operation during startup
     *
     * @param jda The JDA instance
     */
    @Override
    public void register(JDA jda) {
    }

    /**
     * Initiates the command deployment process
     *
     * <p>
     * This method retrieves commands from the registry, categorizes them
     * based on their configuration (global vs. guild-specific), and then
     * sends the deployment requests to the appropriate Discord API endpoints.
     * It logs the success or failure of each deployment
     * </p>
     */
    public void deploy() {
        ShardManager shardManager = client.getShardManager();
        if (shardManager == null) {
            Logger.error("[CommandDeployer] ShardManager was null during command deployment");
            return;
        }

        CommandRegistry registry = client.commandRegistry();
        String[] testServers = client.getConfig().testServers();

        // Partition slash commands into global vs test
        Map<Boolean, List<SlashCommandData>> slashPartition = registry.slashView().values().stream()
                .map(cmd -> buildSlash(cmd.initAndGetConfig()))
                .collect(Collectors.partitioningBy(SlashCommandData::isGuildOnly)); // treat testOnly as guildOnly

        List<SlashCommandData> globalSlash = List.copyOf(slashPartition.get(false));
        List<SlashCommandData> testSlash = List.copyOf(slashPartition.get(true));

        // Partition context commands
        Map<Boolean, List<CommandData>> contextPartition = registry.contextView().values().stream()
                .map(cmd -> buildContext(cmd.initAndGetConfig()))
                .filter(Objects::nonNull)
                .collect(Collectors.partitioningBy(CommandData::isGuildOnly)); // similar handling

        List<CommandData> globalContext = List.copyOf(contextPartition.get(false));
        List<CommandData> testContext = List.copyOf(contextPartition.get(true));

        // Deploy global once (first shard is enough)
        shardManager.getShards().stream().findFirst().ifPresent(firstShard -> firstShard.updateCommands()
                .addCommands(globalSlash)
                .addCommands(globalContext)
                .queue(
                        success -> Logger.debug(() -> "[CommandDeployer] Global commands deployed: "
                                + (globalSlash.size() + globalContext.size())),
                        failure -> Logger.error("[CommandDeployer] Failed to deploy global commands", failure)));

        // Deploy per test guild
        List<CommandData> testMerged = new ArrayList<>(testSlash.size() + testContext.size());
        testMerged.addAll(testSlash);
        testMerged.addAll(testContext);

        if (!testMerged.isEmpty()) {
            for (JDA shard : shardManager.getShards()) {
                for (String guildId : testServers) {
                    Guild guild = shard.getGuildById(guildId);
                    if (guild == null) {
                        Logger.warning("[CommandDeployer] Test guild not found: " + guildId);
                        continue;
                    }
                    guild.updateCommands().addCommands(testMerged).queue(
                            success -> Logger.debug(() -> "[CommandDeployer] Deployed "
                                    + testMerged.size() + " test commands to guild " + guild.getName()),
                            failure -> Logger.error(
                                    "[CommandDeployer] Failed to deploy test commands to guild " + guildId, failure));
                }
            }
        }
    }

    /**
     * Builds a {@link SlashCommandData} object from a {@link SlashCommandConfig}
     *
     * <p>
     * This helper method translates the internal command configuration into
     * the format required by JDA for deployment, including options, subcommands,
     * and default permissions
     * </p>
     *
     * @param cfg The configuration of the slash command
     * @return A {@link SlashCommandData} object ready for deployment
     */
    private SlashCommandData buildSlash(SlashCommandConfig cfg) {
        SlashCommandData data = Commands.slash(cfg.name(), cfg.description())
                .addOptions(cfg.options())
                .addSubcommands(cfg.subcommands())
                .addSubcommandGroups(cfg.subcommandGroups())
                .setNSFW(cfg.NSFW());

        if (!cfg.userPermissions().isEmpty()) {
            data.setDefaultPermissions(
                    net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
                            .enabledFor(cfg.userPermissions()));
        }

        // repurpose testOnly → guildOnly marker
        data.setGuildOnly(cfg.testOnly());
        return data;
    }

    /**
     * Builds a {@link CommandData} object for a context command
     *
     * <p>
     * This helper method converts the context command configuration into
     * the appropriate JDA format, differentiating between user and message types
     * </p>
     *
     * @param cfg The configuration of the context command
     * @return A {@link CommandData} object ready for deployment
     */
    private CommandData buildContext(ContextCommandConfig cfg) {
        CommandData data = (cfg.type() == Command.Type.USER)
                ? Commands.user(cfg.name())
                : Commands.message(cfg.name());

        // mark testOnly as guildOnly
        if (cfg.testOnly()) {
            data.setGuildOnly(true);
        }
        return data;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
       deploy();

       if (client.getConfig().disabledDefaults().contains(DefaultEvents.CLIENT_READY))
           Logger.flushStartupBuffer();
    }
}