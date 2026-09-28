package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.gui.Theme;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.RayTraceResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Renders every HUD-information module (fixed, compact layout). */
public final class Hud {
    private Hud() { }

    public static void render(Minecraft mc) {
        EntityPlayerSP p = mc.player;
        if (p == null || mc.world == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        int fx = 8, fy = 10;

        // --- top-left info lines ---
        if (Modules.on("pingfps")) {
            fy = line(mc, fx, fy, "\u00A7bFPS: \u00A7f" + ClientState.fps
                    + "  \u00A7bPing: \u00A7f" + ClientState.ping + "ms");
        }
        if (Modules.on("cps")) {
            fy = line(mc, fx, fy, "\u00A7bCPS: \u00A7f" + ClientState.leftCps() + " | "
                    + ClientState.rightCps());
        }
        if (Modules.on("coords")) {
            fy = line(mc, fx, fy, "\u00A7bXYZ: \u00A7f" + (int) p.posX + " " + (int) p.posY + " "
                    + (int) p.posZ + " " + dirOf(p.rotationYaw));
        }

        // --- top-right stack ---
        int rx = sw - 8;
        int ry = 10;
        if (Modules.on("combo") && ClientState.combo > 0 && System.currentTimeMillis() < ClientState.comboExpire) {
            ry = lineR(mc, rx, ry, "\u00A76Combo: \u00A7f" + ClientState.combo);
        }
        if (Modules.on("reach") && ClientState.reach > 0 && System.currentTimeMillis() < ClientState.reachExpire) {
            ry = lineR(mc, rx, ry, String.format("\u00A7bReach: \u00A7f%.2fm", ClientState.reach));
        }
        if (Modules.on("damage") && System.currentTimeMillis() < ClientState.reachExpire + 200L
                && ClientState.lastDamageDealt > 0) {
            ry = lineR(mc, rx, ry, String.format("\u00A7cDMG: \u00A7f-%.1f", ClientState.lastDamageDealt));
        }
        if (Modules.on("potions")) {
            for (String s : potionLines(p.getActivePotionEffects())) {
                ry = lineR(mc, rx, ry, s);
            }
        }
        if (Modules.on("totem")) {
            int n = countTotems(p);
            if (n > 0) ry = lineR(mc, rx, ry, "\u00A7dTotems: \u00A7f" + n);
        }

        // --- target HUD (top center) ---
        if (Modules.on("targethud") && pointed(mc) instanceof EntityLivingBase) {
            renderTarget(mc, sw, (EntityLivingBase) pointed(mc));
        }

        // --- keystrokes (bottom left) ---
        if (Modules.on("keystrokes")) {
            renderKeystrokes(mc, 8, sh - 78);
        }

        // --- armor + held item (bottom right) ---
        if (Modules.on("armor")) {
            renderArmor(mc, sw, sh);
        }

        // --- custom crosshair ---
        if (Modules.on("crosshair")) {
            int cx = sw / 2, cy = sh / 2;
            RenderUtils.drawRect(cx - 4, cy, 9, 1, Theme.ACCENT);
            RenderUtils.drawRect(cx, cy - 4, 1, 9, Theme.ACCENT);
        }
    }

    private static Entity pointed(Minecraft mc) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            return mc.objectMouseOver.entityHit;
        }
        return null;
    }

    private static int countTotems(EntityPlayerSP p) {
        int n = 0;
        for (ItemStack st : p.inventory.mainInventory) {
            if (!st.isEmpty() && st.getItem() == Items.TOTEM_OF_UNDYING) n++;
        }
        if (!p.getHeldItemOffhand().isEmpty() && p.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) n++;
        return n;
    }

    private static int line(Minecraft mc, int x, int y, String s) {
        mc.fontRenderer.drawStringWithShadow(s, x, y, 0xFFFFFF);
        return y + 11;
    }

    private static int lineR(Minecraft mc, int rightX, int y, String s) {
        mc.fontRenderer.drawStringWithShadow(s, rightX - mc.fontRenderer.getStringWidth(s), y, 0xFFFFFF);
        return y + 11;
    }

    private static String dirOf(float yaw) {
        yaw = ((yaw % 360F) + 360F) % 360F;
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return "\u00A77[" + dirs[(int) ((yaw + 22.5F) / 45F) & 7] + "\u00A7f";
    }

    private static List<String> potionLines(Collection<PotionEffect> effects) {
        List<String> out = new ArrayList<String>();
        for (PotionEffect e : effects) {
            String name = I18n.format(e.getEffectName());
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            out.add("\u00A75" + name + " \u00A7f"
                    + Potion.getPotionDurationString(e, 1.0F));
        }
        return out;
    }

    private static void renderTarget(Minecraft mc, int sw, EntityLivingBase t) {
        String name = (t.isOnSameTeam(mc.player) ? "\u00A7a\u2605 " : "") + t.getName();
        int pw = 130;
        int x = sw / 2 - pw / 2, y = 26;
        Theme.roundRect(x, y, pw, 30, 0xB3141420);
        mc.fontRenderer.drawStringWithShadow(name, x + 6, y + 4, 0xFFFFFF);
        float hp = Math.max(0F, t.getHealth()), max = Math.max(1F, t.getMaxHealth());
        int barW = pw - 12;
        RenderUtils.drawRect(x + 6, y + 18, barW, 5, 0xFF333344);
        float frac = Math.min(1F, hp / max);
        int col = frac > 0.5F ? 0xFF3ADF6E : (frac > 0.25F ? 0xFFE0B03A : 0xFFE04A3A);
        RenderUtils.drawRect(x + 6, y + 18, (int) (barW * frac), 5, col);
    }

    private static void renderKeystrokes(Minecraft mc, int x, int y) {
        int b = 20, g = 3;
        boolean[] s = {
                mc.gameSettings.keyBindForward.isKeyDown(),
                mc.gameSettings.keyBindLeft.isKeyDown(),
                mc.gameSettings.keyBindBack.isKeyDown(),
                mc.gameSettings.keyBindRight.isKeyDown()
        };
        String[] l = {"W", "A", "S", "D"};
        int[][] pos = {{1, 0}, {0, 1}, {1, 1}, {2, 1}};
        for (int i = 0; i < 4; i++) {
            int kx = x + pos[i][0] * (b + g), ky = y + pos[i][1] * (b + g);
            Theme.roundRect(kx, ky, b, b, s[i] ? Theme.ACCENT_DIM : 0xB3141420);
            String t = l[i];
            mc.fontRenderer.drawStringWithShadow(t, kx + (b - mc.fontRenderer.getStringWidth(t)) / 2,
                    ky + 6, s[i] ? 0x9FD8FF : 0xFFFFFF);
        }
        int my = y + 2 * (b + g);
        String[] ml = {"LMB " + ClientState.leftCps(), "RMB " + ClientState.rightCps()};
        int[] mw = {b * 2 + g, b};
        int mx = x;
        for (int i = 0; i < 2; i++) {
            boolean held = i == 0 ? ClientState.leftHeld : ClientState.rightHeld;
            Theme.roundRect(mx, my, mw[i], b, held ? Theme.ACCENT_DIM : 0xB3141420);
            mc.fontRenderer.drawStringWithShadow(ml[i], mx + 3, my + 6, 0xFFFFFF);
            mx += mw[i] + g;
        }
    }

    private static void renderArmor(Minecraft mc, int sw, int sh) {
        EntityPlayerSP p = mc.player;
        int slot = 14, pad = 2;
        int x = sw - 8 - (slot + pad) * 4 - slot - pad;
        int y = sh - 8 - slot;
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < 4; i++) {
            drawItem(mc, p.inventory.armorItemInSlot(3 - i), x + i * (slot + pad), y, slot);
        }
        drawItem(mc, p.getHeldItemMainhand(), x + 4 * (slot + pad), y, slot);
        RenderHelper.disableStandardItemLighting();
        for (int i = 0; i < 4; i++) {
            ItemStack st = p.inventory.armorItemInSlot(3 - i);
            if (!st.isEmpty() && st.isItemStackDamageable()) {
                int pct = (int) (100F * (st.getMaxDamage() - st.getItemDamage()) / st.getMaxDamage());
                String s = pct + "%";
                mc.fontRenderer.drawStringWithShadow(s,
                        x + i * (slot + pad) + 1, y + slot + 2, pct > 40 ? 0x8FFF8F : 0xFF7F6F);
            }
        }
    }

    private static void drawItem(Minecraft mc, ItemStack st, int x, int y, int slot) {
        if (st == null || st.isEmpty()) return;
        Theme.roundRect(x, y, slot + 2, slot + 2, 0xB3141420);
        mc.getRenderItem().renderItemAndEffectIntoGUI(st, x + 1, y + 1);
        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, st, x + 1, y + 1, null);
    }
}
