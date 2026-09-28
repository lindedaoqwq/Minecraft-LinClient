package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Blue-themed main menu with ClickGUI-styled buttons (replaces the vanilla main menu). */
public class LinMainMenu extends GuiScreen {

    private static class Btn {
        int x, y, w, h, action;
        String label;
        Btn(int x, int y, int w, int h, String label, int action) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label; this.action = action;
        }
    }

    private static final int A_SINGLE = 0, A_MULTI = 1, A_OPTIONS = 2, A_LANG = 3, A_QUIT = 4, A_LINCLIENT = 5;

    private final List<Btn> buttons = new ArrayList<Btn>();

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void layout() {
        buttons.clear();
        int bw = 240, bh = 30, gap = 8;
        int cx = (this.width - bw) / 2;
        int y = this.height / 2 - 20;
        buttons.add(new Btn(cx, y, bw, bh, "Singleplayer", A_SINGLE)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, "Multiplayer", A_MULTI)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, "Options", A_OPTIONS)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, "Language", A_LANG)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, "Quit Game", A_QUIT));
        // LinClient settings accent button, bottom-left
        buttons.add(new Btn(8, this.height - 28, 150, 22, "LinClient Settings", A_LINCLIENT));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        layout();
        // Deep blue gradient background
        RenderUtils.drawVerticalGradient(0, 0, width, height, 0xFF0B1B33, 0xFF14345E);

        // Title
        GL11.glPushMatrix();
        GL11.glScalef(2.0F, 2.0F, 2.0F);
        fontRendererObj.drawStringWithShadow("LinClient", 12, 10, 0x66CCFF);
        GL11.glPopMatrix();
        fontRendererObj.drawStringWithShadow("Client-side optimisation & PvP HUD", 14, 34, 0x9FC2E8);

        for (Btn b : buttons) {
            boolean hover = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            boolean accent = b.action == A_LINCLIENT;
            int bg = accent ? 0xFF1D4B7A : (hover ? 0xFF22406B : 0xE6172942);
            Theme.roundRectBordered(b.x, b.y, b.w, b.h, bg, accent ? Theme.ACCENT : 0xFF0E1E33);
            if (hover) Theme.roundRect(b.x, b.y, 3, b.h, Theme.ACCENT);
            int tw = fontRendererObj.getStringWidth(b.label);
            fontRendererObj.drawStringWithShadow(b.label, b.x + (b.w - tw) / 2, b.y + (b.h - 8) / 2,
                    accent ? 0xBFDDFF : Theme.TEXT);
        }
        fontRendererObj.drawStringWithShadow("LinClient 1.0.0", 4, 4, 0x557799);
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
            case A_SINGLE: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
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
