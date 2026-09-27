package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.lwjgl.opengl.Display;

import java.util.List;

public class EnvironmentModule extends HudModule {
    public EnvironmentModule() {
        super("environment", "linclient.module.environment", false, 0x55FFFF);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        World w = mc.theWorld;
        if (w == null || mc.thePlayer == null) return l;

        l.add(I18n.t("linclient.env.fps", Display.getFPS()));

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

        int chunks = w.getChunkProvider().getLoadedChunkCount();
        l.add(I18n.t("linclient.env.chunks", chunks));

        BlockPos bp = mc.thePlayer.getPosition();
        BiomeGenBase biome = w.getBiomeGenForCoords(bp.getX(), bp.getZ());
        if (biome != null) l.add(I18n.t("linclient.env.biome", biome.biomeName));

        l.add(I18n.t("linclient.env.time", formatTime(w.getWorldTime())));
        l.add(I18n.t("linclient.env.light", w.getLight(bp)));
        return l;
    }

    private String formatTime(long t) {
        long day = t / 24000L + 1L;
        long timeOfDay = t % 24000L;
        long h = (timeOfDay / 1000L + 6L) % 24L;
        long m = (timeOfDay % 1000L) * 60L / 1000L;
        return day + " " + String.format("%02d:%02d", h, m);
    }
}
