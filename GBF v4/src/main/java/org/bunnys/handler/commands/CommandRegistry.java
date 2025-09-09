package org.bunnys.handler.commands;

import org.bunnys.handler.commands.message.MessageCommandConfig;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class CommandRegistry {
    private final ConcurrentHashMap<String, List<CommandEntry>> merged = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageCommand> messageCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SlashCommand> slashCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ContextCommand> contextCommands = new ConcurrentHashMap<>();

    public static String canonical(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
    }

    public void registerMessageCommand(MessageCommand cmd, MessageCommandConfig cfg) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cfg, "cfg");

        String canonicalName = canonical(cfg.commandName());
        validateName(canonicalName, "Command");

        synchronized (this) {
            Set<String> allKeys = new HashSet<>();
            allKeys.add(canonicalName);
            cfg.aliases().stream().map(CommandRegistry::canonical).forEach(allKeys::add);

            List<String> conflicts = allKeys.stream()
                    .filter(messageCommands::containsKey)
                    .toList();

            if (!conflicts.isEmpty()) {
                String msg = "[CommandRegistry] Duplicate message command key detected: " + conflicts.getFirst()
                        + " (attempted to register " + cfg.commandName() + " / aliases=" + cfg.aliases() + ")";
                Logger.error(msg);
                throw new IllegalStateException(msg);
            }

            CommandEntry entry = CommandEntry.forMessage(cmd, cfg);

            messageCommands.put(canonicalName, cmd);
            addToMerged(canonicalName, entry);

            for (String alias : cfg.aliases()) {
                String key = canonical(alias);
                messageCommands.put(key, cmd);
                addToMerged(key, entry);
            }
        }

        Logger.debug(() -> "[CommandRegistry] Registered message command: " + cfg.commandName()
                + (cfg.aliases().isEmpty() ? "" : " with aliases: " + String.join(", ", cfg.aliases())));
    }

    public void registerSlashCommand(SlashCommand cmd, SlashCommandConfig cfg) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cfg, "cfg");

        String canonicalName = canonical(cfg.name());
        validateName(canonicalName, "Slash Command");

        synchronized (this) {
            if (slashCommands.containsKey(canonicalName)) {
                String msg = "Duplicate slash command detected: " + canonicalName;
                Logger.error("[CommandRegistry] " + msg);
                throw new IllegalStateException(msg);
            }

            CommandEntry entry = CommandEntry.forSlash(cmd, cfg);
            slashCommands.put(canonicalName, cmd);
            addToMerged("/" + canonicalName, entry);
        }

        Logger.debug(() -> "[CommandRegistry] Registered Slash Command: " + cfg.name());
    }

    public void registerContextCommand(ContextCommand cmd, ContextCommandConfig cfg) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cfg, "cfg");

        String canonicalName = canonical(cfg.name());
        validateName(canonicalName, "Context Command");

        synchronized (this) {
            if (contextCommands.containsKey(canonicalName)) {
                String msg = "Duplicate context command detected: " + canonicalName;
                Logger.error("[CommandRegistry] " + msg);
                throw new IllegalStateException(msg);
            }

            CommandEntry entry = CommandEntry.forContext(cmd, cfg);
            contextCommands.put(canonicalName, cmd);
            addToMerged(canonicalName, entry);
        }

        Logger.debug(() -> "[CommandRegistry] Registered Context Command: " + cfg.name());
    }

    public List<CommandEntry> findMerged(String token) {
        return token == null || token.isBlank()
                ? List.of()
                : merged.getOrDefault(canonical(token), List.of());
    }

    public CommandEntry findMessage(String token) {
        if (token == null || token.isBlank())
            return null;

        MessageCommand cmd = messageCommands.get(canonical(token));
        return cmd != null ? CommandEntry.forMessage(cmd, cmd.initAndGetConfig()) : null;
    }

    public CommandEntry findSlash(String token) {
        if (token == null || token.isBlank())
            return null;

        SlashCommand cmd = slashCommands.get(canonical(token));
        return cmd != null ? CommandEntry.forSlash(cmd, cmd.initAndGetConfig()) : null;
    }

    public CommandEntry findContext(String token) {
        if (token == null || token.isBlank())
            return null;

        ContextCommand cmd = contextCommands.get(canonical(token));
        return cmd != null ? CommandEntry.forContext(cmd, cmd.initAndGetConfig()) : null;
    }

    public Map<String, MessageCommand> messageView() {
        return Collections.unmodifiableMap(messageCommands);
    }

    public Map<String, SlashCommand> slashView() {
        return Collections.unmodifiableMap(slashCommands);
    }

    public Map<String, ContextCommand> contextView() {
        return Collections.unmodifiableMap(contextCommands);
    }

    public Map<String, List<CommandEntry>> mergedView() {
        return Collections.unmodifiableMap(merged);
    }

    public void clearMessageCommands() {
        synchronized (this) {
            messageCommands.clear();
            merged.entrySet().removeIf(
                    entry -> entry.getValue().stream().anyMatch(e -> e.type() == CommandEntry.CommandType.MESSAGE));
        }
    }

    public void clearSlashCommands() {
        synchronized (this) {
            slashCommands.clear();
            merged.entrySet().removeIf(
                    entry -> entry.getValue().stream().anyMatch(e -> e.type() == CommandEntry.CommandType.SLASH));
        }
    }

    public void clearContextCommands() {
        synchronized (this) {
            contextCommands.clear();
            merged.entrySet().removeIf(
                    entry -> entry.getValue().stream().anyMatch(e -> e.type() == CommandEntry.CommandType.CONTEXT));
        }
    }

    public void clearAll() {
        synchronized (this) {
            messageCommands.clear();
            slashCommands.clear();
            contextCommands.clear();
            merged.clear();
        }
    }

    private void validateName(String canonicalName, String commandType) {
        if (canonicalName.isBlank()) {
            throw new IllegalArgumentException(commandType + " name cannot be blank");
        }
    }

    private void addToMerged(String key, CommandEntry entry) {
        merged.compute(key, (k, list) -> {
            List<CommandEntry> newList = list != null ? new ArrayList<>(list) : new ArrayList<>();
            newList.add(entry);
            return newList;
        });
    }

    public record CommandEntry(
            CommandType type,
            MessageCommand messageCommand, MessageCommandConfig messageMetaData,
            SlashCommand slashCommand, SlashCommandConfig slashMetaData,
            ContextCommand contextCommand, ContextCommandConfig contextMetaData) {

        public enum CommandType {
            MESSAGE, SLASH, CONTEXT
        }

        public static CommandEntry forMessage(MessageCommand cmd, MessageCommandConfig meta) {
            return new CommandEntry(CommandType.MESSAGE, cmd, meta, null, null, null, null);
        }

        public static CommandEntry forSlash(SlashCommand cmd, SlashCommandConfig meta) {
            return new CommandEntry(CommandType.SLASH, null, null, cmd, meta, null, null);
        }

        public static CommandEntry forContext(ContextCommand cmd, ContextCommandConfig meta) {
            return new CommandEntry(CommandType.CONTEXT, null, null, null, null, cmd, meta);
        }
    }
}