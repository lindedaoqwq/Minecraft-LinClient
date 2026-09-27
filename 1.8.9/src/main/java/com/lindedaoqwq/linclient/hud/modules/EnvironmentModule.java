package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import com.lindedaoqwq.linclient.state.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.List;

public class EnvironmentModule extends HudModule {
    public EnvironmentModule() {
        super("environment", "linclient.module.environment", 0x55FFFF);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        World w = mc.theWorld;
        if (w == null || mc.thePlayer == null) return l;

        l.add(I18n.t("linclient.env.fps", ClientState.fps));

        String ping = "?";
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) ping = String.valueOf(info.getResponseTime());
        }
        l.add(I18n.t("linclient.env.ping", ping));

        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long max = rt.maxMemory() / (1024L * 1024L);
        l.add(I18n.t("linclient.env.memory", used, max));

        l.add(I18n.t("linclient.env.entities", w.loadedEntityList.size()));

        int chunks = getLoadedChunkCount(w);
        l.add(I18n.t("linclient.env.chunks", chunks));

        BlockPos bp = mc.thePlayer.getPosition();
        BiomeGenBase biome = w.getBiomeGenForCoords(bp);
        if (biome != null) l.add(I18n.t("linclient.env.biome", biome.biomeName));

        l.add(I18n.t("linclient.env.time", formatTime(w.getWorldTime())));
        l.add(I18n.t("linclient.env.light", w.getLight(bp)));
        return l;
    }

    // Count loaded client chunks without depending on a version-specific API:
    // 1.8.9 exposes ChunkProviderClient.getLoadedChunkCount(); 1.12.2 stores them in a
    // private 'chunkMapping' field, so fall back to reflection there.
    private int getLoadedChunkCount(World w) {
        Object cp = w.getChunkProvider();
        if (!(cp instanceof ChunkProviderClient)) return 0;
        try {
            return ((Integer) cp.getClass().getMethod("getLoadedChunkCount").invoke(cp)).intValue();
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Field f = cp.getClass().getDeclaredField("chunkMapping");
                f.setAccessible(true);
                Object map = f.get(cp);
                if (map instanceof java.util.Map) return ((java.util.Map<?, ?>) map).size();
                return ((Integer) map.getClass().getMethod("size").invoke(map)).intValue();
            } catch (Exception e2) {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatTime(long t) {
        long day = t / 24000L + 1L;
        long timeOfDay = t % 24000L;
        long h = (timeOfDay / 1000L + 6L) % 24L;
        long m = (timeOfDay % 1000L) * 60L / 1000L;
        return day + " " + String.format("%02d:%02d", h, m);
    }
}
