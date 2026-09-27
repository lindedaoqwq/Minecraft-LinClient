package com.lindedaoqwq.linclient.util;

import net.minecraft.client.resources.I18n;

public class I18n {
    public static String t(String key, Object... args) {
        return net.minecraft.client.resources.I18n.format(key, args);
    }
}
