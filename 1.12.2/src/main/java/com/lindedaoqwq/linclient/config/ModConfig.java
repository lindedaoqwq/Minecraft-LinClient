package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.LinClient;
import net.minecraftforge.common.config.Configuration;

/**
 * Global configuration backed by Forge's {@link Configuration} (the 1.8.9 / 1.12.2 native system).
 *
 * Values are read live, so toggling them in the in-game config menu (opened with Right Shift)
 * takes effect immediately. Editing the generated {@code linclient.cfg} also works.
 */
public class ModConfig {
    private static Configuration cfg;

    public static final String CAT_GENERAL = "general";
    public static final String CAT_VISUAL = "visual";
    public static final String CAT_PERF = "performance";
    public static final String CAT_MODULES = "modules";

    // general
    public static boolean enableMod;
    public static boolean showHud;

    // visual
    public static boolean fullbright;
    public static boolean disableClouds;
    public static boolean disableFireOverlay;
    public static boolean disableWaterOverlay;

    // performance
    public static boolean dynamicFps;
    public static int dynamicFpsMin;
    public static int dynamicFpsRestore;
    public static int dynamicFpsMinDist;
    public static int dynamicFpsMaxDist;

    // modules
    public static boolean modSelf, modEnvironment, modItem, modInput, modOther, modEntity, modCombat, modBoss;

    public static void init(Configuration config) {
        cfg = config;
        cfg.load();
        sync();
        ClientState.modActive = enableMod;
        ClientState.hudEnabled = showHud;
    }

    public static void sync() {
        enableMod = cfg.getBoolean("enableMod", CAT_GENERAL, true, "Master switch for LinClient.");
        showHud = cfg.getBoolean("showHud", CAT_GENERAL, true, "Show the HUD overlay.");

        fullbright = cfg.getBoolean("fullbright", CAT_VISUAL, false, "Force maximum brightness (gamma).");
        disableClouds = cfg.getBoolean("disableClouds", CAT_VISUAL, false, "Hide clouds.");
        disableFireOverlay = cfg.getBoolean("disableFireOverlay", CAT_VISUAL, false, "Hide the fire screen overlay.");
        disableWaterOverlay = cfg.getBoolean("disableWaterOverlay", CAT_VISUAL, false, "Hide the water screen overlay.");

        dynamicFps = cfg.getBoolean("dynamicFps", CAT_PERF, false, "Auto-lower render distance when FPS drops.");
        dynamicFpsMin = cfg.getInt("dynamicFpsMin", CAT_PERF, 30, 1, 240, "FPS below this triggers a downgrade.");
        dynamicFpsRestore = cfg.getInt("dynamicFpsRestore", CAT_PERF, 50, 1, 240, "FPS above this restores the distance.");
        dynamicFpsMinDist = cfg.getInt("dynamicFpsMinDist", CAT_PERF, 4, 2, 32, "Render distance used during a downgrade.");
        dynamicFpsMaxDist = cfg.getInt("dynamicFpsMaxDist", CAT_PERF, 12, 2, 32, "Render distance to restore to.");

        modSelf = cfg.getBoolean("self_status", CAT_MODULES, true, "Enable Self Status module.");
        modEnvironment = cfg.getBoolean("environment", CAT_MODULES, true, "Enable Environment module.");
        modItem = cfg.getBoolean("item", CAT_MODULES, true, "Enable Item module.");
        modInput = cfg.getBoolean("input", CAT_MODULES, true, "Enable Input module.");
        modOther = cfg.getBoolean("other", CAT_MODULES, true, "Enable Other module.");
        modEntity = cfg.getBoolean("entity", CAT_MODULES, true, "Enable Entity module.");
        modCombat = cfg.getBoolean("combat", CAT_MODULES, true, "Enable Combat module.");
        modBoss = cfg.getBoolean("boss", CAT_MODULES, true, "Enable Boss module.");

        if (cfg.hasChanged()) cfg.save();
    }

    /** Flip a boolean setting and persist it. */
    public static void toggle(String category, String name) {
        boolean v = cfg.getBoolean(name, category, false, "");
        v = !v;
        cfg.get(category, name, v).set(v);
        cfg.save();
        sync();
    }

    public static void save() {
        if (cfg != null && cfg.hasChanged()) cfg.save();
    }

    public static Configuration get() {
        return cfg;
    }
}
