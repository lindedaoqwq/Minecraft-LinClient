package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.hud.HudEditorScreen;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ClickGUI in the reference style: full-screen dark glass overlay, a 3-column grid of
 *  pill cards (icon | divider | name, gear bottom-right for real settings), a left
 *  vertical icon rail for categories, a search field top-right, thin scrollbar.
 *  Click = toggle; gear = settings popup (sliders + colour). zh/en labels. */
public class ClickGuiScreen extends GuiScreen {

    private static class Card {
        int x, y, w, h;
        Modules.Def def;
        Card(int x, int y, int w, int h, Modules.Def def) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.def = def;
        }
    }

    private static class Row {   // slider row inside the settings popup
        int x, y, w, h;
        Modules.Setting set;
        Row(int x, int y, int w, int h, Modules.Setting set) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.set = set;
        }
    }

    private static final int[] PALETTE = {
            0xFFF2F2F2, 0xFFE04A3A, 0xFFE0912F, 0xFFE0D22F,
            0xFF3ADF6E, 0xFF3ADFE0, 0xFF3AA6F0, 0xFFB03ADF
    };

    // Glass palette (reference style).
    private static final int GLASS        = 0x9007090E;
    private static final int CARD_FILL    = 0x51151920;
    private static final int CARD_FILL_HI = 0x641C222B;
    private static final int BORDER_OFF   = 0x2EFFFFFF;
    private static final int BORDER_ON    = 0xC8FFFFFF;
    private static final int BORDER_HOVER = 0x66FFFFFF;
    private static final int TEXT_DIM     = 0xFF9DA2AB;
    private static final int DIVIDER      = 0x50FFFFFF;

    private int selected = 0;
    private String search = "";
    private boolean searchFocus = false;
    private String settingsOpen = null;   // module id whose settings popup is open
    private String colorTarget = null;    // module id for the colour popup
    private int scrollOff = 0;
    private boolean zh;

    private final List<Card> cards = new ArrayList<Card>();
    private final List<Row> panelRows = new ArrayList<Row>();
    private final Map<String, Row> sliderRows = new HashMap<String, Row>();
    private int gx0, gy0, cardW = 100;
    private final int cardH = 52, gap = 14, cols = 3;

    private boolean dragBar = false;
    private int grabBY;
    private String dragSlider = null;
    private int sliderTrackX, sliderTrackW;
    private Row dragRow = null;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
        scrollOff = 0;
    }

    // ---------- layout ----------

    private int gridBottom() { return height - 44; }

    private int contentH() {
        int n = visibleDefs().size();
        int rows = (n + cols - 1) / cols;
        return rows * (cardH + gap);
    }

    private int maxScroll() { return Math.max(0, contentH() - (gridBottom() - gy0)); }

    private List<Modules.Def> visibleDefs() {
        List<Modules.Def> out = new ArrayList<Modules.Def>();
        String q = search.trim().toLowerCase();
        for (Modules.Def d : Modules.ALL) {
            if (q.isEmpty()) {
                if (d.cat.equals(Modules.CATS[selected])) out.add(d);
            } else {
                String hay = (d.label + " " + d.labelZh).toLowerCase();
                if (hay.contains(q)) out.add(d);
            }
        }
        return out;
    }

    private void buildCards() {
        cards.clear();
        gy0 = 44;
        gx0 = 96;
        int availW = width - gx0 - 36;
        cardW = (availW - (cols - 1) * gap) / cols;
        List<Modules.Def> list = visibleDefs();
        int row = 0, col = 0;
        for (Modules.Def d : list) {
            int cx = gx0 + col * (cardW + gap);
            int cy = gy0 + row * (cardH + gap) - scrollOff;
            cards.add(new Card(cx, cy, cardW, cardH, d));
            col++;
            if (col == cols) { col = 0; row++; }
        }
    }

    // ---------- drawing ----------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        buildCards();
        clampScroll();

        // dark glass backdrop
        RenderUtils.drawRect(0, 0, width, height, GLASS);
        RenderUtils.drawRect(0, 0, width, height, 0x18000000);

        // ---- left icon rail ----
        drawRail(mouseX, mouseY);

        // ---- search field (top-right) ----
        drawSearchField();

        // ---- cards ----
        for (Card c : cards) {
            if (c.y + c.h < gy0 - 10 || c.y > gridBottom() + 10) continue;
            boolean hover = mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= c.y && mouseY <= c.y + c.h;
            boolean on = Modules.on(c.def.id);
            int border = on ? BORDER_ON : (hover ? BORDER_HOVER : BORDER_OFF);
            RenderUtils.drawRounded(c.x, c.y, c.w, c.h, border, c.h / 2);
            RenderUtils.drawRounded(c.x + 1, c.y + 1, c.w - 2, c.h - 2,
                    on || hover ? CARD_FILL_HI : CARD_FILL, c.h / 2 - 1);
            // icon + divider + label
            drawIcon(c.def.cat, c.x + 16, c.y + c.h / 2 - 8, 16, on ? 0xFFFFFFFF : 0xFFB9BEC7);
            RenderUtils.drawRect(c.x + 40, c.y + 12, 1, c.h - 24, on ? 0x90FFFFFF : DIVIDER);
            String label = Modules.label(c.def, zh);
            fontRendererObj.drawStringWithShadow(label, c.x + 52, c.y + (c.h - 8) / 2,
                    on ? 0xFFFFFFFF : TEXT_DIM);
            // gear for modules with real settings
            if (!c.def.settings.isEmpty()) {
                boolean gh = mouseX >= c.x + c.w - 26 && mouseX <= c.x + c.w - 10
                        && mouseY >= c.y + c.h - 26 && mouseY <= c.y + c.h - 10;
                drawGear(c.x + c.w - 18, c.y + c.h - 18, gh || settingsOpen != null
                        && settingsOpen.equals(c.def.id) ? 0xFFE8EAEE : 0xFF8A8F98);
            }
        }

        // ---- thin scrollbar ----
        int content = contentH();
        int view = gridBottom() - gy0;
        if (content > view) {
            int barX = width - 12;
            int trackH = view;
            int thumbH = Math.max(28, trackH * view / content);
            int thumbY = gy0 + (trackH - thumbH) * scrollOff / maxScroll();
            RenderUtils.drawRounded(barX, thumbY, 4, thumbH, 0x70FFFFFF, 2);
        }

        // ---- settings popup ----
        if (settingsOpen != null) drawSettingsPopup(mouseX, mouseY);

        // ---- colour popup ----
        if (colorTarget != null) drawColorPopup();

        // ---- footer ----
        String hint = zh ? "\u70b9\u51fb\u5f00/\u5173 \u00b7 \u9f7f\u8f6e\u8fdb\u5165\u914d\u7f6e \u00b7 ESC \u5173\u95ed"
                : "Click to toggle \u00b7 gear to configure \u00b7 ESC to close";
        fontRendererObj.drawStringWithShadow(hint, 96, height - 22, 0x60FFFFFF);
        fontRendererObj.drawStringWithShadow("LinClient 1.0.0", 96, 18, 0x50FFFFFF);
    }

    private void drawRail(int mouseX, int mouseY) {
        int railX = 24, railW = 40;
        int n = Modules.CATS.length + 1;   // categories + HUD editor
        int cy = height / 2 - (n * 46) / 2;
        for (int i = 0; i < Modules.CATS.length; i++) {
            boolean sel = search.trim().isEmpty() && i == selected;
            boolean hover = mouseX >= railX && mouseX <= railX + railW
                    && mouseY >= cy && mouseY <= cy + 36;
            if (sel || hover) {
                RenderUtils.drawRounded(railX, cy, railW, 36, sel ? 0xB0FFFFFF : 0x40FFFFFF, 10);
                RenderUtils.drawRounded(railX + 1, cy + 1, railW - 2, 34,
                        sel ? 0x5C1E242E : 0x30161A20, 9);
            }
            drawIcon(Modules.CATS[i], railX + railW / 2 - 8, cy + 10, 16,
                    sel ? 0xFFFFFFFF : (hover ? 0xFFD6D9DE : 0xFF8F949D));
            cy += 46;
        }
        // HUD layout editor entry
        boolean hHover = mouseX >= railX && mouseX <= railX + railW
                && mouseY >= cy && mouseY <= cy + 36;
        if (hHover) {
            RenderUtils.drawRounded(railX, cy, railW, 36, 0x40FFFFFF, 10);
            RenderUtils.drawRounded(railX + 1, cy + 1, railW - 2, 34, 0x30161A20, 9);
        }
        drawIcon("hudedit", railX + railW / 2 - 8, cy + 10, 16, hHover ? 0xFFD6D9DE : 0xFF8F949D);
    }

    private void drawSearchField() {
        int w = 220, h = 28;
        int x = width - w - 24, y = 24;
        RenderUtils.drawRounded(x, y, w, h, searchFocus ? 0xB0FFFFFF : 0x40FFFFFF, h / 2);
        RenderUtils.drawRounded(x + 1, y + 1, w - 2, h - 2, 0x5C12161D, h / 2 - 1);
        // magnifier
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 0.65F);
        GL11.glLineWidth(1.2F);
        GL11.glBegin(GL11.GL_LINES);
        float cx0 = x + 14, cy0 = y + h / 2 - 2, r = 4;
        for (int i = 0; i < 8; i++) {
            float a1 = (float) (i * Math.PI / 4), a2 = (float) ((i + 1) * Math.PI / 4);
            vertex(cx0 + r * (float) Math.cos(a1), cy0 + r * (float) Math.sin(a1),
                    cx0 + r * (float) Math.cos(a2), cy0 + r * (float) Math.sin(a2));
        }
        vertex(cx0 + 3, cy0 + 3, cx0 + 8, cy0 + 8);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        // text
        String show = search.isEmpty() && !searchFocus
                ? (zh ? "\u641c\u7d22..." : "Search...") : search;
        int tcol = search.isEmpty() && !searchFocus ? 0xFF6E737C : 0xFFE8EAEE;
        fontRendererObj.drawStringWithShadow(show, x + 30, y + (h - 8) / 2, tcol);
        if (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0) {
            int tx = x + 30 + fontRendererObj.getStringWidth(search);
            RenderUtils.drawRect(tx + 1, y + 7, 1, h - 14, 0xFFE8EAEE);
        }
    }

    private int settingsPopupH(Modules.Def d) {
        int h = 0;
        for (Modules.Setting s : d.settings) h += s.isColor ? 30 : 40;
        return h;
    }

    private void drawSettingsPopup(int mouseX, int mouseY) {
        Modules.Def d = Modules.byId(settingsOpen);
        if (d == null) { settingsOpen = null; return; }
        panelRows.clear();
        sliderRows.clear();
        int pw = Math.min(300, width - 80);
        int ph = 40 + settingsPopupH(d) + 12;
        int x = (width - pw) / 2, y = (height - ph) / 2;
        RenderUtils.drawRect(0, 0, width, height, 0x50000000);
        RenderUtils.drawRounded(x, y, pw, ph, 0xC8FFFFFF, 10);
        RenderUtils.drawRounded(x + 1, y + 1, pw - 2, ph - 2, 0xF4161A22, 9);
        fontRendererObj.drawStringWithShadow(Modules.label(d, zh), x + 18, y + 13, 0xFFFFFFFF);

        int ry = y + 38;
        for (Modules.Setting s : d.settings) {
            if (s.isColor) {
                int cur = ModConfig.color(d.id, s.colorDef);
                fontRendererObj.drawStringWithShadow(s.text(zh), x + 18, ry + 9, TEXT_DIM);
                RenderUtils.drawRounded(x + pw - 68, ry + 3, 50, 22, 0xC8FFFFFF, 8);
                RenderUtils.drawRounded(x + pw - 69, ry + 4, 48, 20, cur, 7);
                panelRows.add(new Row(x, ry, pw, 28, s));
                ry += 30;
            } else {
                float v = ModConfig.value(s.id, s.def);
                float frac = Math.max(0F, Math.min(1F, (v - s.min) / (s.max - s.min)));
                fontRendererObj.drawStringWithShadow(s.text(zh), x + 18, ry + 2, TEXT_DIM);
                String val = fmt(s, v);
                fontRendererObj.drawStringWithShadow(val, x + pw - 18
                        - fontRendererObj.getStringWidth(val), ry + 2, 0xFFE8EAEE);
                int tx = x + 18, tw = pw - 36, ty = ry + 20;
                RenderUtils.drawRounded(tx, ty, tw, 5, 0x38FFFFFF, 2);
                RenderUtils.drawRounded(tx, ty, Math.max(4, (int) (tw * frac)), 5, 0xFF3AA6F0, 2);
                RenderUtils.drawRounded(tx + (int) (tw * frac) - 4, ty - 4, 8, 13, 0xFFE8EAEE, 4);
                Row r = new Row(tx, ty - 8, tw, 20, s);
                panelRows.add(r);
                sliderRows.put(s.id, r);
                ry += 40;
            }
        }
    }

    private void drawColorPopup() {
        int pw = 200, ph = 40 + 4 * 44 + 14;
        int x = (width - pw) / 2, y = (height - ph) / 2;
        RenderUtils.drawRounded(x, y, pw, ph, 0xC8FFFFFF, 10);
        RenderUtils.drawRounded(x + 1, y + 1, pw - 2, ph - 2, 0xF4161A22, 9);
        String title = zh ? "\u989c\u8272" : "Colour";
        fontRendererObj.drawStringWithShadow(title, x + 14, y + 12, Theme.TEXT);
        for (int i = 0; i < PALETTE.length; i++) {
            int cx = x + 14 + (i % 4) * 44, cyy = y + 34 + (i / 4) * 44;
            RenderUtils.drawRounded(cx, cyy, 36, 36, PALETTE[i], 8);
        }
        String def = zh ? "\u9ed8\u8ba4" : "Default";
        fontRendererObj.drawStringWithShadow(def, x + 14 + 3 * 44 + 4, y + 40 + 3 * 44, TEXT_DIM);
    }

    /** Value string for a slider, respecting its step. */
    private String fmt(Modules.Setting s, float v) {
        if (s.step >= 1F) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }

    // ---------- vector icons (16x16 logical space) ----------

    private void drawIcon(String id, float x, float y, float s, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255F, g = ((argb >> 8) & 0xFF) / 255F,
                b = (argb & 0xFF) / 255F, a = ((argb >> 24) & 0xFF) / 255F;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(s / 16F, s / 16F, 1F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(1.4F);
        GL11.glBegin(GL11.GL_LINES);
        if (id.equals("combat")) {                 // crossed swords
            vertex(3, 3, 13, 13); vertex(13, 3, 3, 13);
            vertex(3, 6, 6, 3);   vertex(10, 13, 13, 10);
        } else if (id.equals("movement")) {        // arrow right
            vertex(2, 8, 13, 8);  vertex(8, 3, 13, 8);  vertex(8, 13, 13, 8);
        } else if (id.equals("player")) {          // head + shoulders
            vertex(6, 2, 10, 2);  vertex(10, 2, 10, 6);
            vertex(10, 6, 6, 6);  vertex(6, 6, 6, 2);
            vertex(3, 14, 8, 8);  vertex(8, 8, 13, 14);
        } else if (id.equals("render")) {          // eye (diamond + pupil)
            vertex(8, 3, 14, 8);  vertex(14, 8, 8, 13);
            vertex(8, 13, 2, 8);  vertex(2, 8, 8, 3);
            vertex(7, 7, 9, 9);   vertex(9, 7, 7, 9);
        } else if (id.equals("other")) {           // three dots
            GL11.glEnd();
            GL11.glBegin(GL11.GL_QUADS);
            dot(3, 7); dot(7.5F, 7); dot(12, 7);
        } else if (id.equals("hudedit")) {         // pencil
            vertex(3, 13, 11, 5);
            vertex(11, 5, 13, 3);
            vertex(2, 14, 4, 12);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glPopMatrix();
    }

    /** Small solid square used for the "three dots" icon (quads). */
    private void dot(float x, float y) {
        vertexQ(x, y); vertexQ(x + 2, y); vertexQ(x + 2, y + 2); vertexQ(x, y + 2);
    }

    private void vertex(float x1, float y1, float x2, float y2) {
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
    }

    private void vertexQ(float x, float y) {
        GL11.glVertex2f(x, y);
    }

    private void drawGear(float cx, float cy, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255F, g = ((argb >> 8) & 0xFF) / 255F,
                b = (argb & 0xFF) / 255F, a = ((argb >> 24) & 0xFF) / 255F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(1.2F);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 8; i++) {
            float a1 = (float) (i * Math.PI / 4), a2 = (float) ((i + 1) * Math.PI / 4);
            vertex(cx + 2.6F * (float) Math.cos(a1), cy + 2.6F * (float) Math.sin(a1),
                    cx + 2.6F * (float) Math.cos(a2), cy + 2.6F * (float) Math.sin(a2));
        }
        for (int i = 0; i < 6; i++) {
            float an = (float) (i * Math.PI / 3 + Math.PI / 12);
            vertex(cx + 3.4F * (float) Math.cos(an), cy + 3.4F * (float) Math.sin(an),
                    cx + 4.6F * (float) Math.cos(an), cy + 4.6F * (float) Math.sin(an));
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    // ---------- input ----------

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && colorTarget == null) {
            scrollOff += d > 0 ? -40 : 40;
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

        // colour popup first
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

        // settings popup
        if (settingsOpen != null) {
            Modules.Def d = Modules.byId(settingsOpen);
            if (d == null) { settingsOpen = null; return; }
            int pw = Math.min(300, width - 80);
            int ph = 40 + settingsPopupH(d) + 12;
            int x = (width - pw) / 2, y = (height - ph) / 2;
            if (mouseX >= x && mouseX <= x + pw && mouseY >= y && mouseY <= y + ph) {
                for (Row r : panelRows) {
                    if (mouseX < r.x || mouseX > r.x + r.w || mouseY < r.y || mouseY > r.y + r.h) continue;
                    if (r.set.isColor) {
                        colorTarget = d.id;
                    } else {
                        dragSlider = r.set.id;
                        dragRow = r;
                        applySlider(r, mouseX);
                    }
                    return;
                }
                return;   // inside panel, on nothing: keep open
            }
            settingsOpen = null;   // click outside closes
            return;
        }

        // search field
        int sw = 220, sh = 28;
        int sx = width - sw - 24, sy = 24;
        if (mouseButton == 0 && mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh) {
            searchFocus = true;
            return;
        }
        searchFocus = false;

        // left rail
        int railX = 24, railW = 40;
        int cy = height / 2 - ((Modules.CATS.length + 1) * 46) / 2;
        for (int i = 0; i < Modules.CATS.length; i++) {
            if (mouseButton == 0 && mouseX >= railX && mouseX <= railX + railW
                    && mouseY >= cy && mouseY <= cy + 36) {
                selected = i;
                search = "";
                scrollOff = 0;
                return;
            }
            cy += 46;
        }
        if (mouseButton == 0 && mouseX >= railX && mouseX <= railX + railW
                && mouseY >= cy && mouseY <= cy + 36) {
            mc.displayGuiScreen(new HudEditorScreen());
            return;
        }

        // scrollbar
        int content = contentH();
        int view = gridBottom() - gy0;
        if (content > view && mouseButton == 0) {
            int barX = width - 12;
            int trackH = view;
            int thumbH = Math.max(28, trackH * view / content);
            int thumbY = gy0 + (trackH - thumbH) * scrollOff / maxScroll();
            if (mouseX >= barX - 3 && mouseX <= barX + 7
                    && mouseY >= thumbY - 3 && mouseY <= thumbY + thumbH + 3) {
                dragBar = true;
                grabBY = mouseY - thumbY;
                return;
            }
        }

        // cards (gear zone first)
        if (mouseButton == 0 || mouseButton == 1) {
            for (Card c : cards) {
                if (mouseX < c.x || mouseX > c.x + c.w || mouseY < c.y || mouseY > c.y + c.h) continue;
                if (mouseY < gy0 - 6 || mouseY > gridBottom() + 6) continue;
                boolean gearZone = !c.def.settings.isEmpty()
                        && mouseX >= c.x + c.w - 26 && mouseX <= c.x + c.w - 10
                        && mouseY >= c.y + c.h - 26 && mouseY <= c.y + c.h - 10;
                if (gearZone) {
                    settingsOpen = c.def.id;
                    scrollOff = 0;
                } else {
                    ModConfig.toggle(c.def.id);
                }
                return;
            }
        }
    }

    private void applySlider(Row r, int mouseX) {
        Modules.Setting s = r.set;
        float t = (mouseX - r.x) / (float) r.w;
        t = Math.max(0F, Math.min(1F, t));
        float v = s.min + t * (s.max - s.min);
        v = Math.round(v / s.step) * s.step;
        v = Math.max(s.min, Math.min(s.max, v));
        ModConfig.setValue(s.id, v);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragBar) {
            int content = contentH();
            int view = gridBottom() - gy0;
            int trackH = view;
            int thumbH = Math.max(28, trackH * view / content);
            int rel = mouseY - grabBY - gy0;
            scrollOff = maxScroll() > 0 ? rel * maxScroll() / (trackH - thumbH) : 0;
            clampScroll();
        } else if (dragSlider != null && dragRow != null) {
            applySlider(dragRow, mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragBar = false;
        dragSlider = null;
        dragRow = null;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) {   // ESC
            if (colorTarget != null) colorTarget = null;
            else if (settingsOpen != null) settingsOpen = null;
            else if (searchFocus) searchFocus = false;
            else mc.displayGuiScreen(null);
            return;
        }
        if (searchFocus) {
            if (key == 14 && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
            } else if (key == 28 || key == 15) {
                searchFocus = false;
            } else if (c >= 32 && c < 127) {
                search += c;
            }
            scrollOff = 0;
        }
    }
}
