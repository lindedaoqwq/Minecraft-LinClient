package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.hud.HudEditorScreen;
import com.lindedaoqwq.linclient.util.FontUtils;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** LinClient ClickGUI — own design, matching the blue main-menu branding.
 *  Centred deep-blue panel: brand + category rail on the left (icons + labels),
 *  two-column module cards on the right with a one-line description, a real
 *  toggle switch, and a gear for modules with settings (sliders / colour).
 *  Search field top-right, thin scrollbar, popup settings dialog. zh/en. */
public class ClickGuiScreen extends GuiScreen {

    private static class Card {
        int x, y, w, h;
        Modules.Def def;
        Card(int x, int y, int w, int h, Modules.Def def) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.def = def;
        }
    }

    private static class Row {   // row inside the settings popup
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

    // ---- LinClient blue palette (matches the main menu) ----
    private static final int ACCENT      = 0xFF3FA9FF;
    private static final int ACCENT_DEEP = 0xFF1B6FC4;
    private static final int PANEL_BG    = 0xF60D1420;
    private static final int PANEL_LINE  = 0xFF1D344F;
    private static final int CARD_OFF    = 0xFF131D2C;
    private static final int CARD_ON     = 0xFF152A42;
    private static final int CARD_LINE   = 0xFF20304A;
    private static final int CARD_LINE_ON= 0xFF2E5F8F;
    private static final int TEXT        = 0xFFF0F4FA;
    private static final int TEXT_SUB    = 0xFF7C8AA0;
    private static final int TEXT_DIM    = 0xFF9DA2AB;
    private static final int SWITCH_OFF  = 0xFF2A3648;

    private int selected = 0;
    private String search = "";
    private boolean searchFocus = false;
    private String settingsOpen = null;
    private String colorTarget = null;
    private int scrollOff = 0;
    private boolean zh;

    private final List<Card> cards = new ArrayList<Card>();
    private final List<Row> panelRows = new ArrayList<Row>();
    private final Map<String, Row> sliderRows = new HashMap<String, Row>();

    private boolean dragBar = false;
    private int grabBY;
    private String dragSlider = null;
    private Row dragRow = null;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
        scrollOff = 0;
    }

    // ---------- panel layout ----------

    private int panelW() { return Math.min(720, width - 28); }
    private int panelH() { return Math.min(430, height - 28); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return (height - panelH()) / 2; }

    private int sideW = 178;
    private int sideX() { return panelX() + 14; }
    private int contentX() { return panelX() + sideW + 18; }
    private int contentW() { return panelX() + panelW() - 18 - contentX(); }
    private int cardsTop() { return panelY() + 56; }
    private int cardsBottom() { return panelY() + panelH() - 16; }

    private int visibleCount() { return visibleDefs().size(); }

    private int contentH() {
        int n = visibleCount();
        return ((n + 1) / 2) * (44 + 10);   // two columns
    }

    private int maxScroll() { return Math.max(0, contentH() - (cardsBottom() - cardsTop())); }

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
        List<Modules.Def> list = visibleDefs();
        int cw = (contentW() - 10) / 2;
        int row = 0, col = 0;
        for (Modules.Def d : list) {
            int cx = contentX() + col * (cw + 10);
            int cy = cardsTop() + row * (44 + 10) - scrollOff;
            cards.add(new Card(cx, cy, cw, 44, d));
            col++;
            if (col == 2) { col = 0; row++; }
        }
    }

    // ---------- drawing ----------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        buildCards();
        clampScroll();
        FontRenderer brand = FontUtils.get();

        RenderUtils.drawRect(0, 0, width, height, 0x8C04070C);
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        RenderUtils.drawRounded(px, py, pw, ph, PANEL_LINE, 10);
        RenderUtils.drawRounded(px + 1, py + 1, pw - 2, ph - 2, PANEL_BG, 9);
        // subtle top accent strip
        RenderUtils.drawRounded(px + 14, py, 56, 3, ACCENT, 2);

        // ---- sidebar: brand ----
        GL11.glPushMatrix();
        float bs = 1.25F;
        GL11.glScalef(bs, bs, bs);
        int bx = (int) ((sideX() + 2) / bs);
        int by = (int) ((py + 18) / bs);
        brand.drawStringWithShadow("LinClient", bx, by, ACCENT);
        GL11.glPopMatrix();
        RenderUtils.drawVerticalGradient(sideX() + 3, py + 40, 46, 2, ACCENT, ACCENT_DEEP);
        fontRenderer.drawStringWithShadow("1.0.0", sideX() + 58, py + 36, 0xFF51617A);

        // ---- sidebar: categories ----
        int cy = py + 58;
        for (int i = 0; i < Modules.CATS.length; i++) {
            boolean sel = search.trim().isEmpty() && i == selected;
            boolean hover = mouseX >= sideX() && mouseX <= sideX() + sideW - 6
                    && mouseY >= cy && mouseY <= cy + 32;
            if (sel) {
                RenderUtils.drawRounded(sideX(), cy, sideW - 6, 32, 0xFF16273C, 7);
                RenderUtils.drawRect(sideX(), cy + 6, 3, 20, ACCENT);
            } else if (hover) {
                RenderUtils.drawRounded(sideX(), cy, sideW - 6, 32, 0xFF101B2B, 7);
            }
            drawIcon(Modules.CATS[i], sideX() + 14, cy + 8, 16,
                    sel ? ACCENT : (hover ? 0xFFC9D2DE : 0xFF6F7C90));
            fontRenderer.drawStringWithShadow(Modules.catLabel(Modules.CATS[i], zh),
                    sideX() + 40, cy + 12, sel ? TEXT : (hover ? 0xFFC9D2DE : TEXT_SUB));
            cy += 38;
        }

        // ---- sidebar: HUD layout editor button ----
        int hy = py + ph - 46;
        boolean hHover = mouseX >= sideX() && mouseX <= sideX() + sideW - 6
                && mouseY >= hy && mouseY <= hy + 32;
        RenderUtils.drawRounded(sideX(), hy, sideW - 6, 32,
                hHover ? 0xFF1B3A5C : 0xFF101B2B, 7);
        drawIcon("hudedit", sideX() + 14, hy + 8, 16, hHover ? ACCENT : 0xFF6F7C90);
        fontRenderer.drawStringWithShadow(zh ? "HUD \u5e03\u5c40\u7f16\u8f91" : "HUD Layout Editor",
                sideX() + 40, hy + 12, hHover ? TEXT : TEXT_SUB);
        // sidebar divider
        RenderUtils.drawRect(panelX() + sideW + 4, py + 14, 1, ph - 28, PANEL_LINE);

        // ---- content header: category title + search ----
        String title = search.trim().isEmpty()
                ? Modules.catLabel(Modules.CATS[selected], zh)
                : (zh ? "\u641c\u7d22" : "Search");
        fontRenderer.drawStringWithShadow(title, contentX(), py + 22, TEXT);
        String count = visibleCount() + (zh ? " \u9879" : " modules");
        fontRenderer.drawStringWithShadow(count, contentX() + 14
                + fontRenderer.getStringWidth(title) + 10, py + 24, 0xFF51617A);
        drawSearchField(mouseX, mouseY);

        // ---- cards (clipped) ----
        int cx0 = contentX(), cx1 = panelX() + panelW() - 10;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glScissor(cx0 * f, mc.displayHeight - cardsBottom() * f,
                (cx1 - cx0) * f, (cardsBottom() - cardsTop()) * f);
        for (Card c : cards) {
            if (c.y + c.h < cardsTop() - 8 || c.y > cardsBottom() + 8) continue;
            drawCard(c, mouseX, mouseY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // ---- scrollbar ----
        int content = contentH();
        int view = cardsBottom() - cardsTop();
        if (content > view) {
            int barX = panelX() + panelW() - 8;
            int thumbH = Math.max(30, view * view / content);
            int thumbY = cardsTop() + (view - thumbH) * scrollOff / maxScroll();
            RenderUtils.drawRounded(barX, thumbY, 4, thumbH, 0xFF2E5F8F, 2);
        }

        // ---- popups ----
        if (settingsOpen != null) drawSettingsPopup(mouseX, mouseY);
        if (colorTarget != null) drawColorPopup();

        // ---- footer hint ----
        String hint = zh ? "\u70b9\u51fb\u5361\u7247\u5f00/\u5173 \u00b7 \u9f7f\u8f6e\u8fdb\u5165\u914d\u7f6e \u00b7 ESC \u5173\u95ed"
                : "Click a card to toggle \u00b7 gear for settings \u00b7 ESC to close";
        fontRenderer.drawStringWithShadow(hint, px + 16, py + ph - 12, 0xFF4A5A72);
    }

    private void drawCard(Card c, int mouseX, int mouseY) {
        boolean on = Modules.on(c.def.id);
        boolean hover = mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= c.y && mouseY <= c.y + c.h;
        RenderUtils.drawRounded(c.x, c.y, c.w, c.h, on ? CARD_LINE_ON : CARD_LINE, 8);
        RenderUtils.drawRounded(c.x + 1, c.y + 1, c.w - 2, c.h - 2, on ? CARD_ON : CARD_OFF, 7);
        if (on) RenderUtils.drawRect(c.x + 1, c.y + 7, 2, c.h - 14, ACCENT);

        // name + description
        fontRenderer.drawStringWithShadow(Modules.label(c.def, zh), c.x + 12, c.y + 8, TEXT);
        String desc = Modules.desc(c.def, zh);
        if (fontRenderer.getStringWidth(desc) > c.w - 24) {
            desc = fontRenderer.trimStringToWidth(desc, c.w - 28) + "...";
        }
        fontRenderer.drawStringWithShadow(desc, c.x + 12, c.y + 22, on ? 0xFF8FA8C4 : TEXT_SUB);

        // gear (modules with settings), bottom-right
        if (!c.def.settings.isEmpty()) {
            boolean gh = mouseX >= c.x + c.w - 26 && mouseX <= c.x + c.w - 12
                    && mouseY >= c.y + c.h - 22 && mouseY <= c.y + c.h - 8;
            drawGear(c.x + c.w - 19, c.y + c.h - 15, gh
                    || (settingsOpen != null && settingsOpen.equals(c.def.id))
                    ? 0xFFE8EAEE : 0xFF6F7C90);
        }

        // toggle switch, top-right
        int sw = 34, sh2 = 14;
        int sx = c.x + c.w - sw - 10, sy = c.y + 8;
        RenderUtils.drawRounded(sx, sy, sw, sh2, on ? ACCENT : SWITCH_OFF, sh2 / 2);
        int k = sh2 - 4;
        int kx = on ? sx + sw - k - 2 : sx + 2;
        RenderUtils.drawRounded(kx, sy + 2, k, k, 0xFFF2F5F9, k / 2);
        if (hover) RenderUtils.drawRounded(c.x, c.y, c.w, c.h, 0x18FFFFFF, 8);
    }

    private void drawSearchField(int mouseX, int mouseY) {
        int w = 170, h = 22;
        int x = panelX() + panelW() - w - 16, y = panelY() + 18;
        RenderUtils.drawRounded(x, y, w, h, searchFocus ? 0xFF2E5F8F : 0xFF20304A, h / 2);
        RenderUtils.drawRounded(x + 1, y + 1, w - 2, h - 2, 0xFF0F1928, h / 2 - 1);
        // magnifier
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.55F, 0.62F, 0.72F, 1F);
        GL11.glLineWidth(1.2F);
        GL11.glBegin(GL11.GL_LINES);
        float cx0 = x + 11, cy0 = y + h / 2 - 1.5F, r = 3.2F;
        for (int i = 0; i < 8; i++) {
            float a1 = (float) (i * Math.PI / 4), a2 = (float) ((i + 1) * Math.PI / 4);
            vertex(cx0 + r * (float) Math.cos(a1), cy0 + r * (float) Math.sin(a1),
                    cx0 + r * (float) Math.cos(a2), cy0 + r * (float) Math.sin(a2));
        }
        vertex(cx0 + 2.5F, cy0 + 2.5F, cx0 + 6.5F, cy0 + 6.5F);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        String show = search.isEmpty() && !searchFocus
                ? (zh ? "\u641c\u7d22..." : "Search...") : search;
        int tcol = search.isEmpty() && !searchFocus ? 0xFF4A5A72 : TEXT;
        fontRenderer.drawStringWithShadow(show, x + 24, y + (h - 8) / 2, tcol);
        if (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0) {
            int tx = x + 24 + fontRenderer.getStringWidth(search);
            RenderUtils.drawRect(tx + 1, y + 5, 1, h - 10, TEXT);
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
        RenderUtils.drawRect(0, 0, width, height, 0x58000000);
        RenderUtils.drawRounded(x, y, pw, ph, CARD_LINE_ON, 10);
        RenderUtils.drawRounded(x + 1, y + 1, pw - 2, ph - 2, 0xF8142236, 9);
        fontRenderer.drawStringWithShadow(Modules.label(d, zh), x + 18, y + 13, TEXT);

        int ry = y + 38;
        for (Modules.Setting s : d.settings) {
            if (s.isColor) {
                int cur = ModConfig.color(d.id, s.colorDef);
                fontRenderer.drawStringWithShadow(s.text(zh), x + 18, ry + 9, TEXT_DIM);
                RenderUtils.drawRounded(x + pw - 68, ry + 3, 50, 22, 0xFF2E5F8F, 8);
                RenderUtils.drawRounded(x + pw - 69, ry + 4, 48, 20, cur, 7);
                panelRows.add(new Row(x, ry, pw, 28, s));
                ry += 30;
            } else {
                float v = ModConfig.value(s.id, s.def);
                float frac = Math.max(0F, Math.min(1F, (v - s.min) / (s.max - s.min)));
                fontRenderer.drawStringWithShadow(s.text(zh), x + 18, ry + 2, TEXT_DIM);
                String val = fmt(s, v);
                fontRenderer.drawStringWithShadow(val, x + pw - 18
                        - fontRenderer.getStringWidth(val), ry + 2, TEXT);
                int tx = x + 18, tw = pw - 36, ty = ry + 20;
                RenderUtils.drawRounded(tx, ty, tw, 5, 0xFF20304A, 2);
                RenderUtils.drawRounded(tx, ty, Math.max(4, (int) (tw * frac)), 5, ACCENT, 2);
                RenderUtils.drawRounded(tx + (int) (tw * frac) - 4, ty - 4, 8, 13, 0xFFF2F5F9, 4);
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
        RenderUtils.drawRounded(x, y, pw, ph, CARD_LINE_ON, 10);
        RenderUtils.drawRounded(x + 1, y + 1, pw - 2, ph - 2, 0xF8142236, 9);
        String title = zh ? "\u989c\u8272" : "Colour";
        fontRenderer.drawStringWithShadow(title, x + 14, y + 12, TEXT);
        for (int i = 0; i < PALETTE.length; i++) {
            int cx = x + 14 + (i % 4) * 44, cyy = y + 34 + (i / 4) * 44;
            RenderUtils.drawRounded(cx, cyy, 36, 36, PALETTE[i], 8);
        }
        String def = zh ? "\u9ed8\u8ba4" : "Default";
        fontRenderer.drawStringWithShadow(def, x + 14 + 3 * 44 + 4, y + 40 + 3 * 44, TEXT_DIM);
    }

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
        } else if (id.equals("render")) {          // eye
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
            scrollOff += d > 0 ? -36 : 36;
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
                return;   // inside the panel: keep open
            }
            settingsOpen = null;
            return;
        }

        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();

        // search field
        int sw = 170, sh = 22;
        int sx = px + pw - sw - 16, sy = py + 18;
        if (mouseButton == 0 && mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh) {
            searchFocus = true;
            return;
        }
        searchFocus = false;

        // sidebar categories
        int cy = py + 58;
        for (int i = 0; i < Modules.CATS.length; i++) {
            if (mouseButton == 0 && mouseX >= sideX() && mouseX <= sideX() + sideW - 6
                    && mouseY >= cy && mouseY <= cy + 32) {
                selected = i;
                search = "";
                scrollOff = 0;
                return;
            }
            cy += 38;
        }
        // HUD layout editor button
        int hy = py + ph - 46;
        if (mouseButton == 0 && mouseX >= sideX() && mouseX <= sideX() + sideW - 6
                && mouseY >= hy && mouseY <= hy + 32) {
            mc.displayGuiScreen(new HudEditorScreen());
            return;
        }

        // scrollbar
        int content = contentH();
        int view = cardsBottom() - cardsTop();
        if (content > view && mouseButton == 0) {
            int barX = px + pw - 8;
            int thumbH = Math.max(30, view * view / content);
            int thumbY = cardsTop() + (view - thumbH) * scrollOff / maxScroll();
            if (mouseX >= barX - 3 && mouseX <= barX + 7
                    && mouseY >= thumbY - 3 && mouseY <= thumbY + thumbH + 3) {
                dragBar = true;
                grabBY = mouseY - thumbY;
                return;
            }
        }

        // cards: gear zone first, then toggle
        if (mouseButton == 0 || mouseButton == 1) {
            for (Card c : cards) {
                if (mouseX < c.x || mouseX > c.x + c.w || mouseY < c.y || mouseY > c.y + c.h) continue;
                if (mouseY < cardsTop() - 4 || mouseY > cardsBottom() + 4) continue;
                boolean gearZone = !c.def.settings.isEmpty()
                        && mouseX >= c.x + c.w - 26 && mouseX <= c.x + c.w - 12
                        && mouseY >= c.y + c.h - 22 && mouseY <= c.y + c.h - 8;
                if (gearZone) {
                    settingsOpen = c.def.id;
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
            int view = cardsBottom() - cardsTop();
            int thumbH = Math.max(30, view * view / content);
            int rel = mouseY - grabBY - cardsTop();
            scrollOff = maxScroll() > 0 ? rel * maxScroll() / (view - thumbH) : 0;
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
