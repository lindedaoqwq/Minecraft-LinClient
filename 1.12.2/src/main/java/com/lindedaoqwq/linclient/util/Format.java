package com.lindedaoqwq.linclient.util;

public class Format {
    public static String f(double v) {
        return String.format("%.1f", v);
    }

    public static String f2(double v) {
        return String.format("%.2f", v);
    }

    public static String pos(double v) {
        return String.valueOf((int) Math.floor(v));
    }
}
