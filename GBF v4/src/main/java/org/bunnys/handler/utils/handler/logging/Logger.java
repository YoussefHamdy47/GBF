package org.bunnys.handler.utils.handler.logging;

import org.bunnys.handler.Config;
import org.bunnys.handler.utils.handler.colors.ConsoleColors;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A centralized, thread-safe logging utility for the BunnyNexus framework.
 *
 * <p>
 * This logger provides a flexible and efficient way to handle application
 * logs. Key features include:
 * <ul>
 * <li><b>Console Output:</b> Displays {@code SUCCESS}, {@code INFO},
 * {@code WARN},
 * and {@code ERROR} messages with color-coded prefixes for easy
 * readability.</li>
 * <li><b>File Output:</b> Writes all log levels, including detailed
 * {@code DEBUG}
 * messages, to a persistent log file at {@code logs/debug.log}.</li>
 * <li><b>Startup Buffering:</b> Messages logged during the initial startup
 * phase
 * are buffered and not printed to the console until explicitly flushed. This
 * prevents a messy console during the bot's initialization process.</li>
 * </ul>
 * The class is designed to be high-performance and thread-safe, ensuring
 * that log messages are handled reliably from any part of the application
 * </p>
 */
@SuppressWarnings("unused")
public final class Logger {

    /** The application's configuration, used to control debug logging */
    private static volatile Config config;
    /** Formatter for log entry timestamps */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** The directory for storing log files */
    private static final String LOG_DIR = "logs";
    /** The file path for debug logs */
    private static final String DEBUG_LOG_FILE = LOG_DIR + "/debug.log";

    /** Color-coded prefixes for console output */
    private static final String ERROR_PREFIX = ConsoleColors.RED + "[ERROR] ";
    private static final String INFO_PREFIX = ConsoleColors.CYAN + "[INFO] ";
    private static final String WARN_PREFIX = ConsoleColors.YELLOW + "[WARN] ";
    private static final String SUCCESS_PREFIX = ConsoleColors.GREEN + "[OK] ";

    /** The writer for the persistent log file */
    private static PrintWriter debugWriter;
    /** A lock object for thread-safe file writing */
    private static final Object DEBUG_LOCK = new Object();

    /** A flag to enable or disable console buffering */
    private static boolean buffering = true;
    /** The list that stores log messages during startup buffering */
    private static final List<String> startupBuffer = new ArrayList<>();

    static {
        try {
            // Initialize the log directory and file writer on class load
            Files.createDirectories(Paths.get(LOG_DIR));

            new PrintWriter(DEBUG_LOG_FILE).close();

            debugWriter = new PrintWriter(new FileWriter(DEBUG_LOG_FILE, true), true);
        } catch (IOException e) {
            System.err.println("[Logger] Failed to initialize debug log file: " + e.getMessage());
        }
    }

    private Logger() {
    }

    /**
     * Attaches the main application configuration to the logger.
     *
     * <p>
     * This is a required setup step to enable or disable debug logging based
     * on the application's configuration
     * </p>
     *
     * @param cfg The configuration instance, cannot be null
     * @throws IllegalArgumentException if the config is null
     */
    public static void attachConfig(Config cfg) {
        if (cfg == null)
            throw new IllegalArgumentException("Config cannot be null");
        config = cfg;
    }

    /**
     * Checks if debug logging is disabled in the current configuration
     *
     * @return {@code true} if debug logging is disabled, {@code false} otherwise
     */
    private static boolean isDebugDisabled() {
        Config currentConfig = config;
        return currentConfig == null || !currentConfig.debug();
    }

    /**
     * Logs a success message to the console and log file
     *
     * @param message The message to log
     */
    public static void success(String message) {
        logConsole(SUCCESS_PREFIX, message, ConsoleColors.GREEN);
        logFile("SUCCESS", message);
    }

    /**
     * Logs an informational message to the console and log file
     *
     * @param message The message to log
     */
    public static void info(String message) {
        logConsole(INFO_PREFIX, message, ConsoleColors.CYAN);
        logFile("INFO", message);
    }

    /**
     * Logs a warning message to the console and log file
     *
     * @param message The message to log
     */
    public static void warning(String message) {
        logConsole(WARN_PREFIX, message, ConsoleColors.YELLOW);
        logFile("WARN", message);
    }

    /**
     * Logs an error message to the console and log file
     *
     * @param message The message to log
     */
    public static void error(String message) {
        logConsole(ERROR_PREFIX, message, ConsoleColors.RED);
        logFile("ERROR", message);
    }

    /**
     * Logs an error message with a stack trace to the console and log file
     *
     * @param message   The message to log
     * @param throwable The exception whose stack trace should be included
     */
    public static void error(String message, Throwable throwable) {
        synchronized (System.out) {
            error(message);
            if (throwable != null)
                throwable.printStackTrace(System.out);
        }
        logFile("ERROR", message + "\n" + getStackTrace(throwable));
    }

    /**
     * Logs a debug message to the log file only, using a supplier for lazy
     * evaluation
     *
     * @param supplier A {@link Supplier} that provides the message string
     */
    public static void debug(Supplier<String> supplier) {
        if (isDebugDisabled())
            return;
        String msg = supplier.get();
        if (msg != null)
            logDebug(msg);
    }

    /**
     * Logs a debug message to the log file only
     *
     * @param message The message to log
     */
    public static void debug(String message) {
        if (isDebugDisabled())
            return;
        logDebug(message);
    }

    /**
     * Logs a debug message with a full stack trace to the log file only
     *
     * @param message The message to log
     */
    public static void debugStackTrace(String message) {
        if (isDebugDisabled())
            return;
        logDebug(message + "\n" + getStackTrace(new Throwable("Stack trace")));
    }

    /**
     * Logs a debug message with a timestamp to the log file only, using a supplier
     * for lazy evaluation
     *
     * @param supplier A {@link Supplier} that provides the message string
     */
    public static void debugWithTimestamp(Supplier<String> supplier) {
        if (isDebugDisabled())
            return;
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String msg = supplier.get();
        if (msg != null)
            logDebug("[" + timestamp + "] " + msg);
    }

    /**
     * Handles logging to the console, respecting the startup buffer flag
     *
     * @param prefix  The color-coded prefix
     * @param message The message to log
     * @param color   The console color
     */
    private static void logConsole(String prefix, String message, String color) {
        if (message == null)
            return;
        String line = prefix + message + ConsoleColors.RESET;

        if (buffering) {
            startupBuffer.add(line);
        } else {
            System.out.println(line);
        }
    }

    /**
     * Writes a log message to the debug log file in a thread-safe manner
     *
     * @param level   The log level (e.g., "INFO", "ERROR")
     * @param message The message to log
     */
    private static void logFile(String level, String message) {
        if (debugWriter == null || message == null)
            return;
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        synchronized (DEBUG_LOCK) {
            debugWriter.println("[" + timestamp + "] [" + level + "] " + message);
        }
    }

    /**
     * A private helper method to log debug messages to the file
     *
     * @param message The message to log
     */
    private static void logDebug(String message) {
        logFile("DEBUG", message);
    }

    /**
     * Converts a stack trace from a throwable into a string
     *
     * @param throwable The throwable to format
     * @return The stack trace as a string
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null)
            return "";
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "Error formatting stack trace: " + e.getMessage();
        }
    }

    /**
     * Checks if debug logging is currently enabled
     *
     * @return {@code true} if debug is enabled, {@code false} otherwise
     */
    public static boolean isDebugEnabled() {
        return !isDebugDisabled();
    }

    /**
     * Flushes all buffered startup logs to the console and disables buffering.
     *
     * <p>
     * This method should be called after the application has completed its
     * initial setup to display all accumulated logs at once
     * </p>
     */
    public static void flushStartupBuffer() {
        buffering = false;
        startupBuffer.forEach(System.out::println);
        startupBuffer.clear();
    }
}