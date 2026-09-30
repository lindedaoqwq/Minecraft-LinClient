package com.lindedaoqwq.linclient.gui;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.FontUtils;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Main menu: vanilla panorama backdrop (darkened), "LinClient" title in the bundled
 *  HarmonyOS font, and a single central button column: Singleplayer / Multiplayer /
 *  Options / Quit. No extra corner buttons. */
public class LinMainMenu extends GuiScreen {

    private static final int A_SINGLE = 0, A_MULTI = 1, A_OPTIONS = 2, A_QUIT = 3;

    private static class Btn {
        int x, y, w, h, action;
        String label;
        Btn(int x, int y, int w, int h, String label, int action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.action = action;
        }
    }

    private static final ResourceLocation PANORAMA =
            new ResourceLocation("textures/gui/title/background/panorama_0.png");

    private final List<Btn> buttons = new ArrayList<Btn>();
    private boolean zh;

    private static final int ACCENT = 0xFF3FA9FF, ACCENT_DEEP = 0xFF1B6FC4;
    private static final int BTN_IDLE = 0xB312305C, BTN_HOVER = 0xE61B4C86;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
        buttons.clear();
        String[] labels = zh
                ? new String[]{"\u5355\u4eba\u6e38\u620f", "\u591a\u4eba\u6e38\u620f",
                               "\u8bbe\u7f6e", "\u9000\u51fa\u6e38\u620f"}
                : new String[]{"Singleplayer", "Multiplayer", "Options", "Quit Game"};
        int bw = 260, bh = 34, gap = 10;
        int cx = (this.width - bw) / 2;
        int y = this.height / 2 - 16;
        buttons.add(new Btn(cx, y, bw, bh, labels[0], A_SINGLE)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[1], A_MULTI)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[2], A_OPTIONS)); y += bh + gap;
        buttons.add(new Btn(cx, y, bw, bh, labels[3], A_QUIT));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontRenderer fr = FontUtils.get();

        // ---- vanilla panorama backdrop, slowly drifting, darkened ----
        mc.getTextureManager().bindTexture(PANORAMA);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        float t = (Minecraft.getSystemTime() % 60000L) / 60000F;
        int u = (int) (96D + Math.sin(t * Math.PI * 2.0D) * 40D);
        Gui.drawScaledCustomSizeModalRect(0, 0, u, u / 2, 128, 128, width, height, 256, 256);
        // dark veil for readability
        RenderUtils.drawRect(0, 0, width, height, 0x6E0A1220);

        // ---- Title: "LinClient" in the bundled HarmonyOS Sans Black ----
        GL11.glPushMatrix();
        float scale = 2.6F;
        GL11.glScalef(scale, scale, scale);
        int tw = fr.getStringWidth("LinClient");
        int tx = (int) ((width / 2F - tw * scale / 2F) / scale);
        int ty = (int) ((height / 2F - 92F) / scale);
        fr.drawStringWithShadow("LinClient", tx + 2, ty + 2, 0x66000000);
        fr.drawStringWithShadow("LinClient", tx, ty, ACCENT);
        GL11.glPopMatrix();

        // Accent underline + subtitle.
        int uw = (int) (tw * scale) + 40;
        int ux = width / 2 - uw / 2;
        int uy = height / 2 - 92 + 38;
        RenderUtils.drawVerticalGradient(ux, uy, uw, 2, ACCENT, ACCENT_DEEP);
        String sub = zh ? "\u5ba2\u6237\u7aef\u4f18\u5316 & PvP HUD" : "Client-side optimisation & PvP HUD";
        fr.drawStringWithShadow(sub, width / 2 - fr.getStringWidth(sub) / 2, uy + 8, 0xFF9FC2E8);

        // ---- Buttons ----
        for (Btn b : buttons) {
            boolean hover = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            RenderUtils.drawPanel(b.x, b.y, b.w, b.h, hover ? BTN_HOVER : BTN_IDLE,
                    hover ? ACCENT : 0x661E3A5F);
            if (hover) RenderUtils.drawRect(b.x, b.y, 3, b.h, ACCENT);
            int lw = fr.getStringWidth(b.label);
            fr.drawStringWithShadow(b.label, b.x + (b.w - lw) / 2, b.y + (b.h - 8) / 2,
                    hover ? 0xFFFFFFFF : 0xFFD6E7FA);
        }

        // Footer
        String ver = "LinClient 1.0.0";
        fr.drawStringWithShadow(ver, 6, height - 12, 0xFF7A93B4);
        fr.drawStringWithShadow("Minecraft", width - fr.getStringWidth("Minecraft") - 6,
                height - 12, 0xFF7A93B4);
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
            case A_QUIT: mc.shutdown(); break;
            default: break;
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) mc.displayGuiScreen(null);
    }
}
