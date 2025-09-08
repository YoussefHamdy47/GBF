package org.bunnys.handler.commands;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.bunnys.handler.Config;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;

/**
 * A utility class for dynamically loading command implementations
 * <p>
 * This class uses the ClassGraph library to scan a specified base package for
 * implementations of {@link MessageCommand} and {@link SlashCommand}
 * It leverages reflection to instantiate these command classes, with support
 * for multiple constructor types to allow for dependency injection of
 * the client and configuration objects
 * Command instantiation is performed in parallel using a fixed thread pool
 * to optimize loading time, especially for a large number of commands
 * Error handling is robust, ensuring that a single failing command does not
 * halt the entire loading process
 * All returned lists of commands are immutable
 * </p>
 *
 * @author Bunny
 */
public final class CommandLoader {
    private final String basePackage;
    private final BunnyNexus client;

    /**
     * Constructs a new CommandLoader
     *
     * @param basePackage The package to scan for command implementations
     * @param client      The main bot client instance, used for dependency
     *                    injection
     * @throws NullPointerException if the client is null
     */
    public CommandLoader(String basePackage, BunnyNexus client) {
        this.basePackage = basePackage;
        this.client = Objects.requireNonNull(client, "Client cannot be null");
    }

    /**
     * Scans for and instantiates all concrete implementations of
     * {@link MessageCommand}
     *
     * @return An unmodifiable list of instantiated message commands, or an empty
     *         list if none are found
     */
    public List<MessageCommand> loadMessageCommands() {
        if (basePackage == null || basePackage.isBlank())
            return List.of();

        List<String> classNames = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(basePackage)
                .scan()) {
            scanResult.getSubclasses(MessageCommand.class.getName())
                    .forEach(classInfo -> {
                        try {
                            classNames.add(classInfo.getName());
                        } catch (Exception e) {
                            Logger.error("[CommandLoader] Failed to read class name: " + classInfo, e);
                        }
                    });
        } catch (Exception e) {
            Logger.error("[CommandLoader] ClassGraph scan failed for package: " + basePackage, e);
            return List.of();
        }

        if (classNames.isEmpty()) {
            Logger.debug(() -> "[CommandLoader] No MessageCommand subclasses found in " + basePackage);
            return List.of();
        }

        int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        ExecutorService exec = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("msg-loader-worker");
            return t;
        });

        List<Future<MessageCommand>> futures = new ArrayList<>();
        for (String className : classNames)
            futures.add(exec.submit(() -> instantiateMessageSafely(className)));

        List<MessageCommand> results = new ArrayList<>();
        for (Future<MessageCommand> f : futures) {
            try {
                MessageCommand cmd = f.get();
                if (cmd != null)
                    results.add(cmd);
            } catch (ExecutionException ee) {
                Logger.error("[CommandLoader] MessageCommand instantiation task threw",
                        ee.getCause() == null ? ee : ee.getCause());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.error("[CommandLoader] Interrupted while loading message commands", ie);
            }
        }

        exec.shutdownNow();
        Logger.debug(() -> "[CommandLoader] Instantiated " + results.size() + " message command(s)");
        return Collections.unmodifiableList(results);
    }

    /**
     * Instantiates a single MessageCommand class safely
     * This method attempts to find and invoke a suitable constructor for the class
     *
     * @param className The fully qualified name of the class to instantiate
     * @return An instantiated {@link MessageCommand} object, or {@code null} if
     *         instantiation fails
     */
    private MessageCommand instantiateMessageSafely(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to load class " + className, t);
            return null;
        }

        int mods = clazz.getModifiers();
        if (Modifier.isAbstract(mods) || Modifier.isInterface(mods) || clazz.isAnnotation())
            return null;

        if (!MessageCommand.class.isAssignableFrom(clazz)) {
            Logger.debug(() -> "[CommandLoader] Skipping " + className + " (not a MessageCommand)");
            return null;
        }

        try {
            // (BunnyNexus, Config)
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(BunnyNexus.class, Config.class);
                ctor.setAccessible(true);
                return (MessageCommand) ctor.newInstance(client, client.getConfig());
            } catch (NoSuchMethodException ignored) {
            }

            // (BunnyNexus)
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(BunnyNexus.class);
                ctor.setAccessible(true);
                return (MessageCommand) ctor.newInstance(client);
            } catch (NoSuchMethodException ignored) {
            }

            // (Config)
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(Config.class);
                ctor.setAccessible(true);
                return (MessageCommand) ctor.newInstance(client.getConfig());
            } catch (NoSuchMethodException ignored) {
            }

            // ()
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                return (MessageCommand) ctor.newInstance();
            } catch (NoSuchMethodException ignored) {
            }

            Logger.error("[CommandLoader] No supported constructor found for " + className);
            return null;
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to instantiate " + className + ": " + t.getMessage(), t);
            return null;
        }
    }

    /**
     * Scans for and instantiates all concrete implementations of
     * {@link SlashCommand}
     *
     * @return An unmodifiable list of instantiated slash commands, or an empty list
     *         if none are found
     */
    public List<SlashCommand> loadSlashCommands() {
        if (basePackage == null || basePackage.isBlank())
            return List.of();

        List<String> classNames = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(basePackage)
                .scan()) {
            scanResult.getSubclasses(SlashCommand.class.getName())
                    .forEach(classInfo -> {
                        try {
                            classNames.add(classInfo.getName());
                        } catch (Exception e) {
                            Logger.error("[CommandLoader] Failed to read class name: " + classInfo, e);
                        }
                    });
        } catch (Exception e) {
            Logger.error("[CommandLoader] ClassGraph scan failed for package: " + basePackage, e);
            return List.of();
        }

        if (classNames.isEmpty()) {
            Logger.debug(() -> "[CommandLoader] No SlashCommand subclasses found in " + basePackage);
            return List.of();
        }

        int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        ExecutorService exec = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("slash-loader-worker");
            return t;
        });

        List<Future<SlashCommand>> futures = new ArrayList<>();
        for (String className : classNames)
            futures.add(exec.submit(() -> instantiateSlashSafely(className)));

        List<SlashCommand> results = new ArrayList<>();
        for (Future<SlashCommand> f : futures) {
            try {
                SlashCommand cmd = f.get();
                if (cmd != null)
                    results.add(cmd);
            } catch (ExecutionException ee) {
                Logger.error("[CommandLoader] SlashCommand instantiation task threw",
                        ee.getCause() == null ? ee : ee.getCause());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.error("[CommandLoader] Interrupted while loading slash commands", ie);
            }
        }

        exec.shutdownNow();

        Map<String, SlashCommand> deduped = new LinkedHashMap<>();
        for (SlashCommand cmd : results) {
            try {
                String canonical = CommandRegistry.canonical(cmd.initAndGetConfig().name());
                Logger.debug(() -> "[CommandLoader] Preparing to register: "
                        + cmd.getClass().getName() + " as '" + canonical + "'");
                if (deduped.containsKey(canonical)) {
                    Logger.error("[CommandLoader] Duplicate slash command name detected in loader: "
                            + canonical + " (first: " + deduped.get(canonical).getClass().getName()
                            + ", second: " + cmd.getClass().getName() + ")");
                    continue;
                }
                deduped.put(canonical, cmd);
            } catch (Exception e) {
                Logger.error("[CommandLoader] Failed to read config for command: " + cmd.getClass().getName(), e);
            }
        }

        Logger.debug(() -> "[CommandLoader] Instantiated " + deduped.size() + " unique slash command"
                + (deduped.size() == 1 ? "" : "s"));

        return List.copyOf(deduped.values());
    }

    /**
     * Instantiates a single SlashCommand class safely
     * This method attempts to find and invoke a no-argument constructor for the
     * class
     *
     * @param className The fully qualified name of the class to instantiate
     * @return An instantiated {@link SlashCommand} object, or {@code null} if
     *         instantiation fails
     */
    private SlashCommand instantiateSlashSafely(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to load class " + className, t);
            return null;
        }

        int mods = clazz.getModifiers();
        if (Modifier.isAbstract(mods) || Modifier.isInterface(mods) || clazz.isAnnotation())
            return null;

        if (!SlashCommand.class.isAssignableFrom(clazz)) {
            Logger.debug(() -> "[CommandLoader] Skipping " + className + " (not a SlashCommand)");
            return null;
        }

        try {
            // For now, we only support () constructor for slash commands
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (SlashCommand) ctor.newInstance();
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to instantiate slash command " + className, t);
            return null;
        }
    }

    /**
     * Scans for and instantiates all concrete implementations of {@link ContextCommand}
     *
     * @return An unmodifiable list of instantiated context commands, or an empty list if none are found
     */
    public List<ContextCommand> loadContextCommands() {
        if (basePackage == null || basePackage.isBlank())
            return List.of();

        List<String> classNames = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(basePackage)
                .scan()) {
            scanResult.getSubclasses(ContextCommand.class.getName())
                    .forEach(classInfo -> {
                        try {
                            classNames.add(classInfo.getName());
                        } catch (Exception e) {
                            Logger.error("[CommandLoader] Failed to read class name: " + classInfo, e);
                        }
                    });
        } catch (Exception e) {
            Logger.error("[CommandLoader] ClassGraph scan failed for package: " + basePackage, e);
            return List.of();
        }

        if (classNames.isEmpty()) {
            Logger.debug(() -> "[CommandLoader] No ContextCommand subclasses found in " + basePackage);
            return List.of();
        }

        int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 8));
        ExecutorService exec = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("context-loader-worker");
            return t;
        });

        List<Future<ContextCommand>> futures = new ArrayList<>();
        for (String className : classNames)
            futures.add(exec.submit(() -> instantiateContextSafely(className)));

        List<ContextCommand> results = new ArrayList<>();
        for (Future<ContextCommand> f : futures) {
            try {
                ContextCommand cmd = f.get();
                if (cmd != null)
                    results.add(cmd);
            } catch (ExecutionException ee) {
                Logger.error("[CommandLoader] ContextCommand instantiation task threw",
                        ee.getCause() == null ? ee : ee.getCause());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Logger.error("[CommandLoader] Interrupted while loading context commands", ie);
            }
        }

        exec.shutdownNow();

        Map<String, ContextCommand> deduped = new LinkedHashMap<>();
        for (ContextCommand cmd : results) {
            try {
                String canonical = CommandRegistry.canonical(cmd.initAndGetConfig().name());
                Logger.debug(() -> "[CommandLoader] Preparing to register: "
                        + cmd.getClass().getName() + " as '" + canonical + "'");
                if (deduped.containsKey(canonical)) {
                    Logger.error("[CommandLoader] Duplicate context command name detected in loader: "
                            + canonical + " (first: " + deduped.get(canonical).getClass().getName()
                            + ", second: " + cmd.getClass().getName() + ")");
                    continue;
                }
                deduped.put(canonical, cmd);
            } catch (Exception e) {
                Logger.error("[CommandLoader] Failed to read config for command: " + cmd.getClass().getName(), e);
            }
        }

        Logger.debug(() -> "[CommandLoader] Instantiated " + deduped.size() + " unique context command"
                + (deduped.size() == 1 ? "" : "s"));

        return List.copyOf(deduped.values());
    }

    /**
     * Instantiates a single ContextCommand class safely
     *
     * @param className The fully qualified name of the class to instantiate
     * @return An instantiated {@link ContextCommand} object, or {@code null} if instantiation fails
     */
    private ContextCommand instantiateContextSafely(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to load class " + className, t);
            return null;
        }

        int mods = clazz.getModifiers();
        if (Modifier.isAbstract(mods) || Modifier.isInterface(mods) || clazz.isAnnotation())
            return null;

        if (!ContextCommand.class.isAssignableFrom(clazz)) {
            Logger.debug(() -> "[CommandLoader] Skipping " + className + " (not a ContextCommand)");
            return null;
        }

        try {
            // (BunnyNexus, Config)
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(BunnyNexus.class, Config.class);
                ctor.setAccessible(true);
                return (ContextCommand) ctor.newInstance(client, client.getConfig());
            } catch (NoSuchMethodException ignored) {}

            // (BunnyNexus)
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(BunnyNexus.class);
                ctor.setAccessible(true);
                return (ContextCommand) ctor.newInstance(client);
            } catch (NoSuchMethodException ignored) {}

            // (Config)
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor(Config.class);
                ctor.setAccessible(true);
                return (ContextCommand) ctor.newInstance(client.getConfig());
            } catch (NoSuchMethodException ignored) {}

            // ()
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                return (ContextCommand) ctor.newInstance();
            } catch (NoSuchMethodException ignored) {}

            Logger.error("[CommandLoader] No supported constructor found for " + className);
            return null;
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to instantiate context command " + className + ": " + t.getMessage(), t);
            return null;
        }
    }

}