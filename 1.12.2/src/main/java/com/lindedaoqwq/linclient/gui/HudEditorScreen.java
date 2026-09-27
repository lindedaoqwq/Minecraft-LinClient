package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.hud.HudOverlay;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.Rect;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;

/**
 * Dedicated HUD layout editor. Opened from the ClickGUI / Home "HUD layout" button.
 *
 * Everything happens inside this screen: every module (enabled or not) is rendered in place,
 * modules are dragged with the mouse, and ESC saves the new positions and leaves the editor.
 * No mouse hooks or hidden global flags are involved.
 */
public class HudEditorScreen extends GuiScreen {
    private String draggingId = null;
    private int dragOffX = 0, dragOffY = 0;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Render every module (force: disabled ones are dimmed so they can still be moved).
        HudOverlay.renderAll(mc);

        // Hover outline on the module under the cursor.
        if (draggingId == null) {
            HudModule hover = HudOverlay.get(HudOverlay.hitTest(mouseX, mouseY, true));
            if (hover != null) {
                outline(hover, 0x66FFFFFF);
            }
        }

        // Bright outline while dragging.
        if (draggingId != null) {
            HudModule m = HudOverlay.get(draggingId);
            if (m != null) {
                outline(m, 0xFF7FB2E5);
            }
        }

        // Header + footer hints.
        fontRenderer.drawString(I18n.t("linclient.gui.hudhint.title"), 10, 10, 0xFFFFFF);
        fontRenderer.drawString(I18n.t("linclient.gui.hudhint.tip"), 10, 22, 0x9FB8CC);
        String esc = I18n.t("linclient.gui.hudhint.esc");
        fontRenderer.drawString(esc, width - fontRenderer.getStringWidth(esc) - 10, 10, 0xFFCC66);
        String bottom = I18n.t("linclient.gui.hudhint.bottom");
        fontRenderer.drawString(bottom, width / 2 - fontRenderer.getStringWidth(bottom) / 2, height - 14, 0x7799BB);
    }

    private void outline(HudModule m, int color) {
        Rect r = m.rect(mc);
        RenderUtils.drawRect(r.x, r.y, r.w, 1, color);
        RenderUtils.drawRect(r.x, r.y + r.h - 1, r.w, 1, color);
        RenderUtils.drawRect(r.x, r.y, 1, r.h, color);
        RenderUtils.drawRect(r.x + r.w - 1, r.y, 1, r.h, color);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        String id = HudOverlay.hitTest(mouseX, mouseY, true);
        if (id != null) {
            HudModule m = HudOverlay.get(id);
            draggingId = id;
            dragOffX = mouseX - m.posX;
            dragOffY = mouseY - m.posY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton != 0 || draggingId == null) return;
        HudModule m = HudOverlay.get(draggingId);
        if (m == null) return;
        Rect r = m.rect(mc);
        m.posX = clamp(mouseX - dragOffX, 0, Math.max(0, width - r.w));
        m.posY = clamp(mouseY - dragOffY, 0, Math.max(0, height - r.h));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0) return;
        saveDragging();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) { // ESC: save and leave the editor
            saveDragging();
            this.mc.displayGuiScreen(null);
        }
    }

    private void saveDragging() {
        if (draggingId != null) {
            HudModule m = HudOverlay.get(draggingId);
            if (m != null) ModConfig.setModulePos(m.id, m.posX, m.posY);
            draggingId = null;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
