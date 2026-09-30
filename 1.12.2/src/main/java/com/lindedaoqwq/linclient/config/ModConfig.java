package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.core.Modules;
import net.minecraftforge.common.config.Configuration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Config storage: module toggles, HUD module positions, module colors, module settings. */
public class ModConfig {
    private static Configuration cfg;
    private static final Map<String, Boolean> toggles = new LinkedHashMap<String, Boolean>();
    private static final Map<String, int[]> positions = new LinkedHashMap<String, int[]>();
    private static final Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
    /** Numeric slider settings, e.g. "zoom.zoom" -> 5.0. */
    private static final Map<String, Float> values = new LinkedHashMap<String, Float>();
    /** Relative HUD positions (0..1 of screen size) so layouts survive resolution changes. */
    private static final Map<String, float[]> relPositions = new LinkedHashMap<String, float[]>();
    public static String lang = "zh_CN";

    public static void init(Configuration c) {
        cfg = c;
        for (Modules.Def d : Modules.ALL) {
            toggles.put(d.id, c.get("modules", d.id, defaultOf(d.id)).getBoolean());
        }
        for (Modules.Def d : Modules.ALL) {
            double fx = c.get("relpos", d.id + ".x", -1.0).getDouble(-1.0);
            double fy = c.get("relpos", d.id + ".y", -1.0).getDouble(-1.0);
            if (fx >= 0 && fy >= 0 && fx <= 1.001 && fy <= 1.001) {
                relPositions.put(d.id, new float[]{(float) fx, (float) fy});
            }
        }
        for (Modules.Def d : Modules.ALL) {
            for (Modules.Setting s : d.settings) {
                if (s.isColor) continue;
                values.put(s.id, (float) c.get("settings", s.id, s.def).getDouble(s.def));
            }
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
        // Engine-level optimisations that were measured to be pure wins; on by default.
        if (id.equals("signtext") || id.equals("armorcache")) return true;
        return false;
    }

    public static void sync() {
        for (Map.Entry<String, Boolean> e : toggles.entrySet()) {
            cfg.get("modules", e.getKey(), e.getValue()).set(e.getValue());
        }
        for (Map.Entry<String, float[]> e : relPositions.entrySet()) {
            cfg.get("relpos", e.getKey() + ".x", -1.0).set((double) e.getValue()[0]);
            cfg.get("relpos", e.getKey() + ".y", -1.0).set((double) e.getValue()[1]);
        }
        for (Map.Entry<String, Float> e : values.entrySet()) {
            cfg.get("settings", e.getKey(), e.getValue().doubleValue()).set(e.getValue().doubleValue());
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

    /** Relative HUD position (0..1 fractions of screen size); null = default layout. */
    public static float[] relPos(String id) { return relPositions.get(id); }

    public static void setRelPos(String id, float fx, float fy) {
        relPositions.put(id, new float[]{fx, fy});
        sync();
    }

    /** Numeric module setting; def when unset. */
    public static float value(String id, float def) {
        Float v = values.get(id);
        return v != null ? v.floatValue() : def;
    }

    public static void setValue(String id, float v) {
        values.put(id, v);
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
