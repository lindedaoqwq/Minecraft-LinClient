package com.lindedaoqwq.linclient.config;

import com.lindedaoqwq.linclient.LinClient;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Global, build-time configuration backed by Forge's native config system.
 *
 * Per-module layout (position / scale / opacity / colour / order / visibility) lives in a
 * separate runtime JSON file (see {@link LayoutConfig}) so the HUD editor can mutate it live.
 *
 * All values are read lazily at runtime, so editing {@code config/linclient-client.toml}
 * (or using the "Configured" mod for an in-game GUI) takes effect immediately.
 */
public class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---- General ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOD;
    public static final ForgeConfigSpec.BooleanValue SHOW_HUD_IN_DEBUG;
    public static final ForgeConfigSpec.BooleanValue BACKGROUND_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> BACKGROUND_COLOR;
    public static final ForgeConfigSpec.DoubleValue BACKGROUND_OPACITY;

    // ---- Visual (client only) ----
    public static final ForgeConfigSpec.BooleanValue FULLBRIGHT;
    public static final ForgeConfigSpec.BooleanValue DISABLE_WEATHER;
    public static final ForgeConfigSpec.BooleanValue DISABLE_CLOUDS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_FIRE_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue DISABLE_WATER_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue DISABLE_PORTAL_OVERLAY;

    // ---- Performance (client only) ----
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_FPS;
    public static final ForgeConfigSpec.IntValue DYNAMIC_FPS_MIN;
    public static final ForgeConfigSpec.IntValue DYNAMIC_FPS_RESTORE;
    public static final ForgeConfigSpec.IntValue DYNAMIC_FPS_MIN_DIST;
    public static final ForgeConfigSpec.IntValue DYNAMIC_FPS_MAX_DIST;
    // The following toggles require a companion coremod/mixin to physically alter the renderer.
    // They are exposed here as reserved configuration switches with documented integration points.
    public static final ForgeConfigSpec.BooleanValue ENTITY_CULLING;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_CULLING;
    public static final ForgeConfigSpec.IntValue PARTICLE_LIMIT;
    public static final ForgeConfigSpec.BooleanValue LOD_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue CHUNK_UPDATE_THROTTLE;
    public static final ForgeConfigSpec.BooleanValue TEXTURE_ANIM_LIMIT;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.comment("General settings").push("general");
        ENABLE_MOD = BUILDER.comment("Master switch for LinClient.").define("enableMod", true);
        SHOW_HUD_IN_DEBUG = BUILDER.comment("Keep HUD visible while the debug screen (F3) is open.")
                .define("showHudInDebug", false);
        BACKGROUND_ENABLED = BUILDER.comment("Draw a semi-transparent background behind each module.")
                .define("backgroundEnabled", true);
        BACKGROUND_COLOR = BUILDER.comment("Background colour as RRGGBB hex (no #).").define("backgroundColor", "000000");
        BACKGROUND_OPACITY = BUILDER.comment("Background opacity, 0.0 - 1.0.").defineInRange("backgroundOpacity", 0.35, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Client-side visual overlays (never touch the server).").push("visual");
        FULLBRIGHT = BUILDER.comment("Force maximum brightness (gamma).").define("fullbright", false);
        DISABLE_WEATHER = BUILDER.comment("Hide rain / snow on the client.").define("disableWeather", false);
        DISABLE_CLOUDS = BUILDER.comment("Hide clouds.").define("disableClouds", false);
        DISABLE_FIRE_OVERLAY = BUILDER.comment("Hide the fire screen overlay.").define("disableFireOverlay", false);
        DISABLE_WATER_OVERLAY = BUILDER.comment("Hide the water screen overlay.").define("disableWaterOverlay", false);
        DISABLE_PORTAL_OVERLAY = BUILDER.comment("Hide the portal overlay (needs a coremod; reserved switch).")
                .define("disablePortalOverlay", false);
        BUILDER.pop();

        BUILDER.comment("Client-side performance helpers.").push("performance");
        DYNAMIC_FPS = BUILDER.comment("Auto-lower render distance when FPS drops, restore when stable.")
                .define("dynamicFps", false);
        DYNAMIC_FPS_MIN = BUILDER.comment("FPS below this triggers a downgrade.").defineInRange("dynamicFpsMin", 30, 1, 240);
        DYNAMIC_FPS_RESTORE = BUILDER.comment("FPS above this restores the distance.").defineInRange("dynamicFpsRestore", 50, 1, 240);
        DYNAMIC_FPS_MIN_DIST = BUILDER.comment("Render distance used during a downgrade.").defineInRange("dynamicFpsMinDist", 4, 2, 32);
        DYNAMIC_FPS_MAX_DIST = BUILDER.comment("Render distance to restore to.").defineInRange("dynamicFpsMaxDist", 12, 2, 32);
        ENTITY_CULLING = BUILDER.comment("Entity culling (reserved - needs a companion coremod).").define("entityCulling", false);
        BLOCK_ENTITY_CULLING = BUILDER.comment("Block-entity culling (reserved - needs a companion coremod).").define("blockEntityCulling", false);
        PARTICLE_LIMIT = BUILDER.comment("Max particles (0 = unlimited; reserved - needs a companion coremod).").defineInRange("particleLimit", 0, 0, 4000);
        LOD_DISTANCE = BUILDER.comment("LOD render-distance adjustment (reserved).").define("lodDistance", false);
        CHUNK_UPDATE_THROTTLE = BUILDER.comment("Chunk-update throttling (reserved).").define("chunkUpdateThrottle", false);
        TEXTURE_ANIM_LIMIT = BUILDER.comment("Limit animated-texture updates (reserved).").define("textureAnimLimit", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
        LinClient.LOGGER.info("LinClient config spec built.");
    }
}
