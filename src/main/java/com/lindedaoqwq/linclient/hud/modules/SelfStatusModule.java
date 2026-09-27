package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import java.util.List;

/** Health / absorption / armor / food / air / XP / active potion effects. */
public class SelfStatusModule extends HudModule {
    public SelfStatusModule() {
        super("self_status", "linclient.module.self");
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        Player p = mc.player;
        if (p == null) return lines;

        lines.add(I18n.t("linclient.self.hp", Format.fmt(p.getHealth()), Format.fmt(p.getMaxHealth())));
        if (p.getAbsorptionAmount() > 0.0f) {
            lines.add(I18n.t("linclient.self.absorption", Format.fmt(p.getAbsorptionAmount())));
        }
        lines.add(I18n.t("linclient.self.armor", p.getArmorValue()));

        FoodData fd = p.getFoodData();
        lines.add(I18n.t("linclient.self.food", fd.getFoodLevel(), Format.fmt(fd.getSaturationLevel())));

        if (p.getAirSupply() < p.getMaxAirSupply()) {
            lines.add(I18n.t("linclient.self.air", p.getAirSupply()));
        }

        lines.add(I18n.t("linclient.self.xp", p.experienceLevel, (int) (p.experienceProgress * 100.0f), p.totalExperience));

        for (MobEffectInstance e : p.getActiveEffects()) {
            lines.add(I18n.t("linclient.self.effect", e.getEffect().getDisplayName().getString(), Format.durationTicks(e.getDuration())));
        }
        return lines;
    }
}
