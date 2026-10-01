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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** LinClient ClickGUI — structure inspired by modern glass-panel clients (FPSMaster
 *  Edge-style rows, not a copy): centred panel, left sidebar (brand, search, category
 *  nav with counts, HUD editor), main area with single-column module rows showing
 *  name + description inline, iOS-style toggle and an expand chevron. Rows with
 *  settings expand IN PLACE into a two-column settings grid (sliders / colour dots).
 *  LinClient sky-blue branding is kept (accent), not the reference indigo. */
public class ClickGuiScreen extends GuiScreen {

    private static class Row {      // a module row in the list
        int x, y, w, h;
        Modules.Def def;
        Row(int x, int y, int w, int h, Modules.Def def) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.def = def;
        }
    }

    private static class Ctrl {     // a control row inside an expanded settings grid
        int x, y, w, h;
        Modules.Setting set;
        Ctrl(int x, int y, int w, int h, Modules.Setting set) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.set = set;
        }
    }

    private static final int[] PALETTE = {
            0xFFF2F2F2, 0xFFE04A3A, 0xFFE0912F, 0xFFE0D22F,
            0xFF3ADF6E, 0xFF3ADFE0, 0xFF3AA6F0, 0xFFB03ADF
    };

    // ---- tokens (dark glass, LinClient sky-blue accent) ----
    private static final int ACCENT       = 0xFF3FA9FF;
    private static final int GLASS        = 0xF20C0D10;
    private static final int STROKE       = 0x14FFFFFF;
    private static final int STROKE_STRONG= 0x2EFFFFFF;
    private static final int LAYER        = 0x0BFFFFFF;
    private static final int LAYER_HOVER  = 0x14FFFFFF;
    private static final int LAYER_ACTIVE = 0x1FFFFFFF;
    private static final int TEXT         = 0xFFF2F2F2;
    private static final int TEXT_2       = 0xFF9A9A9A;
    private static final int TEXT_3       = 0xFF5C5C5C;

    private int selected = 0;
    private String search = "";
    private boolean searchFocus = false;
    private String expandedId = null;    // module whose settings grid is open
    private String colorTarget = null;   // module id for the colour popup
    private int scrollOff = 0;
    private boolean zh;

    private final List<Row> rows = new ArrayList<Row>();
    private final List<Ctrl> ctrls = new ArrayList<Ctrl>();
    private final Map<String, Ctrl> sliderCtrls = new HashMap<String, Ctrl>();

    private int px, py, pw, ph;
    private final int sideW = 92;
    private int listTop, listBottom;

    private boolean dragBar = false;
    private int grabBY;
    private String dragSlider = null;
    private Ctrl dragCtrl = null;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
        scrollOff = 0;
    }

    // ---------- layout ----------

    private int panelW() { return Math.min(470, width - 20); }
    private int panelH() { return Math.min(300, height - 20); }

    private int contentX() { return px + sideW + 8; }
    private int contentW() { return px + pw - 8 - contentX(); }

    private int rowHeight(Modules.Def d) {
        return d.id.equals(expandedId) ? 22 + settingsH(d) : 22;
    }

    private int settingsH(Modules.Def d) {
        if (d.settings.isEmpty()) return 0;
        int rows2 = (d.settings.size() + 1) / 2;
        return 1 + 4 + rows2 * 19 + 3;   // divider + pad + grid + pad
    }

    private int contentH() {
        int h = 0;
        for (Modules.Def d : visibleDefs()) h += rowHeight(d) + 3;
        return h;
    }

    private int maxScroll() { return Math.max(0, contentH() - (listBottom - listTop)); }

    private int catCount(String cat) {
        int n = 0;
        for (Modules.Def d : Modules.ALL) if (d.cat.equals(cat)) n++;
        return n;
    }

    private int catEnabled(String cat) {
        int n = 0;
        for (Modules.Def d : Modules.ALL) if (d.cat.equals(cat) && Modules.on(d.id)) n++;
        return n;
    }

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

    private void buildRows() {
        rows.clear();
        ctrls.clear();
        sliderCtrls.clear();
        int y = listTop - scrollOff;
        int x0 = contentX();
        for (Modules.Def d : visibleDefs()) {
            int h = rowHeight(d);
            rows.add(new Row(x0, y, contentW(), h, d));
            if (d.id.equals(expandedId) && !d.settings.isEmpty()) {
                int gy = y + 22 + 1 + 4;
                int colW = (contentW() - 12 - 6) / 2;
                for (int i = 0; i < d.settings.size(); i++) {
                    Modules.Setting s = d.settings.get(i);
                    int cx = x0 + 6 + (i % 2) * (colW + 6);
                    Ctrl c = new Ctrl(cx, gy + (i / 2) * 19, colW, 19, s);
                    ctrls.add(c);
                    if (!s.isColor) sliderCtrls.put(s.id, c);
                }
            }
            y += h + 3;
        }
    }

    private void clampScroll() {
        int max = maxScroll();
        if (scrollOff < 0) scrollOff = 0;
        if (scrollOff > max) scrollOff = max;
    }

    // ---------- drawing ----------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        px = (width - panelW()) / 2;
        py = (height - panelH()) / 2;
        pw = panelW();
        ph = panelH();
        listTop = py + 30;
        listBottom = py + ph - 8;
        buildRows();
        clampScroll();

        // world veil + glass panel
        RenderUtils.drawRect(0, 0, width, height, 0x94000000);
        RenderUtils.drawRounded(px, py, pw, ph, STROKE_STRONG, 9);
        RenderUtils.drawRounded(px + 1, py + 1, pw - 2, ph - 2, GLASS, 8);

        // ---- sidebar ----
        RenderUtils.drawRect(px + sideW, py + 6, 1, ph - 12, STROKE);
        drawSidebar(mouseX, mouseY);

        // ---- main head ----
        boolean searching = !search.trim().isEmpty();
        String title = searching ? (zh ? "\u641c\u7d22" : "Search")
                : Modules.catLabel(Modules.CATS[selected], zh);
        fontRendererObj.drawStringWithShadow(title, contentX(), py + 9, TEXT);
        String sub = searching
                ? (visibleDefs().size() + (zh ? " \u4e2a\u7ed3\u679c" : " results"))
                : (visibleDefs().size() + (zh ? " \u4e2a\u6a21\u5757 \u00b7 \u5df2\u542f\u7528 "
                    + catEnabled(Modules.CATS[selected]) + " \u4e2a"
                    : " modules \u00b7 " + catEnabled(Modules.CATS[selected]) + " on"));
        fontRendererObj.drawStringWithShadow(sub, contentX() + 10
                + fontRendererObj.getStringWidth(title), py + 11, TEXT_2);

        // ---- module rows (clipped) ----
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glScissor(contentX() * f, mc.displayHeight - listBottom * f,
                (px + pw - 8 - contentX()) * f, (listBottom - listTop) * f);
        for (Row r : rows) {
            if (r.y + r.h < listTop - 6 || r.y > listBottom + 6) continue;
            drawModuleRow(r, mouseX, mouseY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // ---- scrollbar ----
        int content = contentH();
        int view = listBottom - listTop;
        if (content > view) {
            int barX = px + pw - 7;
            int thumbH = Math.max(18, view * view / content);
            int thumbY = listTop + (view - thumbH) * scrollOff / maxScroll();
            RenderUtils.drawRounded(barX, thumbY, 3, thumbH, 0x38FFFFFF, 1);
        }

        // ---- colour popup ----
        if (colorTarget != null) drawColorPopup();
    }

    private void drawSidebar(int mouseX, int mouseY) {
        int sx = px + 7;
        int pad = 7;
        // brand: gradient-ish mark + name + version
        int mX = sx, mY = py + 8;
        RenderUtils.drawRounded(mX, mY, 12, 12, ACCENT, 4);
        GL11.glPushMatrix();
        GL11.glTranslatef(mX + 3.5F, mY + 2.5F, 0);
        fontRendererObj.drawStringWithShadow("L", 0, 0, 0xFFFFFFFF);
        GL11.glPopMatrix();
        fontRendererObj.drawStringWithShadow("LinClient", mX + 16, mY + 0, TEXT);
        fontRendererObj.drawStringWithShadow("1.0.0", mX + 16, mY + 8, TEXT_3);

        // search (capsule)
        int swH = 15;
        int sy = py + 26;
        RenderUtils.drawRounded(sx, sy, sideW - 2 * pad, swH,
                searchFocus ? ACCENT : STROKE, swH / 2);
        RenderUtils.drawRounded(sx + 1, sy + 1, sideW - 2 * pad - 2, swH - 2,
                0x40000000, swH / 2 - 1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.45F, 0.5F, 0.56F, 1F);
        GL11.glLineWidth(1F);
        GL11.glBegin(GL11.GL_LINES);
        float mcx = sx + 8, mcy = sy + swH / 2F - 1F, mr = 2.6F;
        for (int i = 0; i < 8; i++) {
            float a1 = (float) (i * Math.PI / 4), a2 = (float) ((i + 1) * Math.PI / 4);
            vertex(mcx + mr * (float) Math.cos(a1), mcy + mr * (float) Math.sin(a1),
                    mcx + mr * (float) Math.cos(a2), mcy + mr * (float) Math.sin(a2));
        }
        vertex(mcx + 2, mcy + 2, mcx + 5, mcy + 5);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        String show = search.isEmpty() && !searchFocus
                ? (zh ? "\u641c\u7d22..." : "Search...") : search;
        fontRendererObj.drawStringWithShadow(show, sx + 13, sy + 4,
                search.isEmpty() && !searchFocus ? TEXT_3 : TEXT);
        if (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0) {
            int tx = sx + 13 + fontRendererObj.getStringWidth(search);
            RenderUtils.drawRect(tx + 1, sy + 3, 1, swH - 6, TEXT);
        }

        // category nav with counts
        int ny = py + 48;
        for (int i = 0; i < Modules.CATS.length; i++) {
            boolean sel = search.trim().isEmpty() && i == selected;
            boolean hover = mouseX >= sx && mouseX <= sx + sideW - 2 * pad
                    && mouseY >= ny && mouseY <= ny + 16;
            if (sel) {
                RenderUtils.drawRounded(sx, ny, sideW - 2 * pad, 16, ACCENT, 8);
            } else if (hover) {
                RenderUtils.drawRounded(sx, ny, sideW - 2 * pad, 16, LAYER_HOVER, 8);
            }
            drawIcon(Modules.CATS[i], sx + 5, ny + 4, 8,
                    sel ? 0xFFFFFFFF : (hover ? 0xFFD0D4DA : TEXT_2));
            String label = Modules.catLabel(Modules.CATS[i], zh);
            fontRendererObj.drawStringWithShadow(label, sx + 16, ny + 4,
                    sel ? 0xFFFFFFFF : (hover ? 0xFFE8EAEE : TEXT_2));
            String n = String.valueOf(catCount(Modules.CATS[i]));
            fontRendererObj.drawStringWithShadow(n, sx + sideW - 2 * pad - 2
                    - fontRendererObj.getStringWidth(n), ny + 4,
                    sel ? 0xCCFFFFFF : TEXT_3);
            ny += 18;
        }

        // bottom: HUD layout editor
        int hy = py + ph - 24;
        boolean hHover = mouseX >= sx && mouseX <= sx + sideW - 2 * pad
                && mouseY >= hy && mouseY <= hy + 16;
        if (hHover) RenderUtils.drawRounded(sx, hy, sideW - 2 * pad, 16, LAYER_HOVER, 8);
        drawIcon("hudedit", sx + 5, hy + 4, 8, hHover ? 0xFFD0D4DA : TEXT_2);
        fontRendererObj.drawStringWithShadow(zh ? "HUD \u5e03\u5c40" : "HUD Layout",
                sx + 16, hy + 4, hHover ? 0xFFE8EAEE : TEXT_2);
    }

    private void drawModuleRow(Row r, int mouseX, int mouseY) {
        Modules.Def d = r.def;
        boolean expanded = d.id.equals(expandedId);
        boolean on = Modules.on(d.id);
        boolean hover = mouseX >= r.x && mouseX <= r.x + r.w && mouseY >= r.y && mouseY <= r.y + 22;
        int h = expanded ? r.h : 22;
        int bg = expanded ? 0x2E000000 : (hover ? LAYER_HOVER : LAYER);
        RenderUtils.drawRounded(r.x, r.y, r.w, h, expanded ? STROKE_STRONG : 0x0AFFFFFF, 5);
        RenderUtils.drawRounded(r.x + 1, r.y + 1, r.w - 2, h - 2, bg, 4);

        // name + inline description
        String name = Modules.label(d, zh);
        fontRendererObj.drawStringWithShadow(name, r.x + 7, r.y + 7, TEXT);
        String desc = Modules.desc(d, zh);
        int nx = r.x + 7 + fontRendererObj.getStringWidth(name) + 6;
        int maxDesc = r.w - 100 - (nx - r.x);
        if (maxDesc > 20) {
            if (fontRendererObj.getStringWidth(desc) > maxDesc) {
                desc = fontRendererObj.trimStringToWidth(desc, maxDesc - 4) + "...";
            }
            fontRendererObj.drawStringWithShadow(desc, nx, r.y + 7,
                    expanded ? TEXT_2 : TEXT_3);
        }

        // iOS-style switch (right)
        int swW = 18, swH = 10;
        int swX = r.x + r.w - swW - 16, swY = r.y + 6;
        RenderUtils.drawRounded(swX, swY, swW, swH, on ? ACCENT : LAYER_ACTIVE, swH / 2);
        int k = swH - 2;
        int kx = on ? swX + swW - k - 1 : swX + 1;
        RenderUtils.drawRounded(kx, swY + 1, k, k, 0xFFEAECEF, k / 2);

        // chevron: > when collapsed, v when expanded
        int chX = r.x + r.w - 11, chY = r.y + 8;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.55F, 0.55F, 0.58F, 1F);
        GL11.glLineWidth(1.2F);
        GL11.glBegin(GL11.GL_LINES);
        if (expanded) {
            vertex(chX - 2.5F, chY, chX, chY + 3);
            vertex(chX, chY + 3, chX + 2.5F, chY);
        } else {
            vertex(chX, chY - 2.5F, chX + 2.5F, chY);
            vertex(chX + 2.5F, chY, chX, chY + 2.5F);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);

        // expanded settings grid (two columns)
        if (expanded && !d.settings.isEmpty()) {
            int divY = r.y + 22;
            RenderUtils.drawRect(r.x + 4, divY, r.w - 8, 1, STROKE);
            for (Ctrl c : ctrls) {
                if (c.set.isColor) {
                    fontRendererObj.drawStringWithShadow(c.set.text(zh), c.x, c.y + 1, TEXT_2);
                    int cur = ModConfig.color(d.id, c.set.colorDef);
                    String hex = String.format("#%06X", cur & 0xFFFFFF);
                    fontRendererObj.drawStringWithShadow(hex, c.x + c.w - 24
                            - fontRendererObj.getStringWidth(hex), c.y + 1, TEXT_3);
                    RenderUtils.drawRounded(c.x + c.w - 9, c.y, 9, 9, 0x40FFFFFF, 4);
                    RenderUtils.drawRounded(c.x + c.w - 8, c.y + 1, 7, 7, cur, 3);
                } else {
                    float v = ModConfig.value(c.set.id, c.set.def);
                    float frac = Math.max(0F, Math.min(1F, (v - c.set.min) / (c.set.max - c.set.min)));
                    fontRendererObj.drawStringWithShadow(c.set.text(zh), c.x, c.y + 1, TEXT_2);
                    String val = fmt(c.set, v);
                    fontRendererObj.drawStringWithShadow(val, c.x + c.w
                            - fontRendererObj.getStringWidth(val), c.y + 1, TEXT_2);
                    int tX = c.x, tW = c.w - 26, tY = c.y + 12;
                    RenderUtils.drawRounded(tX, tY, tW, 3, LAYER_ACTIVE, 1);
                    RenderUtils.drawRounded(tX, tY, Math.max(2, (int) (tW * frac)), 3, ACCENT, 1);
                    RenderUtils.drawRounded(tX + (int) (tW * frac) - 3, tY - 2, 7, 7,
                            0xFFEAECEF, 3);
                }
            }
        }
    }

    private void drawColorPopup() {
        Modules.Def d = Modules.byId(colorTarget);
        int cur;
        int def = 0xFF3AA6F0;
        if (d != null) {
            for (Modules.Setting s : d.settings) {
                if (s.isColor) { def = s.colorDef; break; }
            }
        }
        cur = ModConfig.color(colorTarget, def);
        int pw2 = 190, ph2 = 30 + 4 * 40 + 16;
        int x = (width - pw2) / 2, y = (height - ph2) / 2;
        RenderUtils.drawRect(0, 0, width, height, 0x64000000);
        RenderUtils.drawRounded(x, y, pw2, ph2, STROKE_STRONG, 9);
        RenderUtils.drawRounded(x + 1, y + 1, pw2 - 2, ph2 - 2, GLASS, 8);
        String title = zh ? "\u989c\u8272" : "Colour";
        fontRendererObj.drawStringWithShadow(title, x + 12, y + 10, TEXT);
        String hex = String.format("#%06X", cur & 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(hex, x + pw2 - 12
                - fontRendererObj.getStringWidth(hex), y + 10, TEXT_2);
        for (int i = 0; i < PALETTE.length; i++) {
            int cx = x + 12 + (i % 4) * 42, cyy = y + 28 + (i / 4) * 40;
            RenderUtils.drawRounded(cx, cyy, 34, 34, PALETTE[i], 7);
        }
        String defS = zh ? "\u9ed8\u8ba4" : "Default";
        int dx = x + 12 + 3 * 42 + 4;
        RenderUtils.drawRounded(dx, y + 28 + 3 * 40, 34, 34, LAYER, 7);
        RenderUtils.drawRounded(dx + 1, y + 29 + 3 * 40, 32, 32, 0x12000000, 6);
        fontRendererObj.drawStringWithShadow(defS, dx, y + 28 + 3 * 40 + 40, TEXT_3);
    }

    private String fmt(Modules.Setting s, float v) {
        if (s.step >= 1F) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }

    // ---------- vector icons (16 logical px, scalable) ----------

    private void drawIcon(String id, float x, float y, float s, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255F, g = ((argb >> 8) & 0xFF) / 255F,
                b = (argb & 0xFF) / 255F, a = ((argb >> 24) & 0xFF) / 255F;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(s / 16F, s / 16F, 1F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(2F);
        GL11.glBegin(GL11.GL_LINES);
        if (id.equals("hud")) {                    // monitor / dashboard
            vertex(2, 3, 14, 3);  vertex(14, 3, 14, 11);
            vertex(14, 11, 2, 11); vertex(2, 11, 2, 3);
            vertex(6, 14, 10, 14); vertex(8, 11, 8, 14);
        } else if (id.equals("combat")) {          // crossed swords
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

    // ---------- input ----------

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0 && colorTarget == null) {
            scrollOff += d > 0 ? -18 : 18;
            clampScroll();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // colour popup first
        if (colorTarget != null) {
            int pw2 = 190, ph2 = 30 + 4 * 40 + 16;
            int x = (width - pw2) / 2, y = (height - ph2) / 2;
            for (int i = 0; i < PALETTE.length; i++) {
                int cx = x + 12 + (i % 4) * 42, cyy = y + 28 + (i / 4) * 40;
                if (mouseX >= cx && mouseX <= cx + 34 && mouseY >= cyy && mouseY <= cyy + 34) {
                    ModConfig.setColor(colorTarget, PALETTE[i]);
                    colorTarget = null;
                    return;
                }
            }
            int dx = x + 12 + 3 * 42 + 4;
            if (mouseX >= dx && mouseX <= dx + 34 && mouseY >= y + 28 + 3 * 40
                    && mouseY <= y + 28 + 3 * 40 + 34) {
                ModConfig.setColor(colorTarget, -1);
                colorTarget = null;
                return;
            }
            if (mouseX < x || mouseX > x + pw2 || mouseY < y || mouseY > y + ph2) colorTarget = null;
            return;
        }

        int sx = px + 7;
        int pad = 7;

        // search field
        int swH = 15;
        int sy = py + 26;
        if (mouseButton == 0 && mouseX >= sx && mouseX <= sx + sideW - 2 * pad
                && mouseY >= sy && mouseY <= sy + swH) {
            searchFocus = true;
            return;
        }
        searchFocus = false;

        // category nav
        int ny = py + 48;
        for (int i = 0; i < Modules.CATS.length; i++) {
            if (mouseButton == 0 && mouseX >= sx && mouseX <= sx + sideW - 2 * pad
                    && mouseY >= ny && mouseY <= ny + 16) {
                selected = i;
                search = "";
                scrollOff = 0;
                expandedId = null;
                return;
            }
            ny += 18;
        }
        // HUD layout editor
        int hy = py + ph - 24;
        if (mouseButton == 0 && mouseX >= sx && mouseX <= sx + sideW - 2 * pad
                && mouseY >= hy && mouseY <= hy + 16) {
            mc.displayGuiScreen(new HudEditorScreen());
            return;
        }

        // scrollbar
        int content = contentH();
        int view = listBottom - listTop;
        if (content > view && mouseButton == 0) {
            int barX = px + pw - 7;
            int thumbH = Math.max(18, view * view / content);
            int thumbY = listTop + (view - thumbH) * scrollOff / maxScroll();
            if (mouseX >= barX - 2 && mouseX <= barX + 5
                    && mouseY >= thumbY - 2 && mouseY <= thumbY + thumbH + 2) {
                dragBar = true;
                grabBY = mouseY - thumbY;
                return;
            }
        }

        // module rows: head click = expand (or toggle when no settings);
        // switch = toggle; colour dot = palette
        for (Row r : rows) {
            if (mouseX < r.x || mouseX > r.x + r.w || mouseY < r.y || mouseY > r.y + r.h) continue;
            if (mouseY < listTop - 2 || mouseY > listBottom + 2) continue;
            Modules.Def d = r.def;
            if (mouseY <= r.y + 22) {
                int swW = 18;
                int swX = r.x + r.w - swW - 16;
                if (mouseX >= swX - 2 && mouseX <= swX + swW + 2) {
                    ModConfig.toggle(d.id);
                    return;
                }
                if (!d.settings.isEmpty()) {
                    expandedId = d.id.equals(expandedId) ? null : d.id;
                    clampScroll();
                } else {
                    ModConfig.toggle(d.id);
                }
                return;
            }
            for (Ctrl c : ctrls) {
                if (mouseX < c.x || mouseX > c.x + c.w || mouseY < c.y || mouseY > c.y + c.h) continue;
                if (c.set.isColor) {
                    colorTarget = d.id;
                } else {
                    dragSlider = c.set.id;
                    dragCtrl = c;
                    applySlider(c, mouseX);
                }
                return;
            }
            return;
        }
    }

    private void applySlider(Ctrl c, int mouseX) {
        Modules.Setting s = c.set;
        int tW = c.w - 26;
        float t = (mouseX - c.x) / (float) tW;
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
            int view = listBottom - listTop;
            int thumbH = Math.max(18, view * view / content);
            int rel = mouseY - grabBY - listTop;
            scrollOff = maxScroll() > 0 ? rel * maxScroll() / (view - thumbH) : 0;
            clampScroll();
        } else if (dragSlider != null && dragCtrl != null) {
            applySlider(dragCtrl, mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragBar = false;
        dragSlider = null;
        dragCtrl = null;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) {   // ESC
            if (colorTarget != null) colorTarget = null;
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
