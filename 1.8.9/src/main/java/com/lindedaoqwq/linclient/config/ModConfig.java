package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraftforge.common.config.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global configuration backed by Forge's {@link Configuration} (the 1.8.9 / 1.12.2 native system).
 *
 * Values are read live, so toggling them in the in-game GUI (opened with Right Shift, or the
 * LinClient buttons in the pause / settings menus) takes effect immediately.
 */
public class ModConfig {
    private static Configuration cfg;

    public static final String CAT_GENERAL = "general";
    public static final String CAT_VISUAL = "visual";
    public static final String CAT_PERF = "performance";
    public static final String CAT_MODULES = "modules";
    public static final String CAT_VANILLA = "vanilla_hud";
    public static final String CAT_POS = "hud_positions";

    public static final List<String> MODULE_IDS = Arrays.asList(
            "self_status", "environment", "item", "input", "other", "entity", "combat");

    // general
    public static boolean enableMod;
    public static boolean showHud;
    public static String language = "zh_CN";
    public static boolean autoSprint;

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
    public static boolean smartFps;
    public static boolean fastGraphics;
    public static boolean noSmoothLight;
    public static boolean lowParticles;

    // modules
    public static boolean modSelf, modEnvironment, modItem, modInput, modOther, modEntity, modCombat;

    // vanilla HUD element visibility: true = SHOWN (leave on screen), false = HIDDEN.
    public static boolean showHealth, showArmor, showFood, showAir, showHotbar, showExp,
            showCrosshair, showBoss, showPotion, showVignette, showPortal, showHelmet, showJumpbar;

    private static final Map<String, int[]> modulePos = new HashMap<>();

    public static void init(Configuration config) {
        cfg = config;
        cfg.load();
        sync();
        loadModulePositions();
        I18n.setLang(language);
        ClientState.modActive = enableMod;
        ClientState.hudEnabled = showHud;
    }

    public static void sync() {
        enableMod = cfg.getBoolean("enableMod", CAT_GENERAL, true, "Master switch for LinClient.");
        showHud = cfg.getBoolean("showHud", CAT_GENERAL, true, "Show the HUD overlay.");
        language = cfg.getString("language", CAT_GENERAL, "zh_CN", "UI language: zh_CN or en_US.");
        autoSprint = cfg.getBoolean("autoSprint", CAT_GENERAL, false,
                "Auto sprint when moving forward (vanilla sprint, no speed hack).");

        fullbright = cfg.getBoolean("fullbright", CAT_VISUAL, false, "Force maximum brightness (gamma).");
        disableClouds = cfg.getBoolean("disableClouds", CAT_VISUAL, false, "Hide clouds.");
        disableFireOverlay = cfg.getBoolean("disableFireOverlay", CAT_VISUAL, false, "Hide the fire screen overlay.");
        disableWaterOverlay = cfg.getBoolean("disableWaterOverlay", CAT_VISUAL, false, "Hide the water screen overlay.");

        dynamicFps = cfg.getBoolean("dynamicFps", CAT_PERF, false, "Auto-lower render distance when FPS drops.");
        dynamicFpsMin = cfg.getInt("dynamicFpsMin", CAT_PERF, 30, 1, 240, "FPS below this triggers a downgrade.");
        dynamicFpsRestore = cfg.getInt("dynamicFpsRestore", CAT_PERF, 50,1, 240, "FPS above this restores the distance.");
        dynamicFpsMinDist = cfg.getInt("dynamicFpsMinDist", CAT_PERF, 4, 2, 32, "Render distance used during a downgrade.");
        dynamicFpsMaxDist = cfg.getInt("dynamicFpsMaxDist", CAT_PERF, 12, 2, 32, "Render distance to restore to.");
        smartFps = cfg.getBoolean("smartFps", CAT_PERF, false, "Throttle FPS when the window is not focused.");
        fastGraphics = cfg.getBoolean("fastGraphics", CAT_PERF, false, "Use fast graphics (lower geometry cost).");
        noSmoothLight = cfg.getBoolean("noSmoothLight", CAT_PERF, false, "Disable smooth lighting (big lighting saving).");
        lowParticles = cfg.getBoolean("lowParticles", CAT_PERF, false, "Render minimal particles.");

        modSelf = cfg.getBoolean("self_status", CAT_MODULES, true, "Enable Self Status module.");
        modEnvironment = cfg.getBoolean("environment", CAT_MODULES, true, "Enable Environment module.");
        modItem = cfg.getBoolean("item", CAT_MODULES, true, "Enable Item module.");
        modInput = cfg.getBoolean("input", CAT_MODULES, true, "Enable Input module.");
        modOther = cfg.getBoolean("other", CAT_MODULES, true, "Enable Other module.");
        modEntity = cfg.getBoolean("entity", CAT_MODULES, true, "Enable Entity module.");
        modCombat = cfg.getBoolean("combat", CAT_MODULES, true, "Enable Combat module.");

        showHealth = cfg.getBoolean("showHealth", CAT_VANILLA, true, "Show vanilla health bar.");
        showArmor = cfg.getBoolean("showArmor", CAT_VANILLA, true, "Show vanilla armor bar.");
        showFood = cfg.getBoolean("showFood", CAT_VANILLA, true, "Show vanilla hunger bar.");
        showAir = cfg.getBoolean("showAir", CAT_VANILLA, true, "Show vanilla air bar.");
        showHotbar = cfg.getBoolean("showHotbar", CAT_VANILLA, true, "Show vanilla hotbar.");
        showExp = cfg.getBoolean("showExp", CAT_VANILLA, true, "Show vanilla experience / jump bar.");
        showCrosshair = cfg.getBoolean("showCrosshair", CAT_VANILLA, true, "Show vanilla crosshair.");
        showBoss = cfg.getBoolean("showBoss", CAT_VANILLA, true, "Show vanilla boss bar.");
        showPotion = cfg.getBoolean("showPotion", CAT_VANILLA, true, "Show vanilla potion icons.");
        showVignette = cfg.getBoolean("showVignette", CAT_VANILLA, true, "Show vanilla vignette.");
        showPortal = cfg.getBoolean("showPortal", CAT_VANILLA, true, "Show vanilla portal overlay.");
        showHelmet = cfg.getBoolean("showHelmet", CAT_VANILLA, true, "Show vanilla helmet overlay.");
        showJumpbar = cfg.getBoolean("showJumpbar", CAT_VANILLA, true, "Show vanilla jump bar.");

        if (cfg.hasChanged()) cfg.save();
    }

    public static boolean isModuleOn(String id) {
        switch (id) {
            case "self_status": return modSelf;
            case "environment": return modEnvironment;
            case "item": return modItem;
            case "input": return modInput;
            case "other": return modOther;
            case "entity": return modEntity;
            case "combat": return modCombat;
            default: return true;
        }
    }

    /** True when the vanilla HUD element should be displayed. */
    public static boolean isVanillaShown(String key) {
        switch (key) {
            case "showHealth": return showHealth;
            case "showArmor": return showArmor;
            case "showFood": return showFood;
            case "showAir": return showAir;
            case "showHotbar": return showHotbar;
            case "showExp": return showExp;
            case "showCrosshair": return showCrosshair;
            case "showBoss": return showBoss;
            case "showPotion": return showPotion;
            case "showVignette": return showVignette;
            case "showPortal": return showPortal;
            case "showHelmet": return showHelmet;
            case "showJumpbar": return showJumpbar;
            default: return true;
        }
    }

    /** Toggle a performance setting (most live in CAT_PERF; disableClouds lives in CAT_VISUAL). */
    public static void togglePerf(String name) {
        String cat = perfCategory(name);
        boolean v = cfg.getBoolean(name, cat, false, "");
        cfg.get(cat, name, v).set(!v);
        cfg.save();
        sync();
    }

    public static boolean isPerfOn(String name) {
        return cfg.getBoolean(name, perfCategory(name), false, "");
    }

    private static String perfCategory(String name) {
        if ("disableClouds".equals(name)) return CAT_VISUAL;
        return CAT_PERF;
    }

    /** One-click: turn on every performance optimisation at once. */
    public static void applyPerformanceMode() {
        setBool(CAT_PERF, "dynamicFps", true);
        setBool(CAT_PERF, "smartFps", true);
        setBool(CAT_PERF, "fastGraphics", true);
        setBool(CAT_PERF, "noSmoothLight", true);
        setBool(CAT_PERF, "lowParticles", true);
        setBool(CAT_VISUAL, "disableClouds", true);
        cfg.save();
        sync();
    }

    private static void setBool(String cat, String name, boolean v) {
        cfg.get(cat, name, v).set(v);
    }

    public static void loadModulePositions() {
        for (String id : MODULE_IDS) {
            int x = cfg.getInt("posX_" + id, CAT_POS, defaultX(id), Integer.MIN_VALUE, Integer.MAX_VALUE, "");
            int y = cfg.getInt("posY_" + id, CAT_POS, defaultY(id), Integer.MIN_VALUE, Integer.MAX_VALUE, "");
            modulePos.put(id, new int[]{x, y});
        }
    }

    private static int defaultX(String id) {
        return 4;
    }

    private static int defaultY(String id) {
        switch (id) {
            case "self_status": return 4;
            case "environment": return 130;
            case "item": return 300;
            case "input": return 420;
            case "other": return 520;
            case "entity": return 4;
            case "combat": return 170;
            default: return 4;
        }
    }

    public static int getModulePosX(String id) {
        int[] p = modulePos.get(id);
        return p == null ? 4 : p[0];
    }

    public static int getModulePosY(String id) {
        int[] p = modulePos.get(id);
        return p == null ? 4 : p[1];
    }

    public static void setModulePos(String id, int x, int y) {
        modulePos.put(id, new int[]{x, y});
        cfg.get(CAT_POS, "posX_" + id, x).set(x);
        cfg.get(CAT_POS, "posY_" + id, y).set(y);
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

    public static void setLanguage(String l) {
        language = l;
        I18n.setLang(l);
        cfg.get(CAT_GENERAL, "language", l).set(l);
        save();
    }

    public static void save() {
        if (cfg != null && cfg.hasChanged()) cfg.save();
    }

    public static Configuration get() {
        return cfg;
    }
}
