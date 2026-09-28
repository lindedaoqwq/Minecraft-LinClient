package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.util.RenderUtils;

/** Rounded/pill drawing helpers on top of {@link RenderUtils} (fake 1px-rounded corners). */
public class Theme {
    public static final int BG_PANEL   = 0xE61E1E24;
    public static final int BG_ROW     = 0xFF2A2A31;
    public static final int BG_SIDE    = 0xFF222228;
    public static final int BG_SELECT  = 0xFF35353D;
    public static final int ACCENT     = 0xFF3AA6F0;
    public static final int ACCENT_DIM = 0xFF274053;
    public static final int TEXT       = 0xFFFFFFFF;
    public static final int TEXT_DIM   = 0xFF9A9AA5;
    public static final int KNOB_OFF   = 0xFF4A4A55;
    public static final int BORDER     = 0xFF15151A;

    /** Rounded rectangle (two overlapping rects => 1px cut corners). */
    public static void roundRect(int x, int y, int w, int h, int color) {
        RenderUtils.drawRect(x + 1, y, w - 2, h, color);
        RenderUtils.drawRect(x, y + 1, w, h - 2, color);
    }

    /** Rounded rect with a 1px darker border. */
    public static void roundRectBordered(int x, int y, int w, int h, int color, int border) {
        roundRect(x, y, w, h, border);
        roundRect(x + 1, y + 1, w - 2, h - 2, color);
    }

    /** Pill toggle like the reference screenshot: track + knob. */
    public static void pill(int x, int y, int w, int h, boolean on) {
        roundRect(x, y, w, h, on ? ACCENT : KNOB_OFF);
        int k = h - 2;
        int kx = on ? x + w - k - 2 : x + 2;
        roundRect(kx, y + 1, k, k, 0xFFF2F2F2);
    }
}
