package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI configuration screen (replaces the old config menu). Left sidebar picks a category
 * (modules / vanilla HUD / settings); the right panel lists toggles and action buttons.
 */
public class ClickGuiScreen extends GuiScreen {
    private static final String[] CATS = {"modules", "vanilla", "settings"};

    private static class VanillaEntry {
        final String key;
        final String labelKey;
        VanillaEntry(String key, String labelKey) {
            this.key = key;
            this.labelKey = labelKey;
        }
    }

    private static final VanillaEntry[] VANILLA = {
            new VanillaEntry("hideHealth", "linclient.vanilla.health"),
            new VanillaEntry("hideArmor", "linclient.vanilla.armor"),
            new VanillaEntry("hideFood", "linclient.vanilla.food"),
            new VanillaEntry("hideAir", "linclient.vanilla.air"),
            new VanillaEntry("hideHotbar", "linclient.vanilla.hotbar"),
            new VanillaEntry("hideExp", "linclient.vanilla.exp"),
            new VanillaEntry("hideCrosshair", "linclient.vanilla.crosshair"),
            new VanillaEntry("hideBoss", "linclient.vanilla.boss"),
            // hidePotion / hideVignette have no ElementType in 1.8.9, so they are not listed here.
            new VanillaEntry("hidePortal", "linclient.vanilla.portal"),
            new VanillaEntry("hideHelmet", "linclient.vanilla.helmet"),
            new VanillaEntry("hideJumpbar", "linclient.vanilla.jumpbar"),
    };

    private static final int K_CAT = 0, K_TOGGLE_MODULE = 1, K_TOGGLE_VANILLA = 2,
            K_TOGGLE_GENERAL = 3, K_BTN = 4;

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

    private int selected = 0;
    private final List<Region> regions = new ArrayList<>();

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        buildRegions();
        RenderUtils.drawVerticalGradient(0, 0, width, height, 0xFF10131A, 0xFF1B2030);

        fontRendererObj.drawString("LinClient", 12, 12, 0x66CCFF);
        fontRendererObj.drawString(I18n.t("linclient.gui.clickgui.subtitle"), 90, 16, 0x99AABB);

        for (Region r : regions) {
            if (r.kind == K_CAT) {
                boolean sel = r.code == selected;
                RenderUtils.drawRect(r.x, r.y, r.w, r.h, sel ? 0xFF3A6EA5 : 0xFF222838);
                fontRendererObj.drawString(I18n.t("linclient.gui.cat." + CATS[r.code]), r.x + 8, r.y + 7, 0xFFFFFF);
            } else if (r.kind == K_TOGGLE_MODULE) {
                drawToggleRow(r, I18n.t("linclient.module." + r.payload), ModConfig.isModuleOn(r.payload));
            } else if (r.kind == K_TOGGLE_VANILLA) {
                drawToggleRow(r, I18n.t(VANILLA[findVanilla(r.payload)].labelKey), ModConfig.isVanillaHidden(r.payload));
            } else if (r.kind == K_TOGGLE_GENERAL) {
                drawToggleRow(r, I18n.t("linclient.autosprint"), ModConfig.autoSprint);
            } else if (r.kind == K_BTN) {
                drawButton(r, buttonLabel(r.code));
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

    private String buttonLabel(int code) {
        switch (code) {
            case 200: return I18n.t("linclient.gui.language") + ": " + (I18n.getLang().equals("zh_CN") ? "中文" : "EN");
            case 201: return I18n.t("linclient.gui.hudlayout");
            case 203: return I18n.t("linclient.gui.close");
            default: return "";
        }
    }

    private int findVanilla(String key) {
        for (int i = 0; i < VANILLA.length; i++) if (VANILLA[i].key.equals(key)) return i;
        return 0;
    }

    private void buildRegions() {
        regions.clear();
        int sx = 8, sy = 40, sw = 130, rh = 26, gap = 4;
        for (int i = 0; i < CATS.length; i++) {
            int ry = sy + i * (rh + gap);
            regions.add(new Region(sx, ry, sw, rh, K_CAT, null, i));
        }
        int px = sx + sw + 12, py = 40, pw = width - px - 12;
        int y = py + 8;
        int rowH = 26;
        if (selected == 0) {
            for (String id : ModConfig.MODULE_IDS) {
                regions.add(new Region(px + 10, y, pw - 20, rowH, K_TOGGLE_MODULE, id, 0));
                y += rowH + 4;
            }
        } else if (selected == 1) {
            // Two columns so all vanilla entries fit on screen.
            int colW = (pw - 20 - 8) / 2;
            int half = (VANILLA.length + 1) / 2;
            for (int i = 0; i < VANILLA.length; i++) {
                int col = i / half, row = i % half;
                regions.add(new Region(px + 10 + col * (colW + 8), y + row * (rowH + 4), colW, rowH,
                        K_TOGGLE_VANILLA, VANILLA[i].key, 0));
            }
            y += half * (rowH + 4);
        } else {
            regions.add(new Region(px + 10, y, pw - 20, rowH, K_TOGGLE_GENERAL, "autoSprint", 0));
            y += rowH + 4;
            regions.add(new Region(px + 10, y, pw - 20, rowH, K_BTN, null, 200));
            y += rowH + 4;
            regions.add(new Region(px + 10, y, pw - 20, rowH, K_BTN, null, 201));
            y += rowH + 4;
            regions.add(new Region(px + 10, y, pw - 20, rowH, K_BTN, null, 203));
            y += rowH + 4;
            fontRendererObj.drawString(I18n.t("linclient.gui.hint"), px + 10, y + 6, 0x8899AA);
        }
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
            case K_CAT:
                selected = r.code;
                break;
            case K_TOGGLE_MODULE:
                ModConfig.toggle(ModConfig.CAT_MODULES, r.payload);
                break;
            case K_TOGGLE_VANILLA:
                ModConfig.toggle(ModConfig.CAT_VANILLA, r.payload);
                break;
            case K_TOGGLE_GENERAL:
                ModConfig.toggle(ModConfig.CAT_GENERAL, r.payload);
                break;
            case K_BTN:
                switch (r.code) {
                    case 200:
                        ModConfig.setLanguage(I18n.getLang().equals("zh_CN") ? "en_US" : "zh_CN");
                        break;
                    case 201:
                        mc.displayGuiScreen(new HudEditorScreen());
                        break;
                    case 203:
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
