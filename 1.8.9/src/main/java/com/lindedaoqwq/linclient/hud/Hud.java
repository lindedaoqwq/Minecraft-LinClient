package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.gui.Theme;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StatCollector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders every HUD-information module. Each module has a draggable position. */
public final class Hud {
    private Hud() { }

    /** Rendered bounds per module id: {x, y, w, h}. Refreshed every frame. */
    public static final Map<String, int[]> BOUNDS = new HashMap<String, int[]>();

    /** All on-screen module ids, in render order. */
    private static final String[] IDS = {
            "pingfps", "cps", "coords", "combo", "reach", "damage", "potions",
            "targethud", "keystrokes", "armor", "crosshair"
    };

    private static int sw, sh;

    public static void render(Minecraft mc, boolean editMode) {
        EntityPlayerSP p = mc.thePlayer;
        if (!editMode && (p == null || mc.theWorld == null)) return;
        ScaledResolution sr = new ScaledResolution(mc);
        sw = sr.getScaledWidth();
        sh = sr.getScaledHeight();
        BOUNDS.clear();
        for (String id : IDS) {
            if (id.equals("crosshair")) continue;   // center-locked, no drag box
            if (!editMode && !Modules.on(id)) continue;
            renderModule(mc, id, editMode);
        }
        if (!editMode && Modules.on("crosshair")) renderCrosshair(mc);
    }

    /** Position for a module: custom (stored) or default anchor. */
    private static int[] posOf(Minecraft mc, String id) {
        int[] cust = ModConfig.pos(id);
        if (cust != null) return new int[]{clamp(cust[0], 0, sw - 20), clamp(cust[1], 0, sh - 12)};
        if (id.equals("pingfps") || id.equals("cps") || id.equals("coords")) {
            int y = 10;
            if (id.equals("cps")) y = Modules.on("pingfps") ? 21 : 10;
            if (id.equals("coords")) y = (Modules.on("pingfps") ? 21 : 10) + (Modules.on("cps") ? 11 : 0);
            return new int[]{8, y};
        }
        if (id.equals("combo") || id.equals("reach") || id.equals("damage") || id.equals("potions")) {
            int y = 10;
            if (id.equals("reach")) y = Modules.on("combo") ? 21 : 10;
            if (id.equals("damage")) y = (Modules.on("combo") ? 21 : 10) + (Modules.on("reach") ? 11 : 0);
            if (id.equals("potions")) {
                y = 10 + (Modules.on("combo") ? 11 : 0) + (Modules.on("reach") ? 11 : 0)
                        + (Modules.on("damage") ? 11 : 0);
            }
            return new int[]{-1, y};   // -1 = right-aligned
        }
        if (id.equals("targethud")) return new int[]{sw / 2 - 65, 26};
        if (id.equals("keystrokes")) return new int[]{8, sh - 78};
        if (id.equals("armor")) return new int[]{-2, -2};   // -2 = bottom-right
        return new int[]{8, 10};
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private static void renderModule(Minecraft mc, String id, boolean edit) {
        int[] p = posOf(mc, id);
        if (id.equals("keystrokes")) {
            int[] wh = renderKeystrokes(mc, p[0], p[1]);
            bounds(id, p[0], p[1], wh[0], wh[1], edit);
        } else if (id.equals("armor")) {
            int[] xy = p[0] == -2 ? new int[]{sw - 8 - 80, sh - 8 - 16} : p;
            int[] wh = renderArmor(mc, xy[0], xy[1], edit);
            bounds(id, xy[0], xy[1], wh[0], wh[1], edit);
        } else if (id.equals("targethud")) {
            EntityLivingBase t = mc.pointedEntity instanceof EntityLivingBase ? (EntityLivingBase) mc.pointedEntity : null;
            if (t != null || edit) {
                int[] wh = renderTarget(mc, p[0], p[1], t);
                bounds(id, p[0], p[1], wh[0], wh[1], edit);
            }
        } else if (id.equals("potions")) {
            EntityPlayerSP pl = mc.thePlayer;
            List<String> lines = pl != null ? potionLines(pl.getActivePotionEffects()) : null;
            if (edit && (lines == null || lines.isEmpty())) {
                lines = new ArrayList<String>();
                lines.add(placeholder("potions"));
            }
            if (lines == null) return;
            int y = p[1];
            for (String s : lines) {
                int w = mc.fontRendererObj.getStringWidth(s) + 2;
                mc.fontRendererObj.drawStringWithShadow(s, sw - 8 - w, y, 0xFFFFFF);
                bounds(id, sw - 8 - w, y, w, 11, edit);
                y += 11;
            }
        } else {
            String s = lineText(mc, id);
            if (s == null) {
                if (!edit) return;
                s = placeholder(id);
            }
            int w = mc.fontRendererObj.getStringWidth(s) + 2;
            int x = p[0] == -1 ? sw - 8 - w : p[0];
            mc.fontRendererObj.drawStringWithShadow(s, x, p[1], 0xFFFFFF);
            bounds(id, x, p[1], w, 11, edit);
        }
    }

    /** Bilingual preview label used in HUD edit mode when live data is unavailable. */
    private static String placeholder(String id) {
        Modules.Def d = Modules.byId(id);
        boolean zh = ModConfig.isZh();
        String name = d == null ? id : (zh ? d.labelZh : d.label);
        return zh ? "[" + name + " \u9884\u89c8]" : "[" + name + " preview]";
    }

    private static void bounds(String id, int x, int y, int w, int h, boolean edit) {
        BOUNDS.put(id, new int[]{x, y, w, h});
        if (edit) {
            RenderUtils.drawRectOutline(x - 3, y - 3, w + 6, h + 6, 0xFF55AAFF);
        }
    }

    private static String lineText(Minecraft mc, String id) {
        EntityPlayerSP p = mc.thePlayer;
        if (id.equals("pingfps")) {
            return "\u00A7bFPS: \u00A7f" + ClientState.fps + "  \u00A7bPing: \u00A7f" + ClientState.ping + "ms";
        }
        if (id.equals("cps")) {
            return "\u00A7bCPS: \u00A7f" + ClientState.leftCps() + " | " + ClientState.rightCps();
        }
        if (id.equals("coords") && p != null) {
            return "\u00A7bXYZ: \u00A7f" + (int) p.posX + " " + (int) p.posY + " " + (int) p.posZ
                    + " " + dirOf(p.rotationYaw);
        }
        long now = System.currentTimeMillis();
        if (id.equals("combo")) {
            if (ClientState.combo > 0 && now < ClientState.comboExpire) {
                return "\u00A76Combo: \u00A7f" + ClientState.combo;
            }
            return null;
        }
        if (id.equals("reach")) {
            if (ClientState.reach > 0 && now < ClientState.reachExpire) {
                return String.format("\u00A7bReach: \u00A7f%.2fm", ClientState.reach);
            }
            return null;
        }
        if (id.equals("damage")) {
            if (now < ClientState.reachExpire + 200L && ClientState.lastDamageDealt > 0) {
                return String.format("\u00A7cDMG: \u00A7f-%.1f", ClientState.lastDamageDealt);
            }
            return null;
        }
        return null;
    }

    private static String dirOf(float yaw) {
        yaw = ((yaw % 360F) + 360F) % 360F;
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return "\u00A77[" + dirs[(int) ((yaw + 22.5F) / 45F) & 7] + "\u00A7f";
    }

    private static List<String> potionLines(Collection<PotionEffect> effects) {
        List<String> out = new ArrayList<String>();
        for (PotionEffect e : effects) {
            String name = StatCollector.translateToLocal(e.getEffectName());
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            out.add("\u00A75" + name + " \u00A7f" + (e.getDuration() / 20) + "s");
        }
        return out;
    }

    private static int[] renderTarget(Minecraft mc, int x, int y, EntityLivingBase t) {
        int pw = 130, ph = 30;
        if (t == null) {   // edit-mode placeholder
            Theme.roundRect(x, y, pw, ph, 0xB3141420);
            mc.fontRendererObj.drawStringWithShadow("Target HUD", x + 6, y + 4, 0xFFFFFF);
            return new int[]{pw, ph};
        }
        String name = (t.isOnSameTeam(mc.thePlayer) ? "\u00A7a\u2605 " : "") + t.getName();
        Theme.roundRect(x, y, pw, ph, 0xB3141420);
        mc.fontRendererObj.drawStringWithShadow(name, x + 6, y + 4, 0xFFFFFF);
        float hp = Math.max(0F, t.getHealth()), max = Math.max(1F, t.getMaxHealth());
        int barW = pw - 12;
        RenderUtils.drawRect(x + 6, y + 18, barW, 5, 0xFF333344);
        float frac = Math.min(1F, hp / max);
        // Vanilla-style red health bar (colour customisable via right-click).
        int col = ModConfig.color("targethud", 0xFFE02F2F);
        RenderUtils.drawRect(x + 6, y + 18, (int) (barW * frac), 5, col);
        return new int[]{pw, ph};
    }

    private static int[] renderKeystrokes(Minecraft mc, int x, int y) {
        int b = 20, g = 3;
        int hi = ModConfig.color("keystrokes", Theme.ACCENT_DIM);
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
            Theme.roundRect(kx, ky, b, b, s[i] ? hi : 0xB3141420);
            mc.fontRendererObj.drawStringWithShadow(l[i], kx + (b - mc.fontRendererObj.getStringWidth(l[i])) / 2,
                    ky + 6, s[i] ? 0x9FD8FF : 0xFFFFFF);
        }
        int my = y + 2 * (b + g);
        String[] ml = {"LMB " + ClientState.leftCps(), "RMB " + ClientState.rightCps()};
        int[] mw = {b * 2 + g, b};
        int mx = x;
        for (int i = 0; i < 2; i++) {
            boolean held = i == 0 ? ClientState.leftHeld : ClientState.rightHeld;
            Theme.roundRect(mx, my, mw[i], b, held ? hi : 0xB3141420);
            mc.fontRendererObj.drawStringWithShadow(ml[i], mx + 3, my + 6, 0xFFFFFF);
            mx += mw[i] + g;
        }
        return new int[]{b * 3 + g * 2, b * 3 + g * 2};
    }

    private static int[] renderArmor(Minecraft mc, int x, int y, boolean edit) {
        EntityPlayerSP p = mc.thePlayer;
        if (p == null) {
            // Main-menu preview box so the module stays draggable.
            Theme.roundRect(x, y, 80, 18, 0xB3141420);
            mc.fontRendererObj.drawStringWithShadow(placeholder("armor"), x + 3, y + 5, 0xFFFFFF);
            return new int[]{80, 18};
        }
        int slot = 14, pad = 2;
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < 4; i++) {
            drawItem(mc, p.getCurrentArmor(3 - i), x + i * (slot + pad), y, slot);
        }
        drawItem(mc, p.inventory.getCurrentItem(), x + 4 * (slot + pad), y, slot);
        RenderHelper.disableStandardItemLighting();
        for (int i = 0; i < 4; i++) {
            ItemStack st = p.getCurrentArmor(3 - i);
            if (st != null && st.isItemStackDamageable()) {
                int pct = (int) (100F * (st.getMaxDamage() - st.getItemDamage()) / st.getMaxDamage());
                String s = pct + "%";
                mc.fontRendererObj.drawStringWithShadow(s,
                        x + i * (slot + pad) + 1, y + slot + 2, pct > 40 ? 0x8FFF8F : 0xFF7F6F);
            }
        }
        return new int[]{(slot + pad) * 5, slot + 2};
    }

    private static void drawItem(Minecraft mc, ItemStack st, int x, int y, int slot) {
        if (st == null) return;
        Theme.roundRect(x, y, slot + 2, slot + 2, 0xB3141420);
        mc.getRenderItem().renderItemAndEffectIntoGUI(st, x + 1, y + 1);
        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, st, x + 1, y + 1, null);
    }

    private static void renderCrosshair(Minecraft mc) {
        int cx = sw / 2, cy = sh / 2;
        int col = ModConfig.color("crosshair", 0xFF3AA6F0);
        RenderUtils.drawRect(cx - 4, cy, 9, 1, col);
        RenderUtils.drawRect(cx, cy - 4, 1, 9, col);
    }
}
