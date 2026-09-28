package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI redesigned after the reference screenshot: one dark rounded panel, a left
 * category sidebar (Combat / Movement / Player / Render / Other / Client) and rounded
 * rows with pill toggles on the right.
 */
public class ClickGuiScreen extends GuiScreen {

    private static class Region {
        int x, y, w, h, code;
        boolean cat;
        String payload;
        Region(int x, int y, int w, int h, int code, boolean cat, String payload) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.code = code;
            this.cat = cat; this.payload = payload;
        }
    }

    private int selected = 0;
    private final List<Region> regions = new ArrayList<Region>();

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void layout() {
        regions.clear();
        int catRows = Modules.CATS.length;
        int maxRows = rowsFor(selected);
        int rowH = 34, gap = 8;
        int sideW = 120;
        int panelH = Math.max(catRows * 40 + 40, maxRows * (rowH + gap) + 44);
        int panelHc = Math.min(panelH, this.height - 30);
        int px = (this.width - 420) / 2, py = (this.height - panelHc) / 2;
        int pw = Math.min(420, this.width - 20);
        regions.add(new Region(px, py, pw, panelHc, -1, false, null)); // panel bg (not clickable)

        // sidebar categories
        for (int i = 0; i < catRows; i++) {
            int cy = py + 18 + i * 40;
            regions.add(new Region(px + 12, cy, sideW, 30, i, true, null));
        }
        // rows of selected category
        int rx = px + sideW + 30;
        int rw = px + pw - 16 - rx;
        int ry = py + 18;
        String cat = Modules.CATS[selected];
        for (Modules.Def d : Modules.ALL) {
            if (!d.cat.equals(cat)) continue;
            regions.add(new Region(rx, ry, rw, rowH, 0, false, d.id));
            ry += rowH + gap;
            if (ry + rowH > py + panelHc - 12) break;   // no overflow outside the panel
        }
    }

    private int rowsFor(int cat) {
        int n = 0;
        String c = Modules.CATS[cat];
        for (Modules.Def d : Modules.ALL) if (d.cat.equals(c)) n++;
        return n;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        layout();
        RenderUtils.drawVerticalGradient(0, 0, width, height, 0xB0101016, 0xB01A1A22);

        for (Region r : regions) {
            if (r.code == -1) {
                Theme.roundRectBordered(r.x, r.y, r.w, r.h, Theme.BG_PANEL, Theme.BORDER);
                continue;
            }
            if (r.cat) {
                boolean sel = r.code == selected;
                Theme.roundRect(r.x, r.y, r.w, r.h, sel ? Theme.BG_SELECT : Theme.BG_SIDE);
                String name = Modules.CATS[r.code];
                String label = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                fontRendererObj.drawStringWithShadow(label, r.x + 12, r.y + 11,
                        sel ? Theme.ACCENT : Theme.TEXT_DIM);
            } else {
                boolean on = Modules.on(r.payload);
                Theme.roundRect(r.x, r.y, r.w, r.h, Theme.BG_ROW);
                fontRendererObj.drawStringWithShadow(cap(r.payload), r.x + 14, r.y + 13, Theme.TEXT);
                Theme.pill(r.x + r.w - 46, r.y + 10, 34, 14, on);
            }
        }
    }

    private String payloadFor(int code) { return Modules.CATS[code]; }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        layout();
        for (Region r : regions) {
            if (r.code == -1 || r.payload == null && !r.cat) continue;
            if (mouseX >= r.x && mouseX <= r.x + r.w && mouseY >= r.y && mouseY <= r.y + r.h) {
                if (r.cat) selected = r.code;
                else com.lindedaoqwq.linclient.config.ModConfig.toggle(r.payload);
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) mc.displayGuiScreen(null);
    }
}
