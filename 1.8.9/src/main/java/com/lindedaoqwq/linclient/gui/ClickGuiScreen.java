package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.hud.HudEditorScreen;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** ClickGUI matching the reference screenshot: dark rounded panel, left category
 *  sidebar with a selected highlight block, right rounded rows with pill toggles.
 *  The panel itself is draggable and the list has a scrollbar thumb. zh/en labels. */
public class ClickGuiScreen extends GuiScreen {

    private static class Region {
        int x, y, w, h;
        boolean cat, special;
        Modules.Def def;
        Region(int x, int y, int w, int h, boolean cat, boolean special, Modules.Def def) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.cat = cat; this.special = special; this.def = def;
        }
    }

    private static final int[] PALETTE = {
            0xFFF2F2F2, 0xFFE04A3A, 0xFFE0912F, 0xFFE0D22F,
            0xFF3ADF6E, 0xFF3ADFE0, 0xFF3AA6F0, 0xFFB03ADF
    };

    private int selected = 0;
    private int scrollOff = 0;
    private String colorTarget = null;
    private final List<Region> rows = new ArrayList<Region>();
    private int listTop, listBottom, rx, rw;
    private boolean zh;

    // Panel dragging.
    private int panX, panY;
    private boolean dragPanel = false;
    private int grabPX, grabPY;

    // Scrollbar thumb dragging.
    private boolean dragBar = false;
    private int grabBY;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
        panX = 0;
        panY = 0;
        scrollOff = 0;
    }

    private int panelW() { return Math.min(500, width - 24); }
    private int panelH() { return Math.min(380, height - 24); }
    private int px() { return clampPan((width - panelW()) / 2 + panX, 6, width - panelW() - 6); }
    private int py() { return clampPan((height - panelH()) / 2 + panY, 6, height - panelH() - 6); }

    private static int clampPan(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private int rowCount() {
        String c = Modules.CATS[selected];
        int n = c.equals("other") ? 1 : 0;   // HUD editor entry
        for (Modules.Def d : Modules.ALL) if (d.cat.equals(c)) n++;
        return n;
    }

    private int maxScroll() { return Math.max(0, rowCount() * 54 - (listBottom - listTop)); }

    private void buildRows() {
        rows.clear();
        int rowStep = 54;
        rx = px() + 206;
        rw = px() + panelW() - 20 - rx;
        listTop = py() + 18;
        listBottom = py() + panelH() - 18;
        String cat = Modules.CATS[selected];
        int y = listTop - scrollOff;
        if (cat.equals("other")) {
            rows.add(new Region(rx, y, rw, 44, false, true, null));
            y += rowStep;
        }
        for (Modules.Def d : Modules.ALL) {
            if (!d.cat.equals(cat)) continue;
            rows.add(new Region(rx, y, rw, 44, false, false, d));
            y += rowStep;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawRect(0, 0, width, height, 0x90000000);
        int px = px(), py = py(), pw = panelW(), ph = panelH();
        Theme.roundRectBordered(px, py, pw, ph, 0xF217171B, 0xFF0B0B0D);

        // ---- sidebar ----
        int sideX = px + 16, sideW = 150;
        int cy = py + 34;
        for (int i = 0; i < Modules.CATS.length; i++) {
            boolean sel = i == selected;
            if (sel) Theme.roundRect(sideX, cy, sideW, 34, 0xFF3A3A3F);
            String label = Modules.catLabel(Modules.CATS[i], zh);
            fontRendererObj.drawStringWithShadow(label, sideX + 14, cy + 13,
                    sel ? 0xFFFFFFFF : Theme.TEXT_DIM);
            cy += 42;
        }
        // divider like the screenshot
        RenderUtils.drawRect(px + 186, py + 16, 1, ph - 32, 0xFF26262B);

        // ---- rows (clipped + scrollable) ----
        buildRows();
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((rx - 6) * f, mc.displayHeight - (listBottom) * f, (rw + 12) * f,
                (listBottom - listTop) * f);
        for (Region r : rows) {
            if (r.y + r.h < listTop - 10 || r.y > listBottom + 10) continue;
            Theme.roundRect(r.x, r.y, r.w, r.h, Theme.BG_ROW);
            String label;
            if (r.special) {
                label = zh ? "HUD \u4f4d\u7f6e\u7f16\u8f91" : "HUD Layout Editor";
            } else {
                label = Modules.label(r.def, zh);
            }
            fontRendererObj.drawStringWithShadow(label, r.x + 16, r.y + 18, Theme.TEXT);
            if (!r.special) Theme.pill(r.x + r.w - 50, r.y + 14, 34, 16, Modules.on(r.def.id));
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // ---- scrollbar thumb ----
        int content = rowCount() * 54;
        int view = listBottom - listTop;
        if (content > view) {
            int barX = px + pw - 12;
            int trackH = listBottom - listTop;
            int thumbH = Math.max(24, trackH * view / content);
            int thumbY = listTop + (trackH - thumbH) * scrollOff / maxScroll();
            Theme.roundRect(barX, listTop, 5, trackH, 0xFF232327);
            Theme.roundRect(barX, thumbY, 5, thumbH, 0xFF4A4A52);
        }

        // ---- colour popup ----
        if (colorTarget != null) drawColorPopup();

        // hint
        String hint = zh
                ? "\u5de6\u952e\u5f00\u5173 | \u53f3\u952e\u989c\u8272 | \u62d6\u52a8\u9876\u90e8\u79fb\u52a8\u9762\u677f | ESC \u5173\u95ed"
                : "LMB toggle | RMB colour | drag top to move | ESC close";
        fontRendererObj.drawStringWithShadow(hint, px + 16, py + ph - 14, 0xFF707078);
    }

    private void drawColorPopup() {
        int pw = 200, ph = 40 + 4 * 44 + 14;
        int x = (width - pw) / 2, y = (height - ph) / 2;
        Theme.roundRectBordered(x, y, pw, ph, 0xF21C1C21, 0xFF0B0B0D);
        String title = zh ? "\u989c\u8272" : "Colour";
        fontRendererObj.drawStringWithShadow(title, x + 14, y + 12, Theme.TEXT);
        for (int i = 0; i < PALETTE.length; i++) {
            int cx = x + 14 + (i % 4) * 44, cyy = y + 34 + (i / 4) * 44;
            Theme.roundRectBordered(cx, cyy, 36, 36, PALETTE[i], 0xFF0B0B0D);
        }
        String def = zh ? "\u9ed8\u8ba4" : "Default";
        fontRendererObj.drawStringWithShadow(def, x + 14 + 3 * 44 + 4, y + 40 + 3 * 44, Theme.TEXT_DIM);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && colorTarget == null) {
            scrollOff += d > 0 ? -34 : 34;
            clampScroll();
        }
    }

    private void clampScroll() {
        int max = maxScroll();
        if (scrollOff < 0) scrollOff = 0;
        if (scrollOff > max) scrollOff = max;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        // colour popup handling first
        if (colorTarget != null) {
            int pw = 200, ph = 40 + 4 * 44 + 14;
            int x = (width - pw) / 2, y = (height - ph) / 2;
            for (int i = 0; i < PALETTE.length; i++) {
                int cx = x + 14 + (i % 4) * 44, cyy = y + 34 + (i / 4) * 44;
                if (mouseX >= cx && mouseX <= cx + 36 && mouseY >= cyy && mouseY <= cyy + 36) {
                    ModConfig.setColor(colorTarget, PALETTE[i]);
                    colorTarget = null;
                    return;
                }
            }
            int dx = x + 14 + 3 * 44, dy = y + 34 + 3 * 44;
            if (mouseX >= dx && mouseX <= dx + 40 && mouseY >= dy && mouseY <= dy + 36) {
                ModConfig.setColor(colorTarget, -1);
                colorTarget = null;
                return;
            }
            if (mouseX < x || mouseX > x + pw || mouseY < y || mouseY > y + ph) colorTarget = null;
            return;
        }
        int px = px(), py = py(), pw = panelW(), ph = panelH();
        // scrollbar thumb
        int content = rowCount() * 54;
        int view = listBottom - listTop;
        if (content > view && mouseButton == 0) {
            int barX = px + pw - 12;
            int trackH = listBottom - listTop;
            int thumbH = Math.max(24, trackH * view / content);
            int thumbY = listTop + (trackH - thumbH) * scrollOff / maxScroll();
            if (mouseX >= barX - 2 && mouseX <= barX + 8 && mouseY >= thumbY - 2 && mouseY <= thumbY + thumbH + 2) {
                dragBar = true;
                grabBY = mouseY - thumbY;
                return;
            }
        }
        // sidebar categories
        int sideX = px + 16, sideW = 150;
        int cy = py + 34;
        for (int i = 0; i < Modules.CATS.length; i++) {
            if (mouseX >= sideX && mouseX <= sideX + sideW && mouseY >= cy && mouseY <= cy + 34) {
                if (mouseButton == 0) { selected = i; scrollOff = 0; }
                return;
            }
            cy += 42;
        }
        buildRows();
        for (Region r : rows) {
            if (mouseX < r.x || mouseX > r.x + r.w || mouseY < r.y || mouseY > r.y + r.h) continue;
            if (r.special) {
                if (mouseButton == 0) mc.displayGuiScreen(new HudEditorScreen());
                return;
            }
            if (mouseButton == 0) ModConfig.toggle(r.def.id);
            else if (mouseButton == 1) colorTarget = r.def.id;
            return;
        }
        // drag the panel by its top strip (above sidebar/rows)
        if (mouseButton == 0 && mouseY < py + 16 && mouseX >= px && mouseX <= px + pw) {
            dragPanel = true;
            grabPX = mouseX - px;
            grabPY = mouseY - py;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragPanel) {
            panX = mouseX - grabPX - (width - panelW()) / 2;
            panY = mouseY - grabPY - (height - panelH()) / 2;
        } else if (dragBar) {
            int content = rowCount() * 54;
            int view = listBottom - listTop;
            int trackH = view;
            int thumbH = Math.max(24, trackH * view / content);
            int rel = mouseY - grabBY - listTop;
            scrollOff = maxScroll() > 0 ? rel * maxScroll() / (trackH - thumbH) : 0;
            clampScroll();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragPanel = false;
        dragBar = false;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) {
            if (colorTarget != null) colorTarget = null;
            else mc.displayGuiScreen(null);
        }
    }
}
