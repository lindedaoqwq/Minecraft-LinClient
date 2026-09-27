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

    /** A simple filled panel with a 1px border. */
    public static void drawPanel(int x, int y, int w, int h, int bg, int border) {
        drawRect(x, y, w, h, bg);
        drawRect(x, y, w, 1, border);
        drawRect(x, y + h - 1, w, 1, border);
        drawRect(x, y, 1, h, border);
        drawRect(x + w - 1, y, 1, h, border);
    }

    /** Vertical gradient built from stacked 1px rects (version-safe, no Tessellator use). */
    public static void drawVerticalGradient(int x, int y, int w, int h, int top, int bottom) {
        if (h <= 0) return;
        for (int i = 0; i < h; i++) {
            float t = (float) i / (float) (h - 1);
            drawRect(x, y + i, w, 1, lerpColor(top, bottom, t));
        }
    }

    public static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
