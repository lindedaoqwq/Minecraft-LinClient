package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/** Speedometer / travelled distance / AFK timer / death coordinates / server IP. */
public class OtherModule extends HudModule {
    public OtherModule() {
        super("other", "linclient.module.other");
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();

        lines.add(I18n.t("linclient.other.speed", Format.fmt(ClientState.speed), Format.fmt(ClientState.speed * 3.6)));
        lines.add(I18n.t("linclient.other.distance", Format.fmt(ClientState.distance)));

        long afkTicks = (System.currentTimeMillis() - ClientState.lastInputTime) / 50L;
        lines.add(I18n.t("linclient.other.afk", Format.durationTicks((int) afkTicks)));

        for (ClientState.DeathRecord d : ClientState.deaths) {
            lines.add(I18n.t("linclient.other.death", d.dimension, d.x, d.y, d.z));
        }

        String server = mc.getCurrentServer() != null
                ? mc.getCurrentServer().ip
                : I18n.t("linclient.other.singleplayer");
        lines.add(I18n.t("linclient.other.server", server));

        return lines;
    }
}
