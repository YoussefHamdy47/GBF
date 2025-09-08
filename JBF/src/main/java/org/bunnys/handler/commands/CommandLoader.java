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

@SuppressWarnings("unused")
public record CommandLoader(String basePackage, BunnyNexus client) {
    private static final int MAX_THREADS = 8;
    private static final Constructor<?>[] EMPTY_CONSTRUCTORS = new Constructor[0];

    public CommandLoader(String basePackage, BunnyNexus client) {
        this.basePackage = basePackage;
        this.client = Objects.requireNonNull(client, "Client cannot be null");
    }

    public List<MessageCommand> loadMessageCommands() {
        return loadCommands(MessageCommand.class, this::instantiateMessageSafely);
    }

    public List<SlashCommand> loadSlashCommands() {
        List<SlashCommand> commands = loadCommands(SlashCommand.class, this::instantiateSlashSafely);
        return deduplicateCommands(commands, cmd -> cmd.initAndGetConfig().name());
    }

    public List<ContextCommand> loadContextCommands() {
        List<ContextCommand> commands = loadCommands(ContextCommand.class, this::instantiateContextSafely);
        return deduplicateCommands(commands, cmd -> cmd.initAndGetConfig().name());
    }

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

    private MessageCommand instantiateMessageSafely(String className) {
        return instantiateSafely(className, MessageCommand.class, this::tryMessageConstructors);
    }

    private SlashCommand instantiateSlashSafely(String className) {
        return instantiateSafely(className, SlashCommand.class, this::trySlashConstructors);
    }

    private ContextCommand instantiateContextSafely(String className) {
        return instantiateSafely(className, ContextCommand.class, this::tryContextConstructors);
    }

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

    private <T> T tryContextConstructors(Class<?> clazz) {
        return tryMessageConstructors(clazz); // Same constructor patterns as message commands
    }

    private Constructor<?> getConstructor(Class<?> clazz, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}