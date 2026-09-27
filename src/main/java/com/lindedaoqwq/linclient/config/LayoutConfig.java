package com.lindedaoqwq.linclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lindedaoqwq.linclient.LinClient;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime, per-module layout persisted as JSON so the HUD editor can mutate it live
 * without reloading the Forge config.
 *
 * Location: {@code <game>/config/linclient/layout.json}
 */
public class LayoutConfig {
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("linclient").resolve("layout.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LayoutConfig instance;

    /** moduleId -> layout */
    public Map<String, ModuleLayout> modules = new HashMap<>();

    public static class ModuleLayout {
        public boolean enabled = true;
        public int x = 4;
        public int y = 4;
        public float scale = 1.0f;
        public float opacity = 1.0f;
        public int color = 0xFFFFFF;       // 0xRRGGBB
        public int order = 0;
        public boolean alignRight = false; // anchor to the right edge instead of the left
        public boolean background = true;
    }

    public static LayoutConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static LayoutConfig load() {
        LayoutConfig cfg = new LayoutConfig();
        ensureDefaults(cfg);
        try {
            if (Files.exists(PATH)) {
                String json = new String(Files.readAllBytes(PATH), StandardCharsets.UTF_8);
                LayoutConfig loaded = GSON.fromJson(json, LayoutConfig.class);
                if (loaded != null && loaded.modules != null) {
                    for (Map.Entry<String, ModuleLayout> e : loaded.modules.entrySet()) {
                        cfg.modules.put(e.getKey(), e.getValue());
                    }
                }
            }
        } catch (Exception e) {
            LinClient.LOGGER.error("Failed to load LinClient layout, using defaults.", e);
        }
        return cfg;
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.write(PATH, GSON.toJson(get()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LinClient.LOGGER.error("Failed to save LinClient layout.", e);
        }
    }

    /** Reset a single module to its default position/style. */
    public static void reset(String id) {
        ModuleLayout def = defaultLayout(id);
        if (def != null) get().modules.put(id, def);
    }

    public static ModuleLayout layoutOf(String id) {
        ModuleLayout ml = get().modules.get(id);
        if (ml == null) {
            ml = defaultLayout(id);
            if (ml == null) ml = new ModuleLayout();
            get().modules.put(id, ml);
        }
        return ml;
    }

    private static void ensureDefaults(LayoutConfig cfg) {
        for (String id : new String[]{
                "self_status", "environment", "item", "input", "other",
                "entity", "combat", "boss"}) {
            cfg.modules.putIfAbsent(id, defaultLayout(id));
        }
    }

    private static ModuleLayout defaultLayout(String id) {
        ModuleLayout m = new ModuleLayout();
        switch (id) {
            case "self_status":  m.x = 4;   m.y = 4;   m.order = 0; m.color = 0x55FF55; break;
            case "environment":  m.x = 4;   m.y = 140; m.order = 1; m.color = 0x55FFFF; break;
            case "item":         m.x = 4;   m.y = 320; m.order = 2; m.color = 0xFFAA00; break;
            case "input":        m.x = 4;   m.y = 440; m.order = 3; m.color = 0xFFFF55; break;
            case "other":        m.x = 4;   m.y = 540; m.order = 4; m.color = 0xAAAAFF; break;
            case "entity":       m.x = 4;   m.y = 4;   m.order = 5; m.alignRight = true; m.color = 0xFF5555; break;
            case "combat":       m.x = 4;   m.y = 170; m.order = 6; m.alignRight = true; m.color = 0xFF55FF; break;
            case "boss":         m.x = 4;   m.y = 320; m.order = 7; m.alignRight = true; m.color = 0xFFAA55; break;
            default: return null;
        }
        return m;
    }
}
