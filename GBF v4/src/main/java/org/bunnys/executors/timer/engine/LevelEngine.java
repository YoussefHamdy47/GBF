package org.bunnys.executors.timer.engine;

import org.bunnys.handler.utils.handler.Emojis;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized leveling and ranking system with caching and improved algorithms
 * Thread-safe with performance optimizations for high-frequency calculations
 */
@SuppressWarnings("unused")
public final class LevelEngine {
    private static final int MAX_RANK = 5000;
    private static final double XP_BASE = 100.0;
    private static final double RP_BASE = 200.0;
    private static final double POWER_EXPONENT = 1.3;
    private static final int XP_PER_5_MINUTES = 180;
    private static final int MINUTES_PER_XP_CYCLE = 5;

    // Pre-computed caches for frequently accessed values
    private static final ConcurrentHashMap<Integer, Integer> XP_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Integer> RP_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Integer> TOTAL_XP_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Integer> TOTAL_RP_CACHE = new ConcurrentHashMap<>();

    // Emoji ranges as immutable records
    public record EmojiRange(int min, int max, String emoji) {
    }

    private static final List<EmojiRange> EMOJI_RANGES = List.of(
            new EmojiRange(0, 25, Emojis.DEFAULT_VERIFY),
            new EmojiRange(26, 50, Emojis.BLACK_HEART_SPIN),
            new EmojiRange(51, 100, Emojis.WHITE_HEART_SPIN),
            new EmojiRange(101, 150, Emojis.PINK_HEART_SPIN),
            new EmojiRange(151, 250, Emojis.RED_HEART_SPIN),
            new EmojiRange(251, 1000, Emojis.PINK_CRYSTAL_HEART_SPIN),
            new EmojiRange(1001, MAX_RANK, Emojis.DONUT_SPIN));

    private LevelEngine() {
    } // Utility class

    // Core calculation methods with caching
    public static int xpRequired(int level) {
        if (level <= 1)
            return (int) XP_BASE;
        return XP_CACHE.computeIfAbsent(level,
                k -> (int) (XP_BASE * Math.pow(k, POWER_EXPONENT)));
    }

    public static int rpRequired(int rank) {
        if (rank <= 1)
            return (int) RP_BASE;
        return RP_CACHE.computeIfAbsent(rank,
                k -> (int) (RP_BASE * Math.pow(k, POWER_EXPONENT)));
    }

    public static double hoursRequired(int points) {
        return (double) points * MINUTES_PER_XP_CYCLE / (XP_PER_5_MINUTES * 60.0);
    }

    public static int calculateXP(int minutes) {
        return (minutes / MINUTES_PER_XP_CYCLE) * XP_PER_5_MINUTES;
    }

    // Result classes as records for immutability
    public record LevelResult(boolean hasLeveledUp, int addedLevels, int remainingXP) {
        public static LevelResult noChange(int currentXP) {
            return new LevelResult(false, 0, currentXP);
        }
    }

    public record RankResult(boolean hasRankedUp, int addedRanks, int remainingRP) {
        public static RankResult noChange(int currentRP) {
            return new RankResult(false, 0, currentRP);
        }
    }

    // Optimized progression calculations
    public static LevelResult checkLevel(int currentLevel, int currentXP, int addedXP) {
        if (addedXP == 0 && currentXP < xpRequired(currentLevel + 1)) {
            return LevelResult.noChange(currentXP);
        }

        int totalXP = currentXP + addedXP;
        int newLevel = currentLevel;

        while (newLevel < MAX_RANK) {
            int required = xpRequired(newLevel + 1);
            if (totalXP < required)
                break;

            totalXP -= required;
            newLevel++;
        }

        int levelsGained = newLevel - currentLevel;
        return new LevelResult(levelsGained > 0, levelsGained, totalXP);
    }

    public static RankResult checkRank(int currentRank, int currentRP, int addedRP) {
        if (addedRP == 0 && currentRP < rpRequired(currentRank + 1)) {
            return RankResult.noChange(currentRP);
        }

        int totalRP = currentRP + addedRP;
        int newRank = currentRank;

        while (newRank < MAX_RANK) {
            int required = rpRequired(newRank + 1);
            if (totalRP < required)
                break;

            totalRP -= required;
            newRank++;
        }

        int ranksGained = newRank - currentRank;
        return new RankResult(ranksGained > 0, ranksGained, totalRP);
    }

    // Cached total calculations
    public static int calculateTotalSemesterXP(int level) {
        if (level <= 0)
            return 0;
        return TOTAL_XP_CACHE.computeIfAbsent(level, k -> {
            int total = 0;
            for (int i = 1; i <= k; i++) {
                total += xpRequired(i);
            }
            return total;
        });
    }

    public static int calculateTotalAccountRP(int rank) {
        if (rank <= 0)
            return 0;
        return TOTAL_RP_CACHE.computeIfAbsent(rank, k -> {
            int total = 0;
            for (int i = 1; i <= k; i++) {
                total += rpRequired(i);
            }
            return total;
        });
    }

    // Utility methods
    public static int safePercentage(int value, int required) {
        if (required <= 0)
            return 0;
        return Math.min(100, Math.max(0, Math.round(100.0f * value / required)));
    }

    public static String rankUpEmoji(int level) {
        return EMOJI_RANGES.stream()
                .filter(range -> level >= range.min() && level <= range.max())
                .map(EmojiRange::emoji)
                .findFirst()
                .orElse(Emojis.DEFAULT_VERIFY);
    }

    // Performance monitoring (can be removed in production)
    public static void clearCaches() {
        XP_CACHE.clear();
        RP_CACHE.clear();
        TOTAL_XP_CACHE.clear();
        TOTAL_RP_CACHE.clear();
    }

    public static int getCacheSize() {
        return XP_CACHE.size() + RP_CACHE.size() + TOTAL_XP_CACHE.size() + TOTAL_RP_CACHE.size();
    }
}