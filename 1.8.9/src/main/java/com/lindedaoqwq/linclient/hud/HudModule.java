package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import com.lindedaoqwq.linclient.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.List;

/**
 * Base class for every HUD information module.
 *
 * Subclasses implement {@link #getLines(Minecraft)} returning the text lines to draw. Position is
 * stored per module (x/y) and is edited inside the dedicated HUD layout editor screen. The ClickGUI
 * toggles module visibility.
 */
public abstract class HudModule {
    public final String id;
    public final String titleKey;
    public final int color;
    public int posX = 4, posY = 4;

    protected HudModule(String id, String titleKey, int color) {
        this.id = id;
        this.titleKey = titleKey;
        this.color = color;
    }

    public abstract List<String> getLines(Minecraft mc);

    public final boolean isEnabled() {
        return ModConfig.isModuleOn(id);
    }

    private static final int PAD = 4;
    private static final int LINE_H = 10;
    private static final int TITLE_H = 12;

    protected List<String> lines() {
        return new java.util.ArrayList<>();
    }

    public int contentWidth(Minecraft mc) {
        FontRenderer fr = mc.fontRendererObj;
        int maxW = fr.getStringWidth(I18n.t(titleKey));
        List<String> lns = getLines(mc);
        for (String l : lns) maxW = Math.max(maxW, fr.getStringWidth(l));
        return maxW + 8;
    }

    public int contentHeight(Minecraft mc) {
        List<String> lns = getLines(mc);
        return Math.max(1, lns.size()) * LINE_H + 4;
    }

    public Rect rect(Minecraft mc) {
        int w = contentWidth(mc) + PAD * 2;
        int h = contentHeight(mc) + TITLE_H + PAD;
        return new Rect(posX, posY, w, h);
    }

    public final void render(Minecraft mc) {
        render(mc, false);
    }

    /** Renders the module; when {@code force} is true, disabled modules are dimmed instead of skipped (HUD editor). */
    public void render(Minecraft mc, boolean force) {
        if (!isEnabled() && !force) return;
        FontRenderer fr = mc.fontRendererObj;
        Rect r = rect(mc);
        RenderUtils.drawRect(r.x, r.y, r.w, r.h, RenderUtils.withAlpha(0x000000, 0.35f));
        fr.drawString(I18n.t(titleKey), r.x + PAD, r.y + 2, color);
        drawContent(mc, fr, r.x + PAD, r.y + TITLE_H, r.w - PAD * 2, r.h - TITLE_H - PAD);
        if (!isEnabled()) {
            RenderUtils.drawRect(r.x, r.y, r.w, r.h, 0x55000000);
        }
    }

    protected void drawContent(Minecraft mc, FontRenderer fr, int x, int y, int w, int h) {
        List<String> lns = getLines(mc);
        int yy = y;
        for (String l : lns) {
            fr.drawString(l, x, yy, color);
            yy += LINE_H;
        }
    }

    public boolean contains(int mx, int my) {
        Rect r = rect(Minecraft.getMinecraft());
        return mx >= r.x && mx <= r.x + r.w && my >= r.y && my <= r.y + r.h;
    }
}
