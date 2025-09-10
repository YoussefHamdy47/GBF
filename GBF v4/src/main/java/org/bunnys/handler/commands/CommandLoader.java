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
import java.util.function.Function;

/**
 * A utility for dynamically loading command classes from a package
 *
 * <p>
 * This record provides a powerful, multi-threaded mechanism for discovering,
 * instantiating, and deduplicating various command types. It uses
 * {@link ClassGraph} to scan the classpath for command implementations, and a
 * dedicated thread pool to safely instantiate them. It supports different
 * constructor patterns for each command type to maximize flexibility
 * </p>
 *
 * @param basePackage The root package to scan for command classes
 * @param client      The central {@link BunnyNexus} client instance
 */
@SuppressWarnings("unused")
public record CommandLoader(String basePackage, BunnyNexus client) {
    /** The maximum number of threads to use for command instantiation */
    private static final int MAX_THREADS = 8;
    /** A shared empty array to avoid creating new ones */
    private static final Constructor<?>[] EMPTY_CONSTRUCTORS = new Constructor[0];

    /**
     * Constructs a {@code CommandLoader}
     *
     * @param basePackage The root package to scan
     * @param client      The {@link BunnyNexus} client instance
     * @throws NullPointerException if the client is null
     */
    public CommandLoader(String basePackage, BunnyNexus client) {
        this.basePackage = basePackage;
        this.client = Objects.requireNonNull(client, "Client cannot be null");
    }

    /**
     * Loads all {@link MessageCommand} implementations
     *
     * @return A list of instantiated message commands
     */
    public List<MessageCommand> loadMessageCommands() {
        return loadCommands(MessageCommand.class, this::instantiateMessageSafely);
    }

    /**
     * Loads all {@link SlashCommand} implementations, with deduplication
     *
     * <p>
     * Duplicate commands (same name) are logged and skipped
     * </p>
     *
     * @return A list of unique, instantiated slash commands
     */
    public List<SlashCommand> loadSlashCommands() {
        List<SlashCommand> commands = loadCommands(SlashCommand.class, this::instantiateSlashSafely);
        return deduplicateCommands(commands, cmd -> cmd.initAndGetConfig().name());
    }

    /**
     * Loads all {@link ContextCommand} implementations, with deduplication
     *
     * <p>
     * Duplicate commands (same name) are logged and skipped
     * </p>
     *
     * @return A list of unique, instantiated context commands
     */
    public List<ContextCommand> loadContextCommands() {
        List<ContextCommand> commands = loadCommands(ContextCommand.class, this::instantiateContextSafely);
        return deduplicateCommands(commands, cmd -> cmd.initAndGetConfig().name());
    }

    /**
     * The generic command loading and instantiation logic
     *
     * @param commandType  The class type of the commands to load
     * @param instantiator The function to safely instantiate a command from its
     *                     class name
     * @param <T>          The command type
     * @return A list of instantiated commands
     */
    private <T> List<T> loadCommands(Class<T> commandType, Function<String, T> instantiator) {
        if (basePackage == null || basePackage.isBlank()) {
            return List.of();
        }

        List<String> classNames = scanForSubclasses(commandType);
        if (classNames.isEmpty()) {
            Logger.debug(
                    () -> "[CommandLoader] No " + commandType.getSimpleName() + " subclasses found in " + basePackage);
            return List.of();
        }

        return instantiateCommands(classNames, instantiator, commandType.getSimpleName());
    }

    /**
     * Scans the classpath for subclasses of a given type
     *
     * @param commandType The base class or interface to scan for
     * @param <T>         The command type
     * @return A list of class names that are subclasses of the given type
     */
    private <T> List<String> scanForSubclasses(Class<T> commandType) {
        List<String> classNames = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(basePackage)
                .scan()) {

            scanResult.getSubclasses(commandType.getName())
                    .forEach(classInfo -> {
                        try {
                            classNames.add(classInfo.getName());
                        } catch (Exception e) {
                            Logger.error("[CommandLoader] Failed to read class name: " + classInfo, e);
                        }
                    });
        } catch (Exception e) {
            Logger.error("[CommandLoader] ClassGraph scan failed for package: " + basePackage, e);
        }

        return classNames;
    }

    /**
     * Instantiates commands in a multi-threaded, concurrent fashion
     *
     * <p>
     * This method creates a fixed thread pool to parallelize command instantiation,
     * which can significantly improve startup time for applications with many
     * commands
     * </p>
     *
     * @param classNames   A list of class names to instantiate
     * @param instantiator The function to safely instantiate a command
     * @param commandType  The name of the command type for logging
     * @param <T>          The command type
     * @return A list of instantiated commands
     */
    private <T> List<T> instantiateCommands(List<String> classNames, Function<String, T> instantiator,
            String commandType) {
        int threads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), MAX_THREADS));
        ExecutorService executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(commandType.toLowerCase() + "-loader-worker");
            return t;
        });

        try {
            List<Future<T>> futures = classNames.stream()
                    .map(className -> executor.submit(() -> instantiator.apply(className)))
                    .toList();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                try {
                    T command = future.get();
                    if (command != null) {
                        results.add(command);
                    }
                } catch (ExecutionException ee) {
                    Logger.error("[CommandLoader] " + commandType + " instantiation task threw",
                            ee.getCause() != null ? ee.getCause() : ee);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    Logger.error("[CommandLoader] Interrupted while loading " + commandType.toLowerCase() + " commands",
                            ie);
                    break;
                }
            }

            Logger.debug(() -> "[CommandLoader] Instantiated " + results.size() + " " + commandType.toLowerCase()
                    + " command(s)");
            return results;

        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Deduplicates a list of commands based on their canonical name
     *
     * <p>
     * If two commands have the same name, a warning is logged and the duplicate
     * is ignored in favor of the first one found
     * </p>
     *
     * @param commands      The list of commands to deduplicate
     * @param nameExtractor A function to extract the name from a command instance
     * @param <T>           The command type
     * @return A list of commands with unique names
     */
    private <T> List<T> deduplicateCommands(List<T> commands, Function<T, String> nameExtractor) {
        Map<String, T> deduped = new LinkedHashMap<>();

        for (T command : commands) {
            try {
                String canonical = CommandRegistry.canonical(nameExtractor.apply(command));
                Logger.debug(() -> "[CommandLoader] Preparing to register: "
                        + command.getClass().getName() + " as '" + canonical + "'");

                if (deduped.containsKey(canonical)) {
                    Logger.error("[CommandLoader] Duplicate command name detected: " + canonical
                            + " (first: " + deduped.get(canonical).getClass().getName()
                            + ", second: " + command.getClass().getName() + ")");
                    continue;
                }
                deduped.put(canonical, command);
            } catch (Exception e) {
                Logger.error("[CommandLoader] Failed to read config for command: " + command.getClass().getName(), e);
            }
        }

        Logger.debug(() -> "[CommandLoader] Deduplicated to " + deduped.size() + " unique command(s)");
        return List.copyOf(deduped.values());
    }

    /**
     * Safely instantiates a {@link MessageCommand}
     *
     * @param className The name of the class to instantiate
     * @return An instance of {@link MessageCommand} or null if instantiation fails
     */
    private MessageCommand instantiateMessageSafely(String className) {
        return instantiateSafely(className, MessageCommand.class, this::tryMessageConstructors);
    }

    /**
     * Safely instantiates a {@link SlashCommand}
     *
     * @param className The name of the class to instantiate
     * @return An instance of {@link SlashCommand} or null if instantiation fails
     */
    private SlashCommand instantiateSlashSafely(String className) {
        return instantiateSafely(className, SlashCommand.class, this::trySlashConstructors);
    }

    /**
     * Safely instantiates a {@link ContextCommand}
     *
     * @param className The name of the class to instantiate
     * @return An instance of {@link ContextCommand} or null if instantiation fails
     */
    private ContextCommand instantiateContextSafely(String className) {
        return instantiateSafely(className, ContextCommand.class, this::tryContextConstructors);
    }

    /**
     * The core logic for safely instantiating a command
     *
     * <p>
     * This method handles class loading, basic validation (abstract, interface,
     * etc),
     * and attempts to use a specific constructor strategy to create an instance
     * </p>
     *
     * @param className        The name of the class to instantiate
     * @param expectedType     The expected type of the instance
     * @param constructorTrier A function that attempts to create an instance from a
     *                         class
     * @param <T>              The command type
     * @return An instance of the command or null on failure
     */
    private <T> T instantiateSafely(String className, Class<T> expectedType, Function<Class<?>, T> constructorTrier) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to load class " + className, t);
            return null;
        }

        if (!isInstantiableClass(clazz, expectedType)) {
            return null;
        }

        try {
            return constructorTrier.apply(clazz);
        } catch (Throwable t) {
            Logger.error("[CommandLoader] Failed to instantiate " + className + ": " + t.getMessage(), t);
            return null;
        }
    }

    /**
     * Determines if a class is instantiable
     *
     * @param clazz        The class to check
     * @param expectedType The expected base class or interface
     * @param <T>          The command type
     * @return True if the class is a concrete implementation of the expected type,
     *         false otherwise
     */
    private <T> boolean isInstantiableClass(Class<?> clazz, Class<T> expectedType) {
        int mods = clazz.getModifiers();
        if (Modifier.isAbstract(mods) || Modifier.isInterface(mods) || clazz.isAnnotation()) {
            return false;
        }

        if (!expectedType.isAssignableFrom(clazz)) {
            Logger.debug(() -> "[CommandLoader] Skipping " + clazz.getName() + " (not a " + expectedType.getSimpleName()
                    + ")");
            return false;
        }

        return true;
    }

    /**
     * Attempts to find and use a supported constructor for message commands
     *
     * <p>
     * This method checks for a series of common constructor patterns (e.g.,
     * with {@code BunnyNexus}, {@code Config}, both, or neither) in a specific
     * order of preference
     * </p>
     *
     * @param clazz The class to instantiate
     * @param <T>   The command type
     * @return A new instance of the command, or null if no supported constructor is
     *         found
     */
    @SuppressWarnings("unchecked")
    private <T> T tryMessageConstructors(Class<?> clazz) {
        Constructor<?>[] constructors = {
                getConstructor(clazz, BunnyNexus.class, Config.class),
                getConstructor(clazz, BunnyNexus.class),
                getConstructor(clazz, Config.class),
                getConstructor(clazz)
        };

        Object[][] paramSets = {
                { client, client.getConfig() },
                { client },
                { client.getConfig() },
                {}
        };

        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> ctor = constructors[i];
            if (ctor != null) {
                try {
                    ctor.setAccessible(true);
                    return (T) ctor.newInstance(paramSets[i]);
                } catch (Exception ignored) {
                }
            }
        }

        Logger.error("[CommandLoader] No supported constructor found for " + clazz.getName());
        return null;
    }

    /**
     * Attempts to find and use a supported constructor for slash commands
     *
     * <p>
     * Slash commands are expected to have a simple no-argument constructor
     * </p>
     *
     * @param clazz The class to instantiate
     * @param <T>   The command type
     * @return A new instance of the command, or null on failure
     */
    @SuppressWarnings("unchecked")
    private <T> T trySlashConstructors(Class<?> clazz) {
        Constructor<?> ctor = getConstructor(clazz);
        if (ctor != null) {
            try {
                ctor.setAccessible(true);
                return (T) ctor.newInstance();
            } catch (Exception e) {
                Logger.error("[CommandLoader] Failed to instantiate slash command " + clazz.getName(), e);
            }
        }
        return null;
    }

    /**
     * Attempts to find and use a supported constructor for context commands
     *
     * <p>
     * Context commands share the same constructor patterns as message commands
     * </p>
     *
     * @param clazz The class to instantiate
     * @param <T>   The command type
     * @return A new instance of the command, or null on failure
     */
    private <T> T tryContextConstructors(Class<?> clazz) {
        return tryMessageConstructors(clazz); // Same constructor patterns as message commands
    }

    /**
     * A utility method to safely retrieve a constructor by its parameter types
     *
     * @param clazz      The class to get the constructor from
     * @param paramTypes The parameter types of the constructor
     * @return The constructor, or null if it does not exist
     */
    private Constructor<?> getConstructor(Class<?> clazz, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}