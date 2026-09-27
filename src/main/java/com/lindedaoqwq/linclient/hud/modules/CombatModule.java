package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Combat information only (no automatic actions):
 *  - current crosshair target (health / armor / distance)
 *  - last damage dealt / taken (numbers)
 *  - combo counter
 */
public class CombatModule extends HudModule {
    public CombatModule() {
        super("combat", "linclient.module.combat");
    }

    /** Pick the living entity closest to the crosshair within melee reach. */
    public static LivingEntity findTarget(Minecraft mc) {
        Player p = mc.player;
        if (p == null || mc.level == null) return null;

        Vec3 look = p.getViewVector(1.0f);
        Vec3 eye = p.getEyePosition(1.0f);
        double reach = 4.5;
        LivingEntity best = null;
        double bestDot = 0.97;

        for (Entity e : mc.level.getEntities().getAll()) {
            if (!(e instanceof LivingEntity le) || e == p) continue;
            if (p.distanceTo(e) > reach) continue;
            AABB box = e.getBoundingBox();
            Vec3 to = box.getCenter().subtract(eye).normalize();
            double dot = look.dot(to);
            if (dot > bestDot) {
                bestDot = dot;
                best = le;
            }
        }
        return best;
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        if (mc.player == null) return lines;

        LivingEntity target = findTarget(mc);
        if (target != null) {
            lines.add(I18n.t("linclient.combat.target",
                    target.getName().getString(), Format.fmt(target.getHealth()), Format.fmt(target.getMaxHealth()),
                    target.getArmorValue(), (int) mc.player.distanceTo(target)));
        }

        long now = System.currentTimeMillis();
        if (now - ClientState.lastDamageDealtTime < 3000L) {
            lines.add(I18n.t("linclient.combat.dealt", Format.fmt(ClientState.lastDamageDealt)));
        }
        if (now - ClientState.lastDamageTakenTime < 3000L) {
            lines.add(I18n.t("linclient.combat.taken", Format.fmt(ClientState.lastDamageTaken)));
        }
        if (ClientState.combo > 1) {
            lines.add(I18n.t("linclient.combat.combo", ClientState.combo));
        }
        return lines;
    }
}
