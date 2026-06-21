package org.bunnys.handler.commands;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.bunnys.handler.BunnyHub;
import org.bunnys.utils.BunnyLog;

import java.util.*;

public class CommandRegistry {
    private final BunnyHub client;
    private final Map<String, BunnyCommand> commands = new HashMap<>();

    private final List<String> developerIds;
    private final List<String> testServerIds;

    public CommandRegistry(BunnyHub client, List<String> developerIds, List<String> testServerIds) {
        this.client = client;
        this.developerIds = developerIds;
        this.testServerIds = testServerIds;
    }

    public int clearCommands() {
        int count = commands.size();
        commands.clear();
        return count;
    }

    public void deployCommands() {
        List<CommandData> globalCommands = new ArrayList<>();
        Map<String, List<CommandData>> guildCommands = new HashMap<>();

        for (BunnyCommand cmd : commands.values()) {
            if (cmd.isTestOnly()) {
                for (String guildId : testServerIds) {
                    guildCommands.computeIfAbsent(guildId, k -> new ArrayList<>()).add(cmd.buildCommandData());
                }
            } else {
                globalCommands.add(cmd.buildCommandData());
            }
        }

        client.getJDA().updateCommands().addCommands(globalCommands).queue(
                success -> BunnyLog.success("Registered " + globalCommands.size() + " global commands!"),
                error -> BunnyLog.error("Failed to deploy global commands: " + error.getMessage()));

        guildCommands.forEach((guildId, commandList) -> {
            var guild = client.getJDA().getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(commandList).queue(
                        success -> BunnyLog
                                .success("Registered " + commandList.size() + " test commands to guild: " + guildId),
                        error -> BunnyLog
                                .error("Failed to deploy guild commands for " + guildId + ": " + error.getMessage()));
            }
        });
    }

    public int getCommandCount() {
        return commands.size();
    }

    public Map<String, BunnyCommand> getCommands() {
        return commands;
    }

    public List<String> getDeveloperIds() {
        return developerIds;
    }

    public void registerCommand(BunnyCommand cmdInstance) {
        if (cmdInstance.getName() == null || cmdInstance.getName().isEmpty()) {
            BunnyLog.warning("Attempted to register a command with no name. Ignored.");
            return;
        }
        commands.put(cmdInstance.getName(), cmdInstance);
    }
}