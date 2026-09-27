package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.LayoutConfig;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import com.lindedaoqwq.linclient.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every HUD information module.
 *
 * Subclasses only implement {@link #getLines(Minecraft)} returning the text lines to draw.
 * Position, scale, opacity, colour, order and visibility are handled here from the JSON layout.
 */
public abstract class HudModule {
    public final String id;
    public final String titleKey;

    protected HudModule(String id, String titleKey) {
        this.id = id;
        this.titleKey = titleKey;
    }

    /** The text lines (already translated) to render, excluding the title. */
    public abstract List<String> getLines(Minecraft mc);

    /** Whether this module should currently render. */
    public final boolean isEnabled() {
        LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(id);
        return ml.enabled;
    }

    public final void render(GuiGraphics g, Font font, Minecraft mc, int screenW, int screenH) {
        LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(id);
        if (!ml.enabled) return;

        List<String> lines = getLines(mc);
        if (lines.isEmpty()) return;

        Rect r = computeRect(mc, screenW, screenH, lines);
        int textColor = ml.color;

        if (ModConfig.BACKGROUND_ENABLED.get() && ml.background) {
            int bg = RenderUtils.parseColor(ModConfig.BACKGROUND_COLOR.get());
            g.fill(r.x, r.y, r.x + r.w, r.y + r.h, RenderUtils.withAlpha(bg, ModConfig.BACKGROUND_OPACITY.get()));
        }

        g.pose().pushPose();
        g.pose().translate(r.x, r.y, 0.0);
        g.pose().scale(ml.scale, ml.scale, 1.0f);

        int lineH = font.lineHeight + 2;
        int y = 2;
        g.drawString(font, Component.literal(I18n.t(titleKey)), 4, y, RenderUtils.withAlpha(textColor, ml.opacity), false);
        y += lineH;
        for (String line : lines) {
            g.drawString(font, Component.literal(line), 4, y, RenderUtils.withAlpha(textColor, ml.opacity), false);
            y += lineH;
        }
        g.pose().popPose();
    }

    /** Compute the on-screen rectangle (in GUI-scaled pixels) for this module. */
    public final Rect computeRect(Minecraft mc, int screenW, int screenH, List<String> lines) {
        LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(id);
        int maxW = mc.font.width(Component.literal(I18n.t(titleKey)));
        for (String l : lines) maxW = Math.max(maxW, mc.font.width(Component.literal(l)));
        int lineH = mc.font.lineHeight + 2;
        int h = (lines.size() + 1) * lineH + 4;
        int w = maxW + 8;
        int x = ml.x;
        int y = ml.y;
        if (ml.alignRight) x = screenW - (int) (w * ml.scale) - ml.x;
        return new Rect(x, y, (int) (w * ml.scale), (int) (h * ml.scale));
    }

    /** Convenience for subclasses: an empty, mutable list. */
    protected static List<String> lines() {
        return new ArrayList<>();
    }
}
