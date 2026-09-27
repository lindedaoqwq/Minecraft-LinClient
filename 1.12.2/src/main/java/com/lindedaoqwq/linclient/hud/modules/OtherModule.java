package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class OtherModule extends HudModule {
    public OtherModule() {
        super("other", "linclient.module.other", false, 0xAAAAFF);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        World w = mc.world;
        EntityPlayerSP p = mc.player;
        if (w == null || p == null) return l;

        l.add(I18n.t("linclient.other.dim", w.provider.getDimensionType().getName()));
        EnumFacing f = p.getHorizontalFacing();
        l.add(I18n.t("linclient.other.facing", cap(f.getName())));
        l.add(I18n.t("linclient.other.day", (w.getWorldTime() / 24000L) + 1L));
        l.add(I18n.t("linclient.other.xp", p.experienceLevel, p.experienceTotal));
        l.add(I18n.t("linclient.other.difficulty", w.getDifficulty().getName()));
        l.add(I18n.t("linclient.other.light", w.getLight(p.getPosition())));
        return l;
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
