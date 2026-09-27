package com.lindedaoqwq.linclient.util;

/**
 * Small rendering helpers (colour parsing / alpha). All colours are 0xRRGGBB unless noted.
 */
public final class RenderUtils {
    private RenderUtils() {}

    /** Parse an RRGGBB hex string (with or without leading #) into a 0xRRGGBB int. */
    public static int parseColor(String hex) {
        if (hex == null) return 0xFFFFFF;
        try {
            return (int) Long.parseLong(hex.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    /** Bake an opacity (0..1) into the alpha channel of a 0xRRGGBB colour -> 0xAARRGGBB. */
    public static int withAlpha(int rgb, double opacity) {
        int a = (int) (Math.max(0.0, Math.min(1.0, opacity)) * 255.0) & 0xFF;
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
