package com.lindedaoqwq.linclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Custom TrueType font renderer backed by {@code assets/linclient/font/harmonyos_black.ttf}.
 * Chinese glyphs have no dedicated width entry in the vanilla glyph table, so the raw
 * advance includes a font-metric fudge factor; we correct that for CJK code points.
 */
public class FontUtils {
    public static final ResourceLocation FONT =
            new ResourceLocation("linclient", "font/harmonyos_black.ttf");

    private static FontRenderer renderer = null;
    private static boolean tried = false;

    /** Lazily builds (and caches) the custom font renderer. Null if creation failed. */
    public static FontRenderer font() {
        if (tried) return renderer;
        tried = true;
        try {
            renderer = new UnicodeFontRenderer(Minecraft.getMinecraft().gameSettings, FONT, true);
            // Vanilla uploads the font texture with linear filtering -> blurry glyphs.
            Field f = FontRenderer.class.getDeclaredField("field_111273_g");
            f.setAccessible(true);
            f.setBoolean(renderer, false);
        } catch (Throwable t) {
            renderer = null;
        }
        return renderer;
    }

    /** Custom-font renderer when available, otherwise the vanilla one. */
    public static FontRenderer get() {
        FontRenderer r = font();
        return r != null ? r : Minecraft.getMinecraft().fontRendererObj;
    }
}
