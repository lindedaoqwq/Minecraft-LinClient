package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.gui.Theme;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.potion.PotionEffect;
import net.minecraft.client.resources.I18n;
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
            "fps", "cps", "ping", "memory", "coordinates", "direction",
            "speedometer", "playtime", "serveraddress", "keystrokes",
            "combo", "reach", "potion"
    };

    private static int sw, sh;

    public static void render(Minecraft mc, boolean editMode) {
        EntityPlayerSP p = mc.player;
        if (!editMode && (p == null || mc.world == null)) return;
        ScaledResolution sr = new ScaledResolution(mc);
        sw = sr.getScaledWidth();
        sh = sr.getScaledHeight();
        BOUNDS.clear();
        for (String id : IDS) {
            if (id.equals("keystrokes")) continue;   // has its own box renderer
            if (id.equals("potion")) continue;        // multi-line right column
            if (!editMode && !Modules.on(id)) continue;
            renderModule(mc, id, editMode);
        }
        if (editMode || Modules.on("keystrokes")) renderKeystrokesModule(mc, editMode);
        if (editMode || Modules.on("potion")) renderPotionModule(mc, editMode);
    }

    /** Position for a module: custom (stored relative, resolution-independent) or default anchor. */
    private static int[] posOf(Minecraft mc, String id) {
        float[] rel = ModConfig.relPos(id);
        if (rel != null) {
            return new int[]{clamp((int) (rel[0] * sw), 0, sw - 20), clamp((int) (rel[1] * sh), 0, sh - 12)};
        }
        // Left column: fps / cps / ping / memory, stacked.
        String[] left = {"fps", "cps", "ping", "memory"};
        int y = 10;
        for (String s : left) {
            if (s.equals(id)) return new int[]{8, y};
            if (Modules.on(s)) y += 11;
        }
        // Right column: coordinates / direction / speedometer / playtime / serveraddress.
        String[] right = {"coordinates", "direction", "speedometer", "playtime", "serveraddress"};
        int ry = 10;
        for (String s : right) {
            if (s.equals(id)) return new int[]{-1, ry};   // -1 = right-aligned
            if (Modules.on(s)) ry += 11;
        }
        // Combat read-outs sit above the right column's top.
        if (id.equals("combo")) return new int[]{-1, 10};
        if (id.equals("reach")) return new int[]{-1, Modules.on("combo") ? 21 : 10};
        if (id.equals("keystrokes")) return new int[]{8, sh - 78};
        if (id.equals("potion")) return new int[]{-1, 10};
        if (id.equals("crosshair")) return new int[]{sw / 2, sh / 2};
        return new int[]{8, 10};
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private static void renderModule(Minecraft mc, String id, boolean edit) {
        int[] p = posOf(mc, id);
        String s = lineText(mc, id);
        if (s == null) {
            if (!edit) return;
            s = placeholder(id);
        }
        int w = mc.fontRenderer.getStringWidth(s) + 2;
        int x = p[0] == -1 ? sw - 8 - w : p[0];
        mc.fontRenderer.drawStringWithShadow(s, x, p[1], 0xFFFFFF);
        bounds(id, x, p[1], w, 11, edit);
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
        EntityPlayerSP p = mc.player;
        if (id.equals("fps")) {
            return "\u00A7bFPS: \u00A7f" + ClientState.fps;
        }
        if (id.equals("cps")) {
            return "\u00A7bCPS: \u00A7f" + ClientState.leftCps() + " | " + ClientState.rightCps();
        }
        if (id.equals("ping")) {
            return "\u00A7bPing: \u00A7f" + ClientState.ping + "ms";
        }
        if (id.equals("memory")) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long max = rt.maxMemory() / 1048576L;
            int pct = max > 0 ? (int) (used * 100L / max) : 0;
            int warn = (int) ModConfig.value("memory.warn", 85F);
            String c = pct >= warn ? "\u00A7c" : "\u00A7b";
            return c + "Mem: \u00A7f" + used + "/" + max + "MB";
        }
        if (id.equals("coordinates") && p != null) {
            return "\u00A7bXYZ: \u00A7f" + (int) p.posX + " " + (int) p.posY + " " + (int) p.posZ;
        }
        if (id.equals("direction") && p != null) {
            return "\u00A7bDir: \u00A7f" + dirOf(p.rotationYaw);
        }
        if (id.equals("speedometer")) {
            int flag = (int) ModConfig.value("speedometer.scale", 1F);
            double v = ClientState.speed * Math.max(1, flag);
            return String.format("\u00A7bSpeed: \u00A7f%.2f b/s", v);
        }
        if (id.equals("playtime")) {
            long sec = Math.max(0L, (System.currentTimeMillis() - ClientState.sessionStart) / 1000L);
            long h = sec / 3600L, m = (sec % 3600L) / 60L, s = sec % 60L;
            return String.format("\u00A7bTime: \u00A7f%02d:%02d:%02d", h, m, s);
        }
        if (id.equals("serveraddress")) {
            net.minecraft.client.multiplayer.ServerData sd = mc.getCurrentServerData();
            if (sd == null || sd.serverIP == null || sd.serverIP.isEmpty()) return null;
            return "\u00A7bServer: \u00A7f" + sd.serverIP;
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
        return null;
    }

    private static String dirOf(float yaw) {
        yaw = ((yaw % 360F) + 360F) % 360F;
        String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return dirs[(int) ((yaw + 22.5F) / 45F) & 7];
    }

    private static List<String> potionLines(Collection<PotionEffect> effects) {
        List<String> out = new ArrayList<String>();
        for (PotionEffect e : effects) {
            String name = I18n.format(e.getEffectName());
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            out.add("\u00A75" + name + " \u00A7f" + (e.getDuration() / 20) + "s");
        }
        return out;
    }

    private static void renderPotionModule(Minecraft mc, boolean edit) {
        EntityPlayerSP pl = mc.player;
        List<String> lines = pl != null ? potionLines(pl.getActivePotionEffects()) : null;
        if (edit && (lines == null || lines.isEmpty())) {
            lines = new ArrayList<String>();
            lines.add(placeholder("potion"));
        }
        if (lines == null || lines.isEmpty()) return;
        int[] p = posOf(mc, "potion");
        int y = p[1];
        for (String s : lines) {
            int w = mc.fontRenderer.getStringWidth(s) + 2;
            int x = sw - 8 - w;
            mc.fontRenderer.drawStringWithShadow(s, x, y, 0xFFFFFF);
            y += 11;
        }
        // Single drag box covering all lines.
        bounds("potion", sw - 8 - mc.fontRenderer.getStringWidth(lines.get(0)) - 2,
                p[1], mc.fontRenderer.getStringWidth(lines.get(0)) + 2, 11 * lines.size(), edit);
    }

    private static void renderKeystrokesModule(Minecraft mc, boolean edit) {
        int[] p = posOf(mc, "keystrokes");
        int[] wh = renderKeystrokes(mc, p[0], p[1]);
        bounds("keystrokes", p[0], p[1], wh[0], wh[1], edit);
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
            mc.fontRenderer.drawStringWithShadow(l[i], kx + (b - mc.fontRenderer.getStringWidth(l[i])) / 2,
                    ky + 6, s[i] ? 0x9FD8FF : 0xFFFFFF);
        }
        int my = y + 2 * (b + g);
        String[] ml = {"LMB " + ClientState.leftCps(), "RMB " + ClientState.rightCps()};
        int[] mw = {b * 2 + g, b};
        int mx = x;
        for (int i = 0; i < 2; i++) {
            boolean held = i == 0 ? ClientState.leftHeld : ClientState.rightHeld;
            Theme.roundRect(mx, my, mw[i], b, held ? hi : 0xB3141420);
            mc.fontRenderer.drawStringWithShadow(ml[i], mx + 3, my + 6, 0xFFFFFF);
            mx += mw[i] + g;
        }
        return new int[]{b * 3 + g * 2, b * 3 + g * 2};
    }
}
