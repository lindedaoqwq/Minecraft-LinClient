package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import com.lindedaoqwq.linclient.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every HUD information module.
 *
 * Subclasses implement {@link #getLines(Minecraft)} returning the text lines to draw.
 * Position / alignment / colour are handled here; the config menu toggles module visibility.
 */
public abstract class HudModule {
    public final String id;
    public final String titleKey;
    public final boolean alignRight;
    public final int color;

    protected HudModule(String id, String titleKey, boolean alignRight, int color) {
        this.id = id;
        this.titleKey = titleKey;
        this.alignRight = alignRight;
        this.color = color;
    }

    public abstract List<String> getLines(Minecraft mc);

    public final boolean isEnabled() {
        switch (id) {
            case "self_status": return ModConfig.modSelf;
            case "environment": return ModConfig.modEnvironment;
            case "item": return ModConfig.modItem;
            case "input": return ModConfig.modInput;
            case "other": return ModConfig.modOther;
            case "entity": return ModConfig.modEntity;
            case "combat": return ModConfig.modCombat;
            case "boss": return ModConfig.modBoss;
            default: return true;
        }
    }

    private static final int PAD = 4;
    private static final int LINE_H = 10;

    protected static List<String> lines() {
        return new ArrayList<>();
    }

    public Rect computeRect(Minecraft mc, int screenW, int screenH, List<String> lines) {
        FontRenderer fr = mc.fontRenderer;
        int maxW = fr.getStringWidth(I18n.t(titleKey));
        for (String l : lines) maxW = Math.max(maxW, fr.getStringWidth(l));
        int h = (lines.size() + 1) * LINE_H + 4;
        int w = maxW + 8;
        int[] pos = defaultPos(id);
        int x = pos[0];
        int y = pos[1];
        if (alignRight) x = screenW - w - PAD;
        return new Rect(x, y, w, h);
    }

    private static int[] defaultPos(String id) {
        switch (id) {
            case "self_status": return new int[]{4, 4};
            case "environment": return new int[]{4, 130};
            case "item": return new int[]{4, 300};
            case "input": return new int[]{4, 420};
            case "other": return new int[]{4, 520};
            case "entity": return new int[]{4, 4};
            case "combat": return new int[]{4, 170};
            case "boss": return new int[]{4, 320};
            default: return new int[]{4, 4};
        }
    }

    public final void render(Minecraft mc, int screenW, int screenH) {
        if (!isEnabled()) return;
        List<String> lns = getLines(mc);
        if (lns.isEmpty()) return;
        FontRenderer fr = mc.fontRenderer;
        Rect r = computeRect(mc, screenW, screenH, lns);

        RenderUtils.drawRect(r.x, r.y, r.w, r.h, RenderUtils.withAlpha(0x000000, 0.35f));

        int y = r.y + 2;
        fr.drawString(I18n.t(titleKey), r.x + 4, y, color);
        y += LINE_H;
        for (String l : lns) {
            fr.drawString(l, r.x + 4, y, color);
            y += LINE_H;
        }
    }
}
