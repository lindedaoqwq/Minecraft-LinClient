package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.core.Modules;
import net.minecraftforge.common.config.Configuration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Config storage: one boolean per module id + client settings. */
public class ModConfig {
    private static Configuration cfg;
    private static final Map<String, Boolean> toggles = new LinkedHashMap<String, Boolean>();
    public static String lang = "zh_CN";

    public static void init(Configuration c) {
        cfg = c;
        for (Modules.Def d : Modules.ALL) {
            toggles.put(d.id, c.get("modules", d.id, defaultOf(d.id)).getBoolean());
        }
        lang = c.get("client", "lang", "zh_CN").getString();
    }

    private static boolean defaultOf(String id) {
        // HUD info panels default ON; modifiers/perf switches default sensibly.
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

    public static void setLang(String l) { lang = l; sync(); }
}
