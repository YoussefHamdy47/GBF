package org.bunnys.handler.events;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.util.List;
import java.util.Objects;

/**
 * A utility class for dynamically loading event handlers from a specified
 * package
 * <p>
 * This loader uses the ClassGraph library to discover and instantiate classes
 * that implement
 * the {@link Event} interface It's designed to be efficient and safe for use in
 * concurrent or sharded environments, prioritizing determinism and robust error
 * handling
 * Each event class is expected to provide a public constructor that accepts a
 * {@link BunnyNexus} instance for dependency injection
 * </p>
 *
 * @author Bunny
 * @version 4.0
 */
public final class EventLoader {
    private final String eventPackage;
    private final BunnyNexus client;

    /**
     * Constructs an {@code EventLoader}
     *
     * @param eventPackage The base package to scan for event classes
     * @param client       The {@link BunnyNexus} client instance to be injected
     *                     into event handlers
     */
    public EventLoader(String eventPackage, BunnyNexus client) {
        this.eventPackage = eventPackage;
        this.client = Objects.requireNonNull(client, "Client cannot be null");
    }

    /**
     * Scans the configured package and loads all concrete event handler classes
     * <p>
     * The process involves:
     * <ul>
     * <li>Scanning for classes that implement the {@link Event} interface</li>
     * <li>Sorting classes by simple name for predictable loading order</li>
     * <li>Instantiating each class using its constructor that takes a
     * {@link BunnyNexus} argument</li>
     * <li>Filtering out any classes that fail to instantiate due to missing
     * constructors or other exceptions</li>
     * </ul>
     * This method is thread-safe and designed to return a consistent list of event
     * handlers
     * </p>
     *
     * @return An unmodifiable list of instantiated {@link Event} objects
     */
    public List<Event> loadEvents() {
        Logger.debug(() -> "[EventLoader] Scanning package: " + eventPackage);

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(eventPackage)
                .scan()) {

            var loaded = scanResult.getClassesImplementing(Event.class.getName())
                    .stream() // sequential: predictable, cheaper
                    .sorted(java.util.Comparator.comparing(ClassInfo::getSimpleName))
                    .map(ci -> {
                        try {
                            Class<?> clazz = ci.loadClass();
                            var ctor = clazz.getDeclaredConstructor(BunnyNexus.class); // require public(BunnyNexus)
                            return (Event) ctor.newInstance(client);
                        } catch (NoSuchMethodException e) {
                            Logger.error("[EventLoader] Missing constructor(BunnyNexus) in " + ci.getName(), e);
                            return null;
                        } catch (Exception e) {
                            Logger.error("[EventLoader] Failed to load " + ci.getName(), e);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();

            Logger.debug(() -> "[EventLoader] Total events loaded: " + loaded.size());
            return loaded;
        }
    }
}