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

/**
 * A central, thread-safe registry for managing all bot commands
 *
 * <p>
 * This class acts as the single source of truth for command registration,
 * lookups, and lifecycle management. It separates commands by type (message,
 * slash, and context) while also providing a merged view for unified lookups
 * </p>
 */
@SuppressWarnings("unused")
public class CommandRegistry {
    /** A merged map of all command types for easy lookups by name/alias */
    private final ConcurrentHashMap<String, List<CommandEntry>> merged = new ConcurrentHashMap<>();
    /** A map for message commands, keyed by canonical name/alias */
    private final ConcurrentHashMap<String, MessageCommand> messageCommands = new ConcurrentHashMap<>();
    /** A map for slash commands, keyed by canonical name */
    private final ConcurrentHashMap<String, SlashCommand> slashCommands = new ConcurrentHashMap<>();
    /** A map for context commands, keyed by canonical name */
    private final ConcurrentHashMap<String, ContextCommand> contextCommands = new ConcurrentHashMap<>();

    /**
     * Converts a string to a canonical, lowercase, trimmed format
     *
     * @param name The name to canonicalize
     * @return The canonical string
     */
    public static String canonical(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Registers a message command with its configuration
     *
     * <p>
     * This method performs a thread-safe registration, checking for name
     * and alias conflicts before adding the command to the registry
     * </p>
     *
     * @param cmd The {@link MessageCommand} instance
     * @param cfg The {@link MessageCommandConfig} for the command
     * @throws IllegalStateException if a name or alias conflict is found
     * @throws NullPointerException  if cmd or cfg are null
     */
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

    /**
     * Registers a slash command
     *
     * <p>
     * This method performs a thread-safe registration, ensuring no duplicate
     * slash command names exist before adding the command
     * </p>
     *
     * @param cmd The {@link SlashCommand} instance
     * @param cfg The {@link SlashCommandConfig} for the command
     * @throws IllegalStateException if a duplicate name is found
     * @throws NullPointerException  if cmd or cfg are null
     */
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

    /**
     * Registers a context command
     *
     * <p>
     * This method performs a thread-safe registration, ensuring no duplicate
     * context command names exist before adding the command
     * </p>
     *
     * @param cmd The {@link ContextCommand} instance
     * @param cfg The {@link ContextCommandConfig} for the command
     * @throws IllegalStateException if a duplicate name is found
     * @throws NullPointerException  if cmd or cfg are null
     */
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

    /**
     * Finds a list of all commands (of any type) by a given name or token
     *
     * @param token The name or alias to search for
     * @return A list of {@link CommandEntry} records matching the token, or an
     *         empty list
     */
    public List<CommandEntry> findMerged(String token) {
        return token == null || token.isBlank()
                ? List.of()
                : merged.getOrDefault(canonical(token), List.of());
    }

    /**
     * Finds a message command by its name or alias
     *
     * @param token The name or alias to search for
     * @return A {@link CommandEntry} for the message command, or null if not found
     */
    public CommandEntry findMessage(String token) {
        if (token == null || token.isBlank())
            return null;

        MessageCommand cmd = messageCommands.get(canonical(token));
        return cmd != null ? CommandEntry.forMessage(cmd, cmd.initAndGetConfig()) : null;
    }

    /**
     * Finds a slash command by its name
     *
     * @param token The name to search for
     * @return A {@link CommandEntry} for the slash command, or null if not found
     */
    public CommandEntry findSlash(String token) {
        if (token == null || token.isBlank())
            return null;

        SlashCommand cmd = slashCommands.get(canonical(token));
        return cmd != null ? CommandEntry.forSlash(cmd, cmd.initAndGetConfig()) : null;
    }

    /**
     * Finds a context command by its name
     *
     * @param token The name to search for
     * @return A {@link CommandEntry} for the context command, or null if not found
     */
    public CommandEntry findContext(String token) {
        if (token == null || token.isBlank())
            return null;

        ContextCommand cmd = contextCommands.get(canonical(token));
        return cmd != null ? CommandEntry.forContext(cmd, cmd.initAndGetConfig()) : null;
    }

    /**
     * Provides an unmodifiable view of all message commands
     *
     * @return A map of message command names to their instances
     */
    public Map<String, MessageCommand> messageView() {
        return Collections.unmodifiableMap(messageCommands);
    }

    /**
     * Provides an unmodifiable view of all slash commands
     *
     * @return A map of slash command names to their instances
     */
    public Map<String, SlashCommand> slashView() {
        return Collections.unmodifiableMap(slashCommands);
    }

    /**
     * Provides an unmodifiable view of all context commands
     *
     * @return A map of context command names to their instances
     */
    public Map<String, ContextCommand> contextView() {
        return Collections.unmodifiableMap(contextCommands);
    }

    /**
     * Provides an unmodifiable view of the merged command registry
     *
     * @return A map of canonical names/aliases to lists of command entries
     */
    public Map<String, List<CommandEntry>> mergedView() {
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Clears all registered message commands and their entries from the merged map
     */
    public void clearMessageCommands() {
        synchronized (this) {
            messageCommands.clear();
            merged.entrySet().removeIf(
                    entry -> entry.getValue().stream().anyMatch(e -> e.type() == CommandEntry.CommandType.MESSAGE));
        }
    }

    /**
     * Clears all registered slash commands and their entries from the merged map
     */
    public void clearSlashCommands() {
        synchronized (this) {
            slashCommands.clear();
            merged.entrySet().removeIf(
                    entry -> entry.getValue().stream().anyMatch(e -> e.type() == CommandEntry.CommandType.SLASH));
        }
    }

    /**
     * Clears all registered context commands and their entries from the merged map
     */
    public void clearContextCommands() {
        synchronized (this) {
            contextCommands.clear();
            merged.entrySet().removeIf(
                    entry -> entry.getValue().stream().anyMatch(e -> e.type() == CommandEntry.CommandType.CONTEXT));
        }
    }

    /**
     * Clears all commands from all registries
     */
    public void clearAll() {
        synchronized (this) {
            messageCommands.clear();
            slashCommands.clear();
            contextCommands.clear();
            merged.clear();
        }
    }

    /**
     * Clears all commands from all registries and returns the total number of cleared commands
     *
     * @return total number of commands cleared
     */
    public int clearAllWithCount() {
        synchronized (this) {
            int total = messageCommands.size() + slashCommands.size() + contextCommands.size();

            messageCommands.clear();
            slashCommands.clear();
            contextCommands.clear();
            merged.clear();

            return total;
        }
    }


    /**
     * Validates that a canonical name is not blank
     *
     * @param canonicalName The name to validate
     * @param commandType   The type of command for logging purposes
     * @throws IllegalArgumentException if the name is blank
     */
    private void validateName(String canonicalName, String commandType) {
        if (canonicalName.isBlank()) {
            throw new IllegalArgumentException(commandType + " name cannot be blank");
        }
    }

    /**
     * Adds a command entry to the merged map
     *
     * @param key   The key to add the entry under
     * @param entry The {@link CommandEntry} to add
     */
    private void addToMerged(String key, CommandEntry entry) {
        merged.compute(key, (k, list) -> {
            List<CommandEntry> newList = list != null ? new ArrayList<>(list) : new ArrayList<>();
            newList.add(entry);
            return newList;
        });
    }

    /**
     * A record representing a command entry in the registry
     *
     * <p>
     * This record stores the command type and a reference to the command
     * instance and its configuration, allowing for type-safe retrieval
     * </p>
     *
     * @param type            The type of the command
     * @param messageCommand  The message command instance (if applicable)
     * @param messageMetaData The message command's configuration (if applicable)
     * @param slashCommand    The slash command instance (if applicable)
     * @param slashMetaData   The slash command's configuration (if applicable)
     * @param contextCommand  The context command instance (if applicable)
     * @param contextMetaData The context command's configuration (if applicable)
     */
    public record CommandEntry(
            CommandType type,
            MessageCommand messageCommand, MessageCommandConfig messageMetaData,
            SlashCommand slashCommand, SlashCommandConfig slashMetaData,
            ContextCommand contextCommand, ContextCommandConfig contextMetaData) {

        /** The enumeration of supported command types */
        public enum CommandType {
            MESSAGE, SLASH, CONTEXT
        }

        /**
         * Creates a {@code CommandEntry} for a message command
         *
         * @param cmd  The message command instance
         * @param meta The message command's configuration
         * @return A new {@code CommandEntry}
         */
        public static CommandEntry forMessage(MessageCommand cmd, MessageCommandConfig meta) {
            return new CommandEntry(CommandType.MESSAGE, cmd, meta, null, null, null, null);
        }

        /**
         * Creates a {@code CommandEntry} for a slash command
         *
         * @param cmd  The slash command instance
         * @param meta The slash command's configuration
         * @return A new {@code CommandEntry}
         */
        public static CommandEntry forSlash(SlashCommand cmd, SlashCommandConfig meta) {
            return new CommandEntry(CommandType.SLASH, null, null, cmd, meta, null, null);
        }

        /**
         * Creates a {@code CommandEntry} for a context command
         *
         * @param cmd  The context command instance
         * @param meta The context command's configuration
         * @return A new {@code CommandEntry}
         */
        public static CommandEntry forContext(ContextCommand cmd, ContextCommandConfig meta) {
            return new CommandEntry(CommandType.CONTEXT, null, null, null, null, cmd, meta);
        }
    }
}