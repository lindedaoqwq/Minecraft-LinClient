package com.lindedaoqwq.linclient.util;

import net.minecraft.client.gui.Gui;

public class RenderUtils {
    public static int parseColor(String hex) {
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    /** Build an ARGB color from an RGB int and an alpha in [0,1]. */
    public static int withAlpha(int rgb, float a) {
        int alpha = (int) (Math.max(0f, Math.min(1f, a)) * 255f) & 0xFF;
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static void drawRect(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + h, color);
    }
}
