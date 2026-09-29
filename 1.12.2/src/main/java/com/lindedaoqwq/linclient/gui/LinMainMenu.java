package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.FontUtils;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Beautified blue-themed main menu. Title: LinClient, drawn with the bundled HarmonyOS font. */
public class LinMainMenu extends GuiScreen {

    private static final int A_SINGLE = 0, A_MULTI = 1, A_OPTIONS = 2, A_LANG = 3, A_QUIT = 4, A_LINCLIENT = 5;

    private static class Btn {
        int x, y, w, h, action;
        String label;
        Btn(int x, int y, int w, int h, String label, int action) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label; this.action = action;
        }
    }

    private final List<Btn> buttons = new ArrayList<Btn>();
    private boolean zh;

    // Blue palette.
    private static final int BG_TOP = 0xFF071427, BG_BOTTOM = 0xFF0E2B52;
    private static final int ACCENT = 0xFF3FA9FF, ACCENT_DEEP = 0xFF1B6FC4;
    private static final int BTN_IDLE = 0xCC12305C, BTN_HOVER = 0xE61B4C86;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
        buttons.clear();
        String[] labels = zh
                ? new String[]{"\u5355\u4eba\u6e38\u620f", "\u591a\u4eba\u6e38\u620f", "\u9009\u9879",
                               "\u8bed\u8a00", "\u9000\u51fa\u6e38\u620f", "LinClient \u8bbe\u7f6e"}
                : new String[]{"Singleplayer", "Multiplayer", "Options", "Language", "Quit Game",
                               "LinClient Settings"};
        int bw = 260, bh = 32, gap = 10;
        int cx = (this.width - bw) / 2;
        int y = this.height / 2 - 34;
        buttons.add(new Btn(cx, y, bw, bh, labels[0], A_SINGLE)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[1], A_MULTI)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[2], A_OPTIONS)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[3], A_LANG)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[4], A_QUIT));
        buttons.add(new Btn(14, this.height - 36, 150, 24, labels[5], A_LINCLIENT));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontRenderer fr = FontUtils.get();

        // Backdrop: blue gradient + faint horizontal light bands.
        RenderUtils.drawVerticalGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
        for (int i = 0; i < 6; i++) {
            RenderUtils.drawRect(0, height - 120 + i * 20, width, 1, 0x14FFFFFF);
        }

        // ---- Title: "LinClient" in the bundled HarmonyOS Sans Black ----
        GL11.glPushMatrix();
        float scale = 3.0F;
        GL11.glScalef(scale, scale, scale);
        int tw = fr.getStringWidth("LinClient");
        int tx = (int) ((width / 2F - tw * scale / 2F) / scale);
        int ty = (int) ((height / 2F - 110F) / scale);
        fr.drawStringWithShadow("LinClient", tx + 2, ty + 2, 0x50000000);
        fr.drawStringWithShadow("LinClient", tx, ty, ACCENT);
        GL11.glPopMatrix();

        // Accent underline + subtitle.
        int uw = (int) (tw * scale) + 40;
        int ux = width / 2 - uw / 2;
        int uy = height / 2 - 110 + 42;
        RenderUtils.drawVerticalGradient(ux, uy, uw, 2, ACCENT, ACCENT_DEEP);
        String sub = zh ? "\u5ba2\u6237\u7aef\u4f18\u5316 & PvP HUD" : "Client-side optimisation & PvP HUD";
        fr.drawStringWithShadow(sub, width / 2 - fr.getStringWidth(sub) / 2, uy + 8, 0xFF8FB6DE);

        // ---- Buttons ----
        for (Btn b : buttons) {
            boolean hover = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            boolean accent = b.action == A_LINCLIENT;
            int bg = accent ? (hover ? 0xF01B6FC4 : 0xE01B4E8C) : (hover ? BTN_HOVER : BTN_IDLE);
            RenderUtils.drawPanel(b.x, b.y, b.w, b.h, bg, hover ? ACCENT : 0xFF1E3A5F);
            if (hover) RenderUtils.drawRect(b.x, b.y, 3, b.h, ACCENT);
            int lw = fr.getStringWidth(b.label);
            fr.drawStringWithShadow(b.label, b.x + (b.w - lw) / 2, b.y + (b.h - 8) / 2,
                    hover ? 0xFFFFFFFF : 0xFFD6E7FA);
        }

        // Footer
        String ver = "LinClient 1.0.0";
        fr.drawStringWithShadow(ver, width - fr.getStringWidth(ver) - 6, height - 12, 0xFF5A86B8);
        fr.drawStringWithShadow("Minecraft", 6, height - 12, 0xFF5A86B8);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        for (Btn b : buttons) {
            if (mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h) {
                act(b.action);
                return;
            }
        }
    }

    private void act(int a) {
        Minecraft mc = Minecraft.getMinecraft();
        switch (a) {
            case A_SINGLE: mc.displayGuiScreen(new GuiWorldSelection(this)); break;
            case A_MULTI: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case A_OPTIONS: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case A_LANG: mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager())); break;
            case A_QUIT: mc.shutdown(); break;
            case A_LINCLIENT: mc.displayGuiScreen(new ClickGuiScreen()); break;
            default: break;
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) mc.displayGuiScreen(null);
    }
}
