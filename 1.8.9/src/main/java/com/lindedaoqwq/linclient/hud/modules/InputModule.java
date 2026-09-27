package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.KeyBinding;

import java.util.List;

/**
 * Input module: draws a compact keyboard / mouse pad (W/A/S/D, LMB, RMB, Space) with the keys
 * highlighted while held, plus live left/right CPS.
 */
public class InputModule extends HudModule {
    private static final int BOX = 18;
    private static final int GAP = 4;

    public InputModule() {
        super("input", "linclient.module.input", 0xFFFF55);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        return lines();
    }

    @Override
    public int contentWidth(Minecraft mc) {
        FontRenderer fr = mc.fontRendererObj;
        String l1 = I18n.t("linclient.input.cpsL", 0);
        String l2 = I18n.t("linclient.input.cpsR", 0);
        int cpsW = Math.max(fr.getStringWidth(l1), fr.getStringWidth(l2)) + 8;
        return Math.max(BOX * 3 + GAP * 2, cpsW) + 6;
    }

    @Override
    public int contentHeight(Minecraft mc) {
        return (BOX + GAP) * 3 + BOX + 28;
    }

    @Override
    protected void drawContent(Minecraft mc, FontRenderer fr, int x, int y, int w, int h) {
        KeyBinding fwd = mc.gameSettings.keyBindForward;
        KeyBinding left = mc.gameSettings.keyBindLeft;
        KeyBinding back = mc.gameSettings.keyBindBack;
        KeyBinding right = mc.gameSettings.keyBindRight;
        KeyBinding jump = mc.gameSettings.keyBindJump;

        // W above the A/S/D row
        int asdY = y + BOX + GAP;
        drawKey(fr, x + BOX + GAP, y, "W", fwd.isKeyDown());
        drawKey(fr, x, asdY, "A", left.isKeyDown());
        drawKey(fr, x + BOX + GAP, asdY, "S", back.isKeyDown());
        drawKey(fr, x + BOX * 2 + GAP * 2, asdY, "D", right.isKeyDown());

        // LMB / RMB row
        int lrY = asdY + BOX + GAP;
        drawKey(fr, x, lrY, I18n.t("linclient.input.lmb"), ClientState.leftHeld);
        drawKey(fr, x + BOX + GAP, lrY, I18n.t("linclient.input.rmb"), ClientState.rightHeld);

        // Space bar (wide)
        int spY = lrY + BOX + GAP;
        drawKey(fr, x, spY, I18n.t("linclient.input.space"), jump.isKeyDown(), BOX * 3 + GAP * 2, BOX);

        // CPS readout
        int cpsY = spY + BOX + 6;
        fr.drawString(I18n.t("linclient.input.cpsL", ClientState.leftCps()), x, cpsY, color);
        fr.drawString(I18n.t("linclient.input.cpsR", ClientState.rightCps()), x, cpsY + 10, color);
    }

    private void drawKey(FontRenderer fr, int x, int y, String label, boolean pressed) {
        drawKey(fr, x, y, label, pressed, BOX, BOX);
    }

    private void drawKey(FontRenderer fr, int x, int y, String label, boolean pressed, int w, int h) {
        int bg = pressed ? 0xFF3A6EA5 : 0xFF222833;
        int border = pressed ? 0xFF7FB2E5 : 0xFF4A5366;
        RenderUtils.drawPanel(x, y, w, h, bg, border);
        int tw = fr.getStringWidth(label);
        fr.drawString(label, x + (w - tw) / 2, y + (h - 8) / 2, pressed ? 0xFFFFFF : 0xCCCCCC);
    }
}
