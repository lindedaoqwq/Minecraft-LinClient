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

    /** True = skip the lightmap recompute/texture-upload this tick. Recomputes on world/gamma
     *  change or at most every 250 ms so brightness adaptation stays imperceptible. */
    public static boolean skipLightmap() {
        try {
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
        try { return Modules.on("nohurtcam"); } catch (Throwable t) { return false; }
    }

    /** Engine particle throttle: drops ~75% of new particles when the module is on. */
    public static boolean skipParticle() {
        try {
            if (!Modules.on("particles")) return false;
            return (particleCounter++ & 3) != 0;
        } catch (Throwable t) { return false; }
    }
}
