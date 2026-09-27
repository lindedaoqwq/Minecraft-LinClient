package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Coordinates / facing / biome / light / in-game & real time / FPS / ping / memory / entity & chunk counts. */
public class EnvironmentModule extends HudModule {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    public EnvironmentModule() {
        super("environment", "linclient.module.environment");
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        if (mc.level == null || mc.player == null) return lines;

        BlockPos pos = mc.player.blockPosition();
        lines.add(I18n.t("linclient.env.coords", pos.getX(), pos.getY(), pos.getZ()));

        float yaw = (mc.player.getYRot() % 360f + 360f) % 360f;
        String[] dirs = {"S", "W", "N", "E"};
        String compass = dirs[(int) Math.round(yaw / 90.0) % 4];
        lines.add(I18n.t("linclient.env.facing", compass, (int) yaw));

        Holder<Biome> biomeHolder = mc.level.getBiome(pos);
        ResourceLocation rl = biomeHolder.unwrapKey().map(ResourceKey::location).orElse(null);
        lines.add(I18n.t("linclient.env.biome", rl == null ? "unknown" : rl.getPath()));

        int light = mc.level.getLightEngine().getRawBrightness(pos, 0);
        lines.add(I18n.t("linclient.env.light", light));

        long dayTime = mc.level.getDayTime();
        long day = dayTime / 24000L;
        long t = (dayTime + 6000L) % 24000L;
        int hour = (int) (t / 1000L);
        int minute = (int) ((t % 1000L) * 60L / 1000L);
        lines.add(I18n.t("linclient.env.gametime", day, String.format("%02d:%02d", hour, minute)));

        lines.add(I18n.t("linclient.env.realtime", LocalTime.now().format(CLOCK)));

        lines.add(I18n.t("linclient.env.fps", mc.getFps()));

        int ping = 0;
        ServerData sd = mc.getCurrentServer();
        if (sd != null) ping = (int) sd.ping;
        lines.add(I18n.t("linclient.env.ping", ping));

        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long totalMB = rt.maxMemory() / (1024L * 1024L);
        lines.add(I18n.t("linclient.env.memory", usedMB, totalMB));

        int entities = 0;
        for (Entity ignored : mc.level.entitiesForRendering()) {
            entities++;
        }
        lines.add(I18n.t("linclient.env.entities", entities));

        int chunks = ((ClientChunkCache) mc.level.getChunkSource()).getLoadedChunksCount();
        lines.add(I18n.t("linclient.env.chunks", chunks));

        return lines;
    }
}
