package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.LayoutConfig;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import com.lindedaoqwq.linclient.util.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime HUD editor. Open it with the "Open HUD Editor" key (default G).
 *
 *   - Drag a module to move it
 *   - Mouse wheel over a selected module to scale it
 *   - H toggles the selected module's visibility
 *   - R resets the selected module to defaults
 *   - [ and ] change opacity, = / - change scale
 *   - Arrow keys nudge, ESC closes (and saves)
 *
 * The screen does not pause the game, so the world keeps running behind it.
 */
public class HudEditorScreen extends Screen {
    private String selected = null;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private final Map<String, Rect> rects = new LinkedHashMap<>();

    public HudEditorScreen() {
        super(Component.literal(I18n.t("linclient.editor.title")));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Map<String, Rect> buildRects(Minecraft mc) {
        rects.clear();
        for (HudModule m : HudOverlay.all()) {
            rects.put(m.id, m.computeRect(mc, this.width, this.height, m.getLines(mc)));
        }
        return rects;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        Minecraft mc = this.minecraft;
        if (mc == null) return;
        // Draw the (live) HUD modules underneath.
        for (HudModule m : HudOverlay.all()) {
            m.render(g, this.font, mc, this.width, this.height);
        }

        Map<String, Rect> rs = buildRects(mc);
        for (Map.Entry<String, Rect> e : rs.entrySet()) {
            Rect r = e.getValue();
            boolean isSel = e.getKey().equals(selected);
            int overlay = isSel ? RenderUtils.withAlpha(0xFFFF00, 0.18) : RenderUtils.withAlpha(0xFFFFFF, 0.08);
            g.fill(r.x, r.y, r.x + r.w, r.y + r.h, overlay);
            int border = isSel ? 0xFFFFFF00 : 0x55FFFFFF;
            drawBorder(g, r, border);
            g.drawString(this.font, Component.literal(e.getKey()), r.x, r.y - 10, 0xFFFFFFFF);
        }

        // Help bar
        String help = I18n.t("linclient.editor.help");
        int helpW = this.font.width(Component.literal(help));
        g.fill(0, this.height - 16, this.width, this.height, RenderUtils.withAlpha(0x000000, 0.6));
        g.drawString(this.font, Component.literal(help), (this.width - helpW) / 2, this.height - 12, 0xFFFFFFFF);
    }

    private void drawBorder(GuiGraphics g, Rect r, int color) {
        int t = 1;
        g.fill(r.x, r.y, r.x + r.w, r.y + t, color);
        g.fill(r.x, r.y + r.h - t, r.x + r.w, r.y + r.h, color);
        g.fill(r.x, r.y, r.x + t, r.y + r.h, color);
        g.fill(r.x + r.w - t, r.y, r.x + r.w, r.y + r.h, color);
    }

    private HudModule moduleById(String id) {
        for (HudModule m : HudOverlay.all()) if (m.id.equals(id)) return m;
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Minecraft mc = this.minecraft;
        if (mc == null) return false;
        List<HudModule> reversed = new ArrayList<>(HudOverlay.all());
        java.util.Collections.reverse(reversed);
        for (HudModule m : reversed) {
            Rect r = m.computeRect(mc, this.width, this.height, m.getLines(mc));
            if (r.contains(mx, my)) {
                selected = m.id;
                LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(m.id);
                if (ml.alignRight) {
                    ml.alignRight = false;
                    ml.x = r.x;
                    ml.y = r.y;
                }
                dragOffsetX = mx - r.x;
                dragOffsetY = my - r.y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (selected != null) {
            LayoutConfig.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (selected == null) return false;
        LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(selected);
        ml.x = (int) Math.max(0, mx - dragOffsetX);
        ml.y = (int) Math.max(0, my - dragOffsetY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (selected == null) return false;
        LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(selected);
        ml.scale = clamp(ml.scale + (delta > 0 ? 0.1f : -0.1f), 0.5f, 3.0f);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) { // ESC
            LayoutConfig.save();
            this.onClose();
            return true;
        }
        if (selected != null) {
            LayoutConfig.ModuleLayout ml = LayoutConfig.layoutOf(selected);
            boolean shift = (modifiers & 1) != 0;
            int step = shift ? 5 : 1;
            switch (key) {
                case 72: // H
                    ml.enabled = !ml.enabled;
                    return true;
                case 82: // R
                    LayoutConfig.reset(selected);
                    return true;
                case 263: ml.x -= step; return true; // left
                case 262: ml.x += step; return true; // right
                case 265: ml.y -= step; return true; // up
                case 264: ml.y += step; return true; // down
                case 219: ml.opacity = clamp(ml.opacity - 0.1f, 0.0f, 1.0f); return true; // [
                case 221: ml.opacity = clamp(ml.opacity + 0.1f, 0.0f, 1.0f); return true; // ]
                case 61: case 187: ml.scale = clamp(ml.scale + 0.1f, 0.5f, 3.0f); return true; // =
                case 45: case 189: ml.scale = clamp(ml.scale - 0.1f, 0.5f, 3.0f); return true; // -
                default: return false;
            }
        }
        return false;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void onClose() {
        LayoutConfig.save();
        super.onClose();
    }
}
