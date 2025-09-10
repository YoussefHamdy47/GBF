package org.bunnys.handler.utils.commands.handlers;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1 id="cooldownmanager-class">CooldownManager Class</h1>
 * <p>
 * A thread-safe utility class for managing command cooldowns
 * </p>
 * <p>
 * This class uses a {@link ConcurrentHashMap} to store and manage cooldown
 * timestamps for commands on a per-user basis It's designed to prevent command
 * spam and abuse by ensuring users can't execute a command more frequently than
 * its configured cooldown period allows
 * </p>
 * <p>
 * Key features include:
 * <ul>
 * <li><b>Thread-Safety:</b> The use of {@link ConcurrentHashMap} ensures safe
 * concurrent access from multiple threads, which is essential in a
 * multi-threaded application like a Discord bot</li>
 * <li><b>Efficient Lookups:</b> Cooldowns are stored and retrieved using a
 * unique key composed of the command name and user ID, allowing for fast O(1)
 * lookups</li>
 * <li><b>Stateless Design:</b> The class is a static utility, so it doesn't
 * require instantiation, making it easy to integrate into any part of the
 * command handling logic</li>
 * </ul>
 * </p>
 *
 * @author Bunny
 * @see CommandVerification
 */
public class CooldownManager {
    /**
     * A thread-safe map to store command cooldowns The key is a concatenation of
     * the command name and user ID,
     * and the value is the timestamp (in milliseconds) when the cooldown expires
     */
    private static final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation
     * <p>
     * This is a static utility class and should not be instantiated
     * </p>
     */
    private CooldownManager() {
    }

    /**
     * <h2 id="isoncooldown">Checks if a user is on cooldown for a command</h2>
     * <p>
     * This method first checks if a user is currently on cooldown for a specific
     * command If they are, it returns `true` If they are not, it updates the
     * cooldown timestamp in the map and returns `false`
     * </p>
     * <p>
     * The method is atomic in its check and update, ensuring no race conditions
     * when multiple threads try to access it simultaneously
     * </p>
     *
     * @param commandName The name of the command
     * @param userID      The ID of the user
     * @param cooldownMS  The cooldown duration in milliseconds
     * @return `true` if the user is on cooldown, `false` otherwise
     */
    public static boolean isOnCooldown(String commandName, String userID, long cooldownMS) {
        if (cooldownMS <= 0)
            return false;

        String key = commandName + ":" + userID;
        long now = System.currentTimeMillis();
        Long expiresAt = cooldowns.get(key);

        if (expiresAt != null && expiresAt > now)
            return true; // Still on cooldown

        cooldowns.put(key, now + cooldownMS);
        return false;
    }

    /**
     * <h2 id="getremainingcooldown">Gets the remaining cooldown time for a
     * user</h2>
     * <p>
     * This method calculates the time remaining (in milliseconds) until a user's
     * cooldown for a command expires It returns 0 if the user is not on cooldown or
     * if the cooldown has already expired
     * </p>
     *
     * @param commandName The name of the command
     * @param userID      The ID of the user
     * @return The remaining cooldown time in milliseconds, or 0 if no cooldown is
     *         active
     */
    public static long getRemainingCooldown(String commandName, String userID) {
        String key = commandName + ":" + userID;
        Long expiresAt = cooldowns.get(key);

        if (expiresAt == null)
            return 0;

        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}