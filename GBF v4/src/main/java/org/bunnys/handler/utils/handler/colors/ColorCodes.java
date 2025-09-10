package org.bunnys.handler.utils.handler.colors;

import java.awt.*;

/**
 * Centralized color palette for embeds and UI consistency across the bot
 * <p>
 * All colors are defined as immutable {@link java.awt.Color} constants
 * Use these instead of hardcoding values to maintain a unified theme
 */
@SuppressWarnings("unused")
public final class ColorCodes {

    private ColorCodes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Core Brand / Defaults
    public static final Color DEFAULT = new Color(0xE91E63);   // Default / Primary (Pink)

    // Status Colors
    public static final Color ERROR_RED = new Color(0xFF0000);   // Critical / Error
    public static final Color SUCCESS_GREEN = new Color(0x33A532); // Success / Confirmation

    // Extended Reds / Variants
    public static final Color SALMON_PINK = new Color(0xFF91A4);  // Soft Red / Pinkish
    public static final Color CARDINAL_RED = new Color(0xC41E3A); // Deep Strong Red
    public static final Color CHERRY = new Color(0xD2042D);       // Vivid Cherry Red
    public static final Color PASTEL_RED = new Color(0xFAA0A0);   // Light Pastel Red

    // Other Common Colors
    public static final Color CYAN = new Color(0x00FFFF);       // Bright Cyan
    public static final Color YELLOW = new Color(0xF1C40F);     // Warning / Attention
    public static final Color BLUE = new Color(0x3498DB);       // Informational
    public static final Color PURPLE = new Color(0x9B59B6);     // Neutral / Misc
    public static final Color TEAL = new Color(0x1ABC9C);       // Alternative Info
    public static final Color GREY = new Color(0x95A5A6);       // Disabled / Neutral
    public static final Color ORANGE = new Color(0xE67E22);     // Alerts / Highlight
}
