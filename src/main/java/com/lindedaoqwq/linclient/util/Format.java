package com.lindedaoqwq.linclient.util;

import java.util.Locale;

/**
 * Number / duration formatting helpers.
 */
public final class Format {
    private Format() {}

    public static String fmt(double v) {
        return String.format(Locale.ENGLISH, "%.1f", v);
    }

    /** Format a tick count (20 ticks = 1 second) as mm:ss. */
    public static String durationTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format(Locale.ENGLISH, "%02d:%02d", m, s);
    }
}
