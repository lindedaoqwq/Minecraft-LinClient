package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.LinClient;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone, decorative home screen for LinClient. Opened from the title screen button and the
 * OPEN_HOME key (F8). Shows the mod branding, live FPS, per-module toggles, auto-sprint, and quick
 * actions (open ClickGUI / HUD layout / language / close).
 */
public class HomeScreen extends GuiScreen {
    private static final int K_TOGGLE_MODULE = 1, K_TOGGLE_GENERAL = 2, K_BTN = 3;

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

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        buildRegions();
        RenderUtils.drawVerticalGradient(0, 0, width, height, 0xFF0E1117, 0xFF1C2740);

        String title = "LinClient";
        int tw = fontRendererObj.getStringWidth(title);
        fontRendererObj.drawString(title, width / 2 - tw / 2, 24, 0x66CCFF);
        fontRendererObj.drawString("v" + LinClient.VERSION + "  |  FPS: " + ClientState.fps,
                width / 2 - fontRendererObj.getStringWidth("v" + LinClient.VERSION + "  |  FPS: " + ClientState.fps) / 2,
                42, 0x99AABB);
        fontRendererObj.drawString(I18n.t("linclient.gui.home.subtitle"),
                width / 2 - fontRendererObj.getStringWidth(I18n.t("linclient.gui.home.subtitle")) / 2, 56, 0x8899AA);

        for (Region r : regions) {
            if (r.kind == K_TOGGLE_MODULE) {
                drawToggleRow(r, I18n.t("linclient.module." + r.payload), ModConfig.isModuleOn(r.payload));
            } else if (r.kind == K_TOGGLE_GENERAL) {
                drawToggleRow(r, I18n.t("linclient.autosprint"), ModConfig.autoSprint);
            } else if (r.kind == K_BTN) {
                drawButton(r, homeButtonLabel(r.code));
            }
        }
    }

    private void drawToggleRow(Region r, String label, boolean on) {
        RenderUtils.drawRect(r.x, r.y, r.w, r.h, 0xCC1A1F2B);
        fontRendererObj.drawString(label, r.x + 8, r.y + 7, 0xE6E6E6);
        int bx = r.x + r.w - 26, by = r.y + 5, bs = 14;
        RenderUtils.drawRect(bx, by, bs, bs, on ? 0xFF3A6EA5 : 0xFF33384A);
        if (on) fontRendererObj.drawString("X", bx + 4, by + 3, 0xFFFFFF);
    }

    private void drawButton(Region r, String label) {
        RenderUtils.drawPanel(r.x, r.y, r.w, r.h, 0xFF274053, 0xFF3A6EA5);
        int tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawString(label, r.x + (r.w - tw) / 2, r.y + 7, 0xFFFFFF);
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
        int px = width / 2 - 160, pw = 320;
        int y = 84;
        int rowH = 26;
        for (String id : ModConfig.MODULE_IDS) {
            regions.add(new Region(px, y, pw, rowH, K_TOGGLE_MODULE, id, 0));
            y += rowH + 4;
        }
        regions.add(new Region(px, y, pw, rowH, K_TOGGLE_GENERAL, "autoSprint", 0));
        y += rowH + 12;

        int bw = (pw - 12) / 4;
        regions.add(new Region(px, y, bw, rowH, K_BTN, null, 300));
        regions.add(new Region(px + bw + 4, y, bw, rowH, K_BTN, null, 301));
        regions.add(new Region(px + (bw + 4) * 2, y, bw, rowH, K_BTN, null, 302));
        regions.add(new Region(px + (bw + 4) * 3, y, bw, rowH, K_BTN, null, 303));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        buildRegions();
        for (Region r : regions) {
            if (mouseX >= r.x && mouseX <= r.x + r.w && mouseY >= r.y && mouseY <= r.y + r.h) {
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
                        mc.displayGuiScreen(null);
                        ClientState.hudEditMode = true;
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
}
