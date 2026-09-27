package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import java.util.List;

public class SelfStatusModule extends HudModule {
    public SelfStatusModule() {
        super("self_status", "linclient.module.self_status", 0x55FF55);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        EntityPlayerSP p = mc.player;
        if (p == null) return l;
        float hp = p.getHealth() + p.getAbsorptionAmount();
        l.add(I18n.t("linclient.self.hp", Format.f(hp)));
        l.add(I18n.t("linclient.self.hunger", p.getFoodStats().getFoodLevel()));
        l.add(I18n.t("linclient.self.saturation", Format.f(p.getFoodStats().getSaturationLevel())));
        l.add(I18n.t("linclient.self.air", p.getAir()));
        l.add(I18n.t("linclient.self.pos", (int) p.posX, (int) p.posY, (int) p.posZ));
        l.add(I18n.t("linclient.self.speed", Format.f(ClientState.speed)));
        l.add(I18n.t("linclient.self.distance", Format.f(ClientState.distance)));
        l.add(I18n.t("linclient.self.gamemode", gamemode(p)));
        return l;
    }

    private String gamemode(EntityPlayerSP p) {
        if (p.capabilities.isCreativeMode) return I18n.t("linclient.gm.creative");
        if (p.isSpectator()) return I18n.t("linclient.gm.spectator");
        if (p.capabilities.allowEdit) return I18n.t("linclient.gm.survival");
        return I18n.t("linclient.gm.adventure");
    }
}
