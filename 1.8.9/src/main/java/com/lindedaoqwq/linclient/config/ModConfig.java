package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.LinClient;
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

    // modules
    public static boolean modSelf, modEnvironment, modItem, modInput, modOther, modEntity, modCombat;

    // vanilla HUD element visibility
    public static boolean hideHealth, hideArmor, hideFood, hideAir, hideHotbar, hideExp,
            hideCrosshair, hideBoss, hidePotion, hideVignette, hidePortal, hideHelmet, hideJumpbar;

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

        hideHealth = cfg.getBoolean("hideHealth", CAT_VANILLA, false, "Hide vanilla health bar.");
        hideArmor = cfg.getBoolean("hideArmor", CAT_VANILLA, false, "Hide vanilla armor bar.");
        hideFood = cfg.getBoolean("hideFood", CAT_VANILLA, false, "Hide vanilla hunger bar.");
        hideAir = cfg.getBoolean("hideAir", CAT_VANILLA, false, "Hide vanilla air bar.");
        hideHotbar = cfg.getBoolean("hideHotbar", CAT_VANILLA, false, "Hide vanilla hotbar.");
        hideExp = cfg.getBoolean("hideExp", CAT_VANILLA, false, "Hide vanilla experience / jump bar.");
        hideCrosshair = cfg.getBoolean("hideCrosshair", CAT_VANILLA, false, "Hide vanilla crosshair.");
        hideBoss = cfg.getBoolean("hideBoss", CAT_VANILLA, false, "Hide vanilla boss bar.");
        hidePotion = cfg.getBoolean("hidePotion", CAT_VANILLA, false, "Hide vanilla potion icons.");
        hideVignette = cfg.getBoolean("hideVignette", CAT_VANILLA, false, "Hide vanilla vignette.");
        hidePortal = cfg.getBoolean("hidePortal", CAT_VANILLA, false, "Hide vanilla portal overlay.");
        hideHelmet = cfg.getBoolean("hideHelmet", CAT_VANILLA, false, "Hide vanilla helmet overlay.");
        hideJumpbar = cfg.getBoolean("hideJumpbar", CAT_VANILLA, false, "Hide vanilla jump bar.");

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

    public static boolean isVanillaHidden(String key) {
        switch (key) {
            case "hideHealth": return hideHealth;
            case "hideArmor": return hideArmor;
            case "hideFood": return hideFood;
            case "hideAir": return hideAir;
            case "hideHotbar": return hideHotbar;
            case "hideExp": return hideExp;
            case "hideCrosshair": return hideCrosshair;
            case "hideBoss": return hideBoss;
            case "hidePotion": return hidePotion;
            case "hideVignette": return hideVignette;
            case "hidePortal": return hidePortal;
            case "hideHelmet": return hideHelmet;
            case "hideJumpbar": return hideJumpbar;
            default: return false;
        }
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
