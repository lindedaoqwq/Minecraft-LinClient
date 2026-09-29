package com.lindedaoqwq.linclient.util;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;

/**
 * FontRenderer that loads a TrueType font from the mod's assets
 * ({@code assets/linclient/font/harmonyos_black.ttf}).
 */
public class UnicodeFontRenderer extends FontRenderer {

    public UnicodeFontRenderer(GameSettings settings, ResourceLocation font, boolean antiAlias) {
        super(settings, font, MinecraftHolder.textureManager(), antiAlias);
    }

    /** Kept separate so the constructor call stays readable. */
    private static class MinecraftHolder {
        static net.minecraft.client.renderer.texture.TextureManager textureManager() {
            return net.minecraft.client.Minecraft.getMinecraft().getTextureManager();
        }
    }
}
