package org.bunnys.handler.utils.handler;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized emoji registry for reusable Discord custom and animated emojis
 *
 * <p>Automatically registers all public static final String fields via reflection
 * This means adding a new emoji constant is enough, no need to update the map</p>
 *
 * @example
 * public class EmojiTest { <br>
 *     public static void main(String[] args) { <br>
 *         // Direct access <br>
 *         System.out.println("Verify: " + Emojis.DEFAULT_VERIFY); <br>
 *
 *         // Dynamic lookup <br>
 *         System.out.println("From map: " + Emojis.get("gbf_logo")); <br>
 *
 *         // List all <br>
 *         Emojis.all().forEach((k, v) -> <br>
 *             System.out.println(k + " = " + v) <br>
 *         ); <br>
 *     } <br>
 * } <br>
 */
@SuppressWarnings("unused")
public final class Emojis {

    // --- Default Emojis ---
    public static final String DEFAULT_VERIFY = "<:verified:821419611438317638>";
    public static final String DEFAULT_ERROR = "<:error:822091680605011978>";

    // --- Custom Emojis ---
    public static final String SPOTIFY = "<:Spotify:962905037649096815>";
    public static final String GBF_LOGO = "<:LogoTransparent:838994085527945266>";

    // Progress Bar
    public static final String PROGRESS_BAR_LEFT_EMPTY = "<:leftEmpty:1068143435095220265>";
    public static final String PROGRESS_BAR_LEFT_FULL = "<:leftFull:1068143511179894804>";
    public static final String PROGRESS_BAR_RIGHT_EMPTY = "<:rightEmpty:1068143887010517032>";
    public static final String PROGRESS_BAR_RIGHT_FULL = "<:rightFull:1068143806622470244>";
    public static final String PROGRESS_BAR_MIDDLE_EMPTY = "<:middleEmpty:1068143614804377681>";
    public static final String PROGRESS_BAR_MIDDLE_FULL = "<:middleFull:1068143723080319038>";

    // Animated Emojis
    public static final String DIAMOND_SPIN = "<a:DiamondGIF:954506603426635837>";
    public static final String CROWN_ANIMATED = "<a:Crown:1335252412394377250>";
    public static final String BLACK_HEART_SPIN = "<a:blackSpin:1025851052442005594>";
    public static final String WHITE_HEART_SPIN = "<a:whiteSpin:1025851168720687174>";
    public static final String RED_HEART_SPIN = "<a:redSpin:1025851361583173773>";
    public static final String PINK_HEART_SPIN = "<a:pinkSpin:1025851222068052101>";
    public static final String DONUT_SPIN = "<a:donutSpin:1025851417421955204>";

    // Immutable lookup map
    private static final Map<String, String> REGISTRY;

    static {
        Map<String, String> map = new HashMap<>();
        try {
            for (Field field : Emojis.class.getDeclaredFields()) {
                // Only include public static final String fields
                if (field.getType() == String.class &&
                        java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        java.lang.reflect.Modifier.isFinal(field.getModifiers())) {

                    String name = field.getName().toLowerCase(Locale.ROOT); // default key
                    String value = (String) field.get(null);

                    map.put(name, value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize Emojis registry", e);
        }
        REGISTRY = Collections.unmodifiableMap(map);
    }

    private Emojis() {
        throw new UnsupportedOperationException("Utility class, instantiation not allowed.");
    }

    /**
     * Lookup an emoji by its key (case-insensitive)
     *
     * @param key Emoji identifier (e.g. "default_error", "gbf_logo")
     * @return The emoji string if found, otherwise null
     */
    public static String get(String key) {
        if (key == null) return null;
        return REGISTRY.get(key.toLowerCase(Locale.ROOT));
    }

    /**
     * @return Unmodifiable map of all registered emojis
     */
    public static Map<String, String> all() {
        return REGISTRY;
    }
}
