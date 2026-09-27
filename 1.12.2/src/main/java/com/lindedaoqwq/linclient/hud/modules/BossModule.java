package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;

import java.util.List;

/**
 * Boss module. Detects nearby boss-type entities in the loaded world (Ender Dragon, Wither, etc.)
 * by checking their class. This is purely client-side observation of entities already present in
 * the world - no hidden server data is read.
 */
public class BossModule extends HudModule {
    public BossModule() {
        super("boss", "linclient.module.boss", true, 0xFFAA55);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        if (mc.world == null) return l;
        boolean any = false;
        for (Entity e : mc.world.loadedEntityList) {
            if ((e instanceof EntityDragon || e instanceof EntityWither) && e instanceof EntityLivingBase) {
                l.add(((EntityLivingBase) e).getDisplayName().getUnformattedText());
                any = true;
            }
        }
        if (!any) l.add(I18n.t("linclient.boss.none"));
        return l;
    }
}
