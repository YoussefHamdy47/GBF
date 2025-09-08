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
 * A thread-safe registry for both message-based and slash commands
 * <p>
 * This class ensures atomic registration of commands, preventing any
 * race conditions or inconsistent states The registry maintains separate
 * maps for message and slash commands, as well as a merged view
 * for unified lookup Duplicate command names or aliases will result in a
 * {@link IllegalStateException} to enforce a fail-fast behavior and ensure
 * a deterministic command set
 * </p>
 *
 * @author Bunny
 */
@SuppressWarnings("unused")
public class CommandRegistry {
    private final ConcurrentHashMap<String, List<CommandEntry>> merged = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageCommand> messageCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SlashCommand> slashCommands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ContextCommand> contextCommands = new ConcurrentHashMap<>();

    /**
     * Normalizes a command name or alias to its canonical form
     * This involves converting the string to lowercase and trimming whitespace
     *
     * @param name The command name or alias
     * @return The canonical, normalized string
     */
    public static String canonical(String name) {
        if (name == null)
            return "";
        return name.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Registers a message command and all its aliases
     * <p>
     * This operation is fully synchronized to guarantee atomicity of the
     * registration process It first checks for any naming conflicts with existing
     * commands or aliases before inserting the new command
     * </p>
     *
     * @param cmd The {@link MessageCommand} instance to register
     * @param cfg The {@link MessageCommandConfig} containing the command's metadata
     * @throws IllegalStateException    if a name or alias conflict is found
     * @throws IllegalArgumentException if the command name is blank
     */
    public void registerMessageCommand(MessageCommand cmd, MessageCommandConfig cfg) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cfg, "cfg");

        String canonicalName = canonical(cfg.commandName());
        if (canonicalName.isBlank())
            throw new IllegalArgumentException("Command name cannot be blank");

        CommandEntry entry = CommandEntry.forMessage(cmd, cfg);

        synchronized (this) {
            // Check for conflicts only inside message commands
            List<String> conflicts = new ArrayList<>();
            if (messageCommands.containsKey(canonicalName))
                conflicts.add(canonicalName);
            for (String alias : cfg.aliases()) {
                String key = canonical(alias);
                if (messageCommands.containsKey(key))
                    conflicts.add(key);
            }

            if (!conflicts.isEmpty()) {
                String firstConflict = conflicts.getFirst();
                String msg = "[CommandRegistry] Duplicate message command key detected: " + firstConflict
                        + " (attempted to register " + cfg.commandName() + " / aliases=" + cfg.aliases() + ")";
                Logger.error(msg);
                throw new IllegalStateException(msg);
            }

            // Register into message map
            this.messageCommands.put(canonicalName, cmd);
            addToMerged(canonicalName, CommandEntry.forMessage(cmd, cfg));

            for (String alias : cfg.aliases()) {
                String key = canonical(alias);
                this.messageCommands.putIfAbsent(key, cmd);
                addToMerged(key, CommandEntry.forMessage(cmd, cfg));
            }
        }

        Logger.debug(() -> "[CommandRegistry] Registered message command: " + cfg.commandName()
                + (cfg.aliases().isEmpty() ? "" : " with aliases: " + String.join(", ", cfg.aliases())));
    }

    /**
     * Registers a slash command
     * <p>
     * This operation is synchronized to ensure thread safety during registration
     * It checks for duplicate command names before insertion
     * </p>
     *
     * @param cmd The {@link SlashCommand} instance to register
     * @param cfg The {@link SlashCommandConfig} containing the command's metadata
     * @throws IllegalStateException    if the command name is already registered
     * @throws IllegalArgumentException if the command name is blank
     */
    public void registerSlashCommand(SlashCommand cmd, SlashCommandConfig cfg) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cfg, "cfg");

        String canonicalName = canonical(cfg.name());
        if (canonicalName.isBlank())
            throw new IllegalArgumentException("Slash Command name cannot be blank");

        CommandEntry entry = CommandEntry.forSlash(cmd, cfg);

        synchronized (this) {
            if (this.slashCommands.containsKey(canonicalName)) {
                String msg = "Duplicate slash command detected: " + canonicalName;
                Logger.error("[CommandRegistry] " + msg);
                throw new IllegalStateException(msg);
            }

            this.slashCommands.putIfAbsent(canonicalName, cmd);
            addToMerged("/" + canonicalName, CommandEntry.forSlash(cmd, cfg));
        }

        Logger.debug(() -> "[CommandRegistry] Registered Slash Command: " + cfg.name());
    }

    public void registerContextCommand(ContextCommand cmd, ContextCommandConfig cfg) {
        Objects.requireNonNull(cmd, "cmd");
        Objects.requireNonNull(cfg, "cfg");

        String canonicalName = canonical(cfg.name());

        if (canonicalName.isBlank())
            throw new IllegalArgumentException("Context Command name cannot be blank");

        synchronized (this) {
            if (this.contextCommands.containsKey(canonicalName)) {
                String msg = "Duplicate context command detected: " + canonicalName;
                Logger.error("[CommandRegistry] " + msg);
                throw new IllegalStateException(msg);
            }
            this.contextCommands.putIfAbsent(canonicalName, cmd);
            addToMerged(canonicalName, CommandEntry.forContext(cmd, cfg));
        }
    }

    /**
     * Finds a command by its name or alias across all command types
     *
     * @param token The name or alias to look up
     * @return The {@link CommandEntry} if found, otherwise {@code null}
     */
    public List<CommandEntry> findMerged(String token) {
        if (token == null || token.isBlank())
            return null;
        return this.merged.getOrDefault(canonical(token), List.of());
    }

    /**
     * Finds a message command by its name or alias
     *
     * @param token The name or alias to look up
     * @return The {@link CommandEntry} for the message command if found, otherwise
     *         {@code null}
     */
    public CommandEntry findMessage(String token) {
        if (token == null || token.isBlank())
            return null;

        String key = canonical(token);
        MessageCommand cmd = this.messageCommands.get(key);
        return cmd != null ? CommandEntry.forMessage(cmd, cmd.initAndGetConfig()) : null;
    }

    /**
     * Finds a slash command by its name
     *
     * @param token The name to look up
     * @return The {@link CommandEntry} for the slash command if found, otherwise
     *         {@code null}
     */
    public CommandEntry findSlash(String token) {
        if (token == null || token.isBlank())
            return null;

        String key = canonical(token);
        SlashCommand cmd = this.slashCommands.get(key);
        return cmd != null ? CommandEntry.forSlash(cmd, cmd.initAndGetConfig()) : null;
    }

    public CommandEntry findContext(String token) {
        if (token == null || token.isBlank())
            return null;

        String key = canonical(token);
        ContextCommand cmd = this.contextCommands.get(key);
        return cmd != null ? CommandEntry.forContext(cmd, cmd.initAndGetConfig()) : null;
    }

    /**
     * Gets an unmodifiable view of the registered message commands
     *
     * @return A map of canonical names to {@link MessageCommand} instances
     */
    public Map<String, MessageCommand> messageView() {
        return Collections.unmodifiableMap(this.messageCommands);
    }

    /**
     * Gets an unmodifiable view of the registered slash commands
     *
     * @return A map of canonical names to {@link SlashCommand} instances
     */
    public Map<String, SlashCommand> slashView() {
        return Collections.unmodifiableMap(this.slashCommands);
    }

    public Map<String, ContextCommand> contextView() {
        return Collections.unmodifiableMap(this.contextCommands);
    }

    /**
     * Gets an unmodifiable view of all registered commands, regardless of type
     *
     * @return A map of canonical names to {@link CommandEntry} instances
     */
    public Map<String, List<CommandEntry>> mergedView() {
        return Collections.unmodifiableMap(this.merged);
    }

    public void clearMessageCommands() {
        this.messageCommands.clear();
    }

    public void clearSlashCommands() {
        this.slashCommands.clear();
    }

    private void addToMerged(String key, CommandEntry entry) {
        this.merged.compute(key, (k, list) -> {
            if (list == null)
                list = new ArrayList<>();

            list.add(entry);
            return list;
        });
    }

    /**
     * A record representing a unified command entry, abstracting over message
     * and slash command types It provides a single point of access to the command
     * instance and its metadata
     */
    public record CommandEntry(
            CommandType type,
            MessageCommand messageCommand, MessageCommandConfig messageMetaData,
            SlashCommand slashCommand, SlashCommandConfig slashMetaData,
            ContextCommand contextCommand, ContextCommandConfig contextMetaData
    ) {
        public enum CommandType {
            MESSAGE, SLASH, CONTEXT
        }

        /**
         * Creates a new CommandEntry for a message command
         *
         * @param cmd  The {@link MessageCommand} instance
         * @param meta The {@link MessageCommandConfig} metadata
         * @return A new {@link CommandEntry}
         */
        public static CommandEntry forMessage(MessageCommand cmd, MessageCommandConfig meta) {
            return new CommandEntry(CommandType.MESSAGE, cmd, meta, null, null, null, null);
        }

        /**
         * Creates a new CommandEntry for a slash command
         *
         * @param cmd  The {@link SlashCommand} instance
         * @param meta The {@link SlashCommandConfig} metadata
         * @return A new {@link CommandEntry}
         */
        public static CommandEntry forSlash(SlashCommand cmd, SlashCommandConfig meta) {
            return new CommandEntry(CommandType.SLASH, null, null, cmd, meta, null, null);
        }

        public static CommandEntry forContext(ContextCommand cmd, ContextCommandConfig meta) {
            return new CommandEntry(CommandType.CONTEXT, null, null, null, null, cmd, meta);
        }
    }
}