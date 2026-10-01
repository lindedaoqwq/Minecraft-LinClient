package com.lindedaoqwq.linclient.hooks;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * Static hooks called from bytecode injected by {@link com.lindedaoqwq.linclient.core.LinClientTransformer}.
 * Must stay cheap and never throw.
 *
 * IMPORTANT: the bytecode transformer injects INVOKESTATIC calls against these exact method
 * names and descriptors, so the signatures must never change. The transformer's injection
 * points are kept as infrastructure; each hook below decides at runtime whether it does
 * anything. Hooks whose feature was retired return "do not intercept" so the injected guard
 * is a harmless no-op.
 *
 * Active:
 *  - Lightmap dirty-check : skip the 256-entry recompute + texture upload when the world and
 *                           gamma are unchanged (at most every 250 ms).
 * Retired (kept as safe pass-through so old injection points stay valid):
 *  - noHurtCam / skipParticle / skipSignText / armorResolve
 */
public class EngineHooks {
    private static Object lastWorld = null;
    private static float lastGamma = Float.NaN;
    private static long lastLightmapMs = 0L;
    private static boolean announced = false;

    private static void announce() {
        if (!announced) {
            announced = true;
            System.out.println("[LinClient] engine hooks live (called from patched vanilla code)");
        }
    }

    /** True = skip the lightmap recompute/texture-upload this tick. Recomputes on world/gamma
     *  change or at most every 250 ms so brightness adaptation stays imperceptible. */
    public static boolean skipLightmap() {
        try {
            announce();
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) return false;
            long now = System.currentTimeMillis();
            if (mc.theWorld != lastWorld || mc.gameSettings.gammaSetting != lastGamma) {
                lastWorld = mc.theWorld;
                lastGamma = mc.gameSettings.gammaSetting;
                lastLightmapMs = now;
                return false;
            }
            if (now - lastLightmapMs >= 250L) { lastLightmapMs = now; return false; }
            return true;
        } catch (Throwable t) { return false; }
    }

    /** Retired feature: never intercept. Signature kept for the injected guard. */
    public static boolean noHurtCam() {
        return false;
    }

    /** Retired feature: never drop a particle. Signature kept for the injected guard. */
    public static boolean skipParticle() {
        return false;
    }

    /** Retired feature: never cull sign text. Signature kept for the injected guard. */
    public static boolean skipSignText(Object tileEntity) {
        return false;
    }

    /** Retired feature: pass the computed ResourceLocation straight through. */
    public static ResourceLocation armorResolve(ResourceLocation computed) {
        return computed;
    }

    public static boolean signTextVisible() {
        return true;
    }

    public static void clearCaches() {
        lastWorld = null;
    }

    /** Called once per rendered frame from the HUD pass. */
    public static void onFrame() {
    }

    /** One-line summary for logs / diagnostics. */
    public static String stats() {
        return "engine hooks ok";
    }
}
