package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class CombatModule extends HudModule {
    public CombatModule() {
        super("combat", "linclient.module.combat", true, 0xFF55FF);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        EntityPlayerSP p = mc.player;
        if (p == null) return l;

        EntityLivingBase target = findTarget(mc);
        if (target != null) {
            l.add(I18n.t("linclient.combat.target", target.getName()));
            l.add(I18n.t("linclient.combat.hp", Format.f(target.getHealth())));
            l.add(I18n.t("linclient.combat.dist", Format.f(p.getDistance(target))));
        } else {
            l.add(I18n.t("linclient.combat.target", I18n.t("linclient.none")));
        }
        l.add(I18n.t("linclient.combat.combo", ClientState.combo));
        l.add(I18n.t("linclient.combat.dealt", Format.f(ClientState.lastDamageDealt)));
        l.add(I18n.t("linclient.combat.taken", Format.f(ClientState.lastDamageTaken)));
        return l;
    }

    public static EntityLivingBase findTarget(Minecraft mc) {
        World w = mc.world;
        EntityPlayerSP p = mc.player;
        if (w == null || p == null) return null;
        double range = 8.0;
        double best = range;
        EntityLivingBase bestE = null;
        AxisAlignedBB box = p.getEntityBoundingBox().grow(range);
        for (Entity e : w.getEntitiesWithinAABBExcludingEntity(p, box)) {
            if (!(e instanceof EntityLivingBase)) continue;
            if (e instanceof EntityPlayerSP) continue;
            if (e instanceof EntityMob || e instanceof EntityAnimal
                    || e instanceof EntityDragon || e instanceof EntityGhast) {
                double d = p.getDistance(e);
                if (d < best) {
                    best = d;
                    bestE = (EntityLivingBase) e;
                }
            }
        }
        return bestE;
    }
}
