package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.core.Modules;
import net.minecraftforge.common.config.Configuration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Config storage: module toggles, HUD module positions, module colors. */
public class ModConfig {
    private static Configuration cfg;
    private static final Map<String, Boolean> toggles = new LinkedHashMap<String, Boolean>();
    private static final Map<String, int[]> positions = new LinkedHashMap<String, int[]>();
    private static final Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
    public static String lang = "zh_CN";

    public static void init(Configuration c) {
        cfg = c;
        for (Modules.Def d : Modules.ALL) {
            toggles.put(d.id, c.get("modules", d.id, defaultOf(d.id)).getBoolean());
        }
        for (Modules.Def d : Modules.ALL) {
            int x = c.get("positions", d.id + ".x", -1).getInt();
            int y = c.get("positions", d.id + ".y", -1).getInt();
            if (x >= 0 && y >= 0) positions.put(d.id, new int[]{x, y});
        }
        for (Modules.Def d : Modules.ALL) {
            int col = c.get("colors", d.id, -1).getInt();
            if (col != -1) colors.put(d.id, col);
        }
        lang = c.get("client", "lang", "zh_CN").getString();
    }

    private static boolean defaultOf(String id) {
        if (id.equals("hud") || id.equals("keystrokes") || id.equals("cps") || id.equals("pingfps")
                || id.equals("coords") || id.equals("armor") || id.equals("targethud")
                || id.equals("crosshair") || id.equals("sprint") || id.equals("potions")
                || id.equals("combo") || id.equals("reach")) return true;
        return false;
    }

    public static void sync() {
        for (Map.Entry<String, Boolean> e : toggles.entrySet()) {
            cfg.get("modules", e.getKey(), e.getValue()).set(e.getValue());
        }
        for (Map.Entry<String, int[]> e : positions.entrySet()) {
            cfg.get("positions", e.getKey() + ".x", -1).set(e.getValue()[0]);
            cfg.get("positions", e.getKey() + ".y", -1).set(e.getValue()[1]);
        }
        for (Map.Entry<String, Integer> e : colors.entrySet()) {
            cfg.get("colors", e.getKey(), -1).set(e.getValue().intValue());
        }
        cfg.get("client", "lang", lang).set(lang);
        if (cfg.hasChanged()) cfg.save();
    }

    public static boolean on(String id) {
        Boolean b = toggles.get(id);
        return b != null && b;
    }

    public static void set(String id, boolean v) {
        toggles.put(id, v);
        sync();
    }

    public static void toggle(String id) { set(id, !on(id)); }

    /** Custom HUD position; null = use the default layout. */
    public static int[] pos(String id) { return positions.get(id); }

    public static void setPos(String id, int x, int y) {
        positions.put(id, new int[]{x, y});
        sync();
    }

    /** Custom module accent colour; def when unset. */
    public static int color(String id, int def) {
        Integer c = colors.get(id);
        return c != null ? c.intValue() : def;
    }

    public static void setColor(String id, int c) {
        colors.put(id, c);
        sync();
    }

    public static void setLang(String l) { lang = l; sync(); }

    public static boolean isZh() { return lang != null && lang.startsWith("zh"); }
}
