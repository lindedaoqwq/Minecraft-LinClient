package com.lindedaoqwq.linclient.util;

import net.minecraft.network.chat.Component;

/**
 * Thin wrapper around Minecraft's translation component so HUD strings pick up
 * the active language (en_us / zh_cn / ...).
 */
public final class I18n {
    private I18n() {}

    public static String t(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}
