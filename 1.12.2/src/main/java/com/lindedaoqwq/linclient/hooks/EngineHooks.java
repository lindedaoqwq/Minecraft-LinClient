package com.lindedaoqwq.linclient.hooks;

import com.lindedaoqwq.linclient.core.Modules;
import net.minecraft.client.Minecraft;

/**
 * Static hooks called from bytecode injected by {@link com.lindedaoqwq.linclient.core.LinClientTransformer}.
 * Must stay cheap and never throw.
 */
public class EngineHooks {
    private static Object lastWorld = null;
    private static float lastGamma = Float.NaN;
    private static long lastLightmapMs = 0L;
    private static int particleCounter = 0;
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
            if (mc.world == null) return false;
            long now = System.currentTimeMillis();
            if (mc.world != lastWorld || mc.gameSettings.gammaSetting != lastGamma) {
                lastWorld = mc.world;
                lastGamma = mc.gameSettings.gammaSetting;
                lastLightmapMs = now;
                return false;
            }
            if (now - lastLightmapMs >= 250L) { lastLightmapMs = now; return false; }
            return true;
        } catch (Throwable t) { return false; }
    }

    public static boolean noHurtCam() {
        try { announce(); return Modules.on("nohurtcam"); } catch (Throwable t) { return false; }
    }

    /** Engine particle throttle: keeps only the configured percentage of new particles. */
    public static boolean skipParticle() {
        try {
            if (!Modules.on("particles")) return false;
            float keep = com.lindedaoqwq.linclient.config.ModConfig.value("particles.keep", 25F);
            if (keep >= 100F) return false;
            if (keep <= 0F) return true;
            long h = (particleCounter++ * 2654435761L) >>> 33;   // cheap, even pseudo-random
            return (int) (h % 100L) >= (int) keep;
        } catch (Throwable t) { return false; }
    }
}
