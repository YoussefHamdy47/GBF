package org.bunnys.handler.utils.handler.logging;

@SuppressWarnings("unused")
public class TimestampUtils {
    /**
     * Enum representing Discord UNIX timestamp format types
     */
    public enum UnixFormat {
        d, // Short date (e.g., "2025-07-17")
        D, // Long date (e.g., "July 17, 2025")
        t, // Short time (e.g., "12:00 AM")
        T, // Long time (e.g., "12:00:00 AM")
        f, // Short date and time (e.g., "Jul 17, 2025 12:00 AM")
        F, // Long date and time (e.g., "July 17, 2025 12:00:00 AM")
        R // Relative time (e.g., "5 minutes ago")
    }

    /**
     * Converts a given date to a formatted Discord UNIX timestamp string
     *
     * @param date The date to be converted
     * @param type The format type for the UNIX timestamp
     * @return The formatted Discord UNIX timestamp string (e.g.,
     *         "<t:1234567890:R>"), or null if the type is invalid
     *         <p>
     *         Format types:
     *         - d: Short date (e.g., "2026-05-26")
     *         - D: Long date (e.g., "May 26, 2026")
     *         - t: Short time (e.g., "12:00 AM")
     *         - T: Long time (e.g., "12:00:00 AM")
     *         - f: Short date and time (e.g., "Jan 30, 2025 12:00 AM")
     *         - F: Long date and time (e.g., "January 30, 2025 12:00:00 AM")
     *         - R: Relative time (e.g., "5 minutes ago")
     */
    public static String getTimestamp(java.util.Date date, UnixFormat type) {
        long unixTimestamp = date.getTime() / 1000;
        return String.format("<t:%d:%s>", unixTimestamp, type.name());
    }

    /**
     * Converts a millisecond timestamp to a Unix timestamp (seconds)
     *
     * @param millis The timestamp in milliseconds since the epoch
     * @return The Unix timestamp in seconds
     */
    public static long toUnixTimestamp(long millis) {
        return millis / 1000;
    }

    /**
     * Gets the current Unix timestamp in seconds
     *
     * @return The current Unix timestamp in seconds
     */
    public static long getCurrentUnixTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Converts a Unix timestamp (seconds) to milliseconds
     *
     * @param unixTimestamp The Unix timestamp in seconds
     * @return The timestamp in milliseconds
     */
    public static long toMillis(long unixTimestamp) {
        return unixTimestamp * 1000;
    }

    /**
     * Calculates a future Unix timestamp based on the current time plus a duration
     *
     * @param secondsFromNow The number of seconds from now
     * @return The future Unix timestamp in seconds
     */
    public static long getFutureUnixTimestamp(long secondsFromNow) {
        return getCurrentUnixTimestamp() + secondsFromNow;
    }
}