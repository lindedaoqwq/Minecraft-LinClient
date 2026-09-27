package com.lindedaoqwq.linclient.util;

public class I18n {
    public static String t(String key, Object... args) {
        return net.minecraft.client.resources.I18n.format(key, args);
    }
}
