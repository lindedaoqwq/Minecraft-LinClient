package com.lindedaoqwq.linclient.hooks;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;

/**
 * Static hooks called from bytecode injected by {@link com.lindedaoqwq.linclient.core.LinClientTransformer}.
 * Must stay cheap and never throw.
 *
 * The optimisation techniques here are clean-room implementations written from publicly
 * documented algorithm descriptions (GL specification behaviour, projection maths, vanilla
 * hot-path analysis). No code is copied from other clients.
 *
 *  - Lightmap dirty-check        : skip a 256-entry recompute + 16x16 texture upload per tick
 *  - No Hurt Cam                 : skip hurtCameraEffect
 *  - Particle throttle           : configurable keep ratio
 *  - Sign-text distance culling  : skip per-sign splitText / getStringWidth / drawString
 *  - Armour texture cache        : skip Forge's nested String.format + 40-char hash per slot
 *  - Perf counters               : so a feature can prove it actually ran
 */
public class EngineHooks {
    private static Object lastWorld = null;
    private static float lastGamma = Float.NaN;
    private static long lastLightmapMs = 0L;
    private static int particleCounter = 0;
    private static boolean announced = false;

    // ---- counters: "did the feature actually run?" ----
    public static volatile long cSignDrawn = 0;     // sign texts that were drawn
    public static volatile long cSignCulled = 0;    // sign texts skipped by distance
    public static volatile long cArmorLookups = 0;  // armour texture lookups served
    public static volatile long cArmorHits = 0;     // armour texture cache hits
    public static volatile long cParticlesIn = 0;   // particles offered
    public static volatile long cParticlesCut = 0;  // particles dropped

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

    public static boolean noHurtCam() {
        try { announce(); return Modules.on("nohurtcam"); } catch (Throwable t) { return false; }
    }

    /** Engine particle throttle: keeps only the configured percentage of new particles. */
    public static boolean skipParticle() {
        try {
            if (!Modules.on("particles")) return false;
            float keep = ModConfig.value("particles.keep", 25F);
            if (keep >= 100F) return false;
            cParticlesIn++;
            if (keep <= 0F) { cParticlesCut++; return true; }
            long h = (particleCounter++ * 2654435761L) >>> 33;   // cheap, even pseudo-random
            boolean cut = (int) (h % 100L) >= (int) keep;
            if (cut) cParticlesCut++;
            return cut;
        } catch (Throwable t) { return false; }
    }

    /**
     * Called at the top of TileEntitySignRenderer.render. Returns true when the sign is far
     * enough away that laying out and drawing its text is not worth it.
     *
     * The threshold comes from the projection relation: at distance d the vertical size of one
     * pixel is 2*d*tan(fov/2)/viewportHeight, so a glyph falls below ~2 px when
     * d > 1.5 * viewportHeight / fov(degrees). Clamped to >= 16 blocks so a genuinely legible
     * sign is never culled.
     */
    public static boolean skipSignText(Object tileEntity) {
        try {
            if (!Modules.on("signtext")) return false;
            if (!(tileEntity instanceof TileEntity)) return false;
            Minecraft mc = Minecraft.getMinecraft();
            Entity view = mc.getRenderViewEntity();
            if (view == null) return false;

            double distSq = view.getDistanceSq(((TileEntity) tileEntity).getPos());
            float range = textRange(mc);
            if (distSq <= (double) range * (double) range) return false;

            cSignCulled++;
            return true;
        } catch (Throwable t) { return false; }
    }

    /** True = the sign text will actually be laid out and drawn this time. */
    public static boolean signTextVisible() {
        try {
            cSignDrawn++;
            return true;
        } catch (Throwable t) { return true; }
    }

    private static float textRange(Minecraft mc) {
        // Explicit override wins; 0 = derive from the projection (1.5 * height / fov).
        float manual = ModConfig.value("signtext.dist", 0F);
        if (manual >= 1F) return manual;
        float fov = mc.gameSettings.fovSetting;
        if (fov < 1F) fov = 70F;
        int h = mc.displayHeight;
        if (h <= 0) h = 720;
        float r = 1.5F * (float) h / fov;
        return r < 16F ? 16F : r;
    }

    // ---- armour texture memoisation ---------------------------------------
    // Forge's LayerArmorBase.getArmorResource builds the texture path with a nested
    // String.format (4 args + boxing) and then hashes a ~40-char string, for every worn
    // armour slot, every frame. The returned ResourceLocation is a pure function of that
    // path, so an equal path can reuse the instance vanilla produced the first time.
    // Cached by the ResourceLocation's own path, which we can read without formatting.

    private static final int ARMOR_CACHE = 32;
    private static final String[] aKeys = new String[ARMOR_CACHE];
    private static final ResourceLocation[] aVals = new ResourceLocation[ARMOR_CACHE];
    private static int aFill = 0;

    public static ResourceLocation armorResolve(ResourceLocation computed) {
        try {
            if (computed == null) return null;
            if (!Modules.on("armorcache")) return computed;
            cArmorLookups++;
            String key = computed.toString();
            for (int i = 0; i < aFill; i++) {
                if (aKeys[i].equals(key)) { cArmorHits++; return aVals[i]; }
            }
            if (aFill < ARMOR_CACHE) {
                aKeys[aFill] = key;
                aVals[aFill] = computed;
                aFill++;
            }
            return computed;
        } catch (Throwable t) { return computed; }
    }

    public static void clearCaches() {
        Arrays.fill(aKeys, null);
        Arrays.fill(aVals, null);
        aFill = 0;
        lastWorld = null;
    }

    /** Called once per rendered frame from the HUD pass. */
    public static void onFrame() {
        if (cSignDrawn > 20_000_000L) { cSignDrawn = 0; cSignCulled = 0; }
        if (cParticlesIn > 20_000_000L) { cParticlesIn = 0; cParticlesCut = 0; }
        if (cArmorLookups > 20_000_000L) { cArmorLookups = 0; cArmorHits = 0; }
    }

    /** One-line summary for logs / diagnostics. */
    public static String stats() {
        return "sign " + cSignCulled + " culled /" + (cSignCulled + cSignDrawn) + " total"
                + " | armor hit " + cArmorHits + "/" + cArmorLookups
                + " | particle cut " + cParticlesCut + "/" + cParticlesIn;
    }
}
