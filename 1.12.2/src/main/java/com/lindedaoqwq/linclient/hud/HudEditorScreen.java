package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
import java.util.Map;

/** HUD layout editor: drag enabled modules to reposition them. Positions persist. */
public class HudEditorScreen extends GuiScreen {
    private String dragging = null;
    private int grabDx, grabDy;
    private boolean zh;

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        zh = ModConfig.isZh();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dim backdrop (no super.drawScreen: works identically in-game and at the main menu).
        RenderUtils.drawRect(0, 0, width, height, 0x60000000);
        // Render enabled modules with drag outlines (placeholders when no live data).
        Hud.render(mc, true);
        String tip = zh ? "\u62d6\u52a8\u4ee5\u8c03\u6574\u4f4d\u7f6e\uff0c\u6309 ESC \u4fdd\u5b58\u5e76\u9000\u51fa"
                : "Drag to move. Press ESC to save & close";
        mc.fontRenderer.drawStringWithShadow(tip, 6, height - 14, 0xFFAAFFAA);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging == null) {
            // Pick the topmost module whose bounds contain the point.
            Map<String, int[]> b = Hud.BOUNDS;
            String[] order = {"crosshair", "targethud", "keystrokes", "armor", "potions",
                    "damage", "reach", "combo", "coords", "cps", "pingfps"};
            for (String id : order) {
                int[] r = b.get(id);
                if (r == null) continue;
                if (mouseX >= r[0] - 3 && mouseX <= r[0] + r[2] + 3
                        && mouseY >= r[1] - 3 && mouseY <= r[1] + r[3] + 3) {
                    dragging = id;
                    grabDx = mouseX - r[0];
                    grabDy = mouseY - r[1];
                    break;
                }
            }
        }
        if (dragging != null) {
            ModConfig.setPos(dragging,
                    clamp(mouseX - grabDx, 0, width - 10),
                    clamp(mouseY - grabDy, 0, height - 12));
        }
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
        ModConfig.sync();
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        super.keyTyped(c, key);
        if (key == 1) mc.displayGuiScreen(null);
    }
}
