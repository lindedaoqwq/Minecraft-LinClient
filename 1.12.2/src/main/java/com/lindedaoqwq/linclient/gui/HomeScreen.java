package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.LinClient;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone home screen for LinClient. Opened from the title screen button and the OPEN_HOME key
 * (F8). Blue-themed, decorated with the same panel style as the ClickGUI: module toggle cards in a
 * two-column grid plus quick actions (open ClickGUI / HUD layout editor / language / close).
 */
public class HomeScreen extends GuiScreen {
    private static final int K_TOGGLE_MODULE = 1, K_TOGGLE_GENERAL = 2, K_BTN = 3;

    // Palette (blue theme, matches the ClickGUI).
    private static final int BG_TOP = 0xFF060D1F;
    private static final int BG_BOTTOM = 0xFF10305C;
    private static final int ACCENT = 0xFF66CCFF;
    private static final int PANEL_BG = 0xB312294E;
    private static final int PANEL_BORDER = 0xFF3A6EA5;
    private static final int CARD_BG = 0x99153060;
    private static final int CARD_BORDER = 0xFF2A5A8A;
    private static final int CARD_BG_HOVER = 0xCC1B3A66;
    private static final int BTN_BG = 0xFF1B3A66;
    private static final int BTN_BG_HOVER = 0xFF27548F;
    private static final int BTN_BORDER_HOVER = 0xFF7FB2E5;
    private static final int ON_BG = 0xFF3A6EA5;
    private static final int OFF_BG = 0xFF334455;

    private static class Region {
        int x, y, w, h;
        int kind;
        String payload;
        int code;
        Region(int x, int y, int w, int h, int kind, String payload, int code) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.kind = kind; this.payload = payload; this.code = code;
        }
    }

    private final List<Region> regions = new ArrayList<>();

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        buildRegions();

        // Blue gradient backdrop with subtle grid decoration.
        RenderUtils.drawVerticalGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
        RenderUtils.drawRect(0, 0, width, 2, ACCENT);
        RenderUtils.drawRect(0, height - 2, width, 2, 0xFF2A5A8A);
        for (int gx = width / 2; gx < width; gx += 48) {
            RenderUtils.drawRect(gx, 2, 1, height - 4, 0x0AFFFFFF);
            RenderUtils.drawRect(width - gx, 2, 1, height - 4, 0x0AFFFFFF);
        }
        for (int gy = 40; gy < height - 20; gy += 48) {
            RenderUtils.drawRect(0, gy, width, 1, 0x0AFFFFFF);
        }

        int px = width / 2 - 170, pw = 340;

        // Central panel.
        RenderUtils.drawPanel(px - 12, 56, pw + 24, 212, PANEL_BG, PANEL_BORDER);

        // Title with side rules.
        String title = "LinClient";
        int tw = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, width / 2 - tw / 2, 66, ACCENT);
        int ruleW = Math.max(0, (pw - tw) / 2 - 14);
        RenderUtils.drawRect(px, 71, ruleW, 1, PANEL_BORDER);
        RenderUtils.drawRect(width / 2 + tw / 2 + 14, 71, ruleW, 1, PANEL_BORDER);

        // Subtitle: version + live FPS.
        String sub = "v" + LinClient.VERSION + "  |  FPS: " + ClientState.fps;
        fontRenderer.drawString(sub, width / 2 - fontRenderer.getStringWidth(sub) / 2, 78, 0x7FA8CC);
        RenderUtils.drawRect(px + 10, 92, pw - 20, 1, 0x33FFFFFF);

        for (Region r : regions) {
            if (r.kind == K_TOGGLE_MODULE) {
                drawToggleRow(r, I18n.t("linclient.module." + r.payload), ModConfig.isModuleOn(r.payload), mouseX, mouseY);
            } else if (r.kind == K_TOGGLE_GENERAL) {
                drawToggleRow(r, I18n.t("linclient.autosprint"), ModConfig.autoSprint, mouseX, mouseY);
            } else if (r.kind == K_BTN) {
                drawButton(r, homeButtonLabel(r.code), mouseX, mouseY);
            }
        }

        String tip = I18n.t("linclient.gui.home.tip");
        fontRenderer.drawString(tip, width / 2 - fontRenderer.getStringWidth(tip) / 2, height - 16, 0x557799);
    }

    private void drawToggleRow(Region r, String label, boolean on, int mx, int my) {
        boolean hover = inside(r, mx, my);
        RenderUtils.drawPanel(r.x, r.y, r.w, r.h, hover ? CARD_BG_HOVER : CARD_BG, CARD_BORDER);
        fontRenderer.drawString(label, r.x + 8, r.y + 8, 0xE6EEF5);
        String st = on ? "ON" : "OFF";
        int bw = fontRenderer.getStringWidth(st) + 8;
        int bx = r.x + r.w - bw - 6;
        RenderUtils.drawRect(bx, r.y + 6, bw, 12, on ? ON_BG : OFF_BG);
        fontRenderer.drawString(st, bx + 4, r.y + 8, 0xFFFFFF);
    }

    private void drawButton(Region r, String label, int mx, int my) {
        boolean hover = inside(r, mx, my);
        RenderUtils.drawPanel(r.x, r.y, r.w, r.h,
                hover ? BTN_BG_HOVER : BTN_BG,
                hover ? BTN_BORDER_HOVER : PANEL_BORDER);
        int tw = fontRenderer.getStringWidth(label);
        fontRenderer.drawString(label, r.x + (r.w - tw) / 2, r.y + 8, 0xFFFFFF);
    }

    private String homeButtonLabel(int code) {
        switch (code) {
            case 300: return I18n.t("linclient.gui.openconfig");
            case 301: return I18n.t("linclient.gui.hudlayout");
            case 302: return I18n.t("linclient.gui.language") + ": " + (I18n.getLang().equals("zh_CN") ? "中文" : "EN");
            case 303: return I18n.t("linclient.gui.close");
            default: return "";
        }
    }

    private void buildRegions() {
        regions.clear();
        int px = width / 2 - 170, pw = 340;
        int colW = (pw - 8) / 2;
        int rowH = 24, gap = 4;
        int y = 102;

        // 7 module toggles + auto sprint in a two-column grid.
        List<String> ids = new ArrayList<String>(ModConfig.MODULE_IDS);
        int half = (ids.size() + 1) / 2;
        for (int i = 0; i < ids.size(); i++) {
            int col = i / half, row = i % half;
            regions.add(new Region(px + col * (colW + 8), y + row * (rowH + gap), colW, rowH,
                    K_TOGGLE_MODULE, ids.get(i), 0));
        }
        int rows = half;
        int sprintRow = ids.size() % half;
        regions.add(new Region(px + (ids.size() / half) * (colW + 8), y + sprintRow * (rowH + gap), colW, rowH,
                K_TOGGLE_GENERAL, "autoSprint", 0));

        int by = y + rows * (rowH + gap) + 8;
        int bw = (pw - 12) / 4;
        regions.add(new Region(px, by, bw, rowH, K_BTN, null, 300));
        regions.add(new Region(px + (bw + 4), by, bw, rowH, K_BTN, null, 301));
        regions.add(new Region(px + (bw + 4) * 2, by, bw, rowH, K_BTN, null, 302));
        regions.add(new Region(px + (bw + 4) * 3, by, bw, rowH, K_BTN, null, 303));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        buildRegions();
        for (Region r : regions) {
            if (inside(r, mouseX, mouseY)) {
                handle(r);
                return;
            }
        }
    }

    private void handle(Region r) {
        Minecraft mc = Minecraft.getMinecraft();
        switch (r.kind) {
            case K_TOGGLE_MODULE:
                ModConfig.toggle(ModConfig.CAT_MODULES, r.payload);
                break;
            case K_TOGGLE_GENERAL:
                ModConfig.toggle(ModConfig.CAT_GENERAL, r.payload);
                break;
            case K_BTN:
                switch (r.code) {
                    case 300:
                        mc.displayGuiScreen(new ClickGuiScreen());
                        break;
                    case 301:
                        mc.displayGuiScreen(new HudEditorScreen());
                        break;
                    case 302:
                        ModConfig.setLanguage(I18n.getLang().equals("zh_CN") ? "en_US" : "zh_CN");
                        break;
                    case 303:
                        mc.displayGuiScreen(null);
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    private static boolean inside(Region r, int mx, int my) {
        return mx >= r.x && mx <= r.x + r.w && my >= r.y && my <= r.y + r.h;
    }
}
