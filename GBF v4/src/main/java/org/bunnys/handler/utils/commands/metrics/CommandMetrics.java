package org.bunnys.handler.utils.commands.metrics;

import org.bunnys.handler.utils.handler.logging.Logger;
import org.bunnys.handler.utils.commands.handlers.CommandVerification;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1 id="commandmetrics-class">CommandMetrics Class</h1>
 * <p>
 * A utility class for collecting and recording command execution metrics
 * </p>
 * <p>
 * This class provides a centralized, thread-safe mechanism to track various
 * statistics about command usage, including the number of attempts, successes,
 * failures, and execution errors. It uses {@link ConcurrentHashMap}s to ensure
 * high-performance, concurrent access without race conditions, making it
 * suitable for a multithreaded bot environment
 * </p>
 * <p>
 * Key features include:
 * <ul>
 * <li><b>Thread-Safe Counters:</b> The use of `ConcurrentHashMap.merge` ensures
 * atomic updates to metric counters</li>
 * <li><b>Detailed Failure Tracking:</b> Failures are categorized by type,
 * providing granular insights into why commands fail (e.g., missing
 * permissions, cooldowns)</li>
 * <li><b>Performance Monitoring:</b> A simple logging mechanism flags commands
 * that exceed a certain execution time threshold, which is useful for
 * identifying performance bottlenecks</li>
 * </ul>
 * </p>
 *
 * @author Bunny
 * @see CommandVerification
 */
public class CommandMetrics {
    /**
     * A thread-safe map to store the total number of command attempts
     * The key is a combination of command name and user ID
     */
    private final ConcurrentHashMap<String, Long> attempts = new ConcurrentHashMap<>();
    /**
     * A thread-safe map to store the total number of successful command executions
     */
    private final ConcurrentHashMap<String, Long> successes = new ConcurrentHashMap<>();
    /**
     * A thread-safe map to store the total number of command failures, categorized
     * by failure type
     */
    private final ConcurrentHashMap<String, Long> failures = new ConcurrentHashMap<>();

    /**
     * <h2 id="recordcommandattempt">Records a command attempt</h2>
     * <p>
     * Increments the attempt counter for a specific command and user
     * </p>
     *
     * @param cmd    The name of the command
     * @param userId The ID of the user who attempted the command
     */
    public void recordCommandAttempt(String cmd, String userId) {
        attempts.merge(key(cmd, userId), 1L, Long::sum);
    }

    /**
     * <h2 id="recordcommandsuccess">Records a successful command execution</h2>
     * <p>
     * Increments the success counter for a specific command and user
     * </p>
     *
     * @param cmd    The name of the command
     * @param userId The ID of the user who successfully executed the command
     */
    public void recordCommandSuccess(String cmd, String userId) {
        successes.merge(key(cmd, userId), 1L, Long::sum);
    }

    /**
     * <h2 id="recordcommandfailure">Records a command failure</h2>
     * <p>
     * Increments the failure counter, categorized by the specific type of
     * validation failure
     * </p>
     *
     * @param cmd    The name of the command
     * @param userId The ID of the user
     * @param type   The {@link CommandVerification.ValidationFailureType} that
     *               caused the failure
     */
    public void recordCommandFailure(String cmd, String userId,
            CommandVerification.ValidationFailureType type) {
        failures.merge(key(cmd, userId) + ":" + type, 1L, Long::sum);
    }

    /**
     * <h2 id="recordcommanderror">Records a general command error</h2>
     * <p>
     * This method is used for recording unexpected exceptions or runtime errors
     * during command execution, distinct from validation failures
     * </p>
     *
     * @param cmd    The name of the command
     * @param userId The ID of the user
     * @param ex     The {@link Throwable} that represents the error
     */
    public void recordCommandError(String cmd, String userId, Throwable ex) {
        failures.merge(key(cmd, userId) + ":ERROR", 1L, Long::sum);
    }

    /**
     * <h2 id="recordexecutiontime">Records command execution time</h2>
     * <p>
     * Logs a warning if the command's execution time exceeds a predefined
     * threshold, which helps in performance analysis
     * </p>
     *
     * @param cmd The name of the command
     * @param ms  The execution time in milliseconds
     */
    public void recordExecutionTime(String cmd, long ms) {
        if (ms > 5000)
            Logger.warning("[Perf] Command '" + cmd + "' took " + ms + "ms");
    }

    /**
     * <h3 id="key">Generates a unique key for a command and user</h3>
     * <p>
     * A private helper method to create a consistent, concatenated key for map
     * lookups
     * </p>
     *
     * @param cmd    The command name
     * @param userId The user ID
     * @return The concatenated key string
     */
    private String key(String cmd, String userId) {
        return cmd + ":" + userId;
    }
}