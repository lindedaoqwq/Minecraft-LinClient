package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Information about nearby players (health / armor / held item / potions) and the nearest
 * living entities (health). This only reads publicly-synced entity data - no hidden server info.
 */
public class EntityModule extends HudModule {
    public EntityModule() {
        super("entity", "linclient.module.entity");
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        if (mc.level == null || mc.player == null) return lines;

        List<Player> players = new ArrayList<>();
        List<LivingEntity> mobs = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof Player p) {
                if (p != mc.player) players.add(p);
            } else if (e instanceof LivingEntity le) {
                mobs.add(le);
            }
        }

        players.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)));
        mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(mc.player)));

        int shown = 0;
        for (Player p : players) {
            if (shown >= 5) break;
            String held = p.getMainHandItem().isEmpty() ? "-" : p.getMainHandItem().getHoverName().getString();
            lines.add(I18n.t("linclient.entity.player",
                    p.getName().getString(), Format.fmt(p.getHealth()), Format.fmt(p.getMaxHealth()),
                    p.getArmorValue(), held));
            if (!p.getActiveEffects().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (MobEffectInstance e : p.getActiveEffects()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(e.getEffect().getDisplayName().getString());
                }
                lines.add(I18n.t("linclient.entity.effects", sb));
            }
            shown++;
        }

        int shownMob = 0;
        for (LivingEntity m : mobs) {
            if (shownMob >= 5) break;
            lines.add(I18n.t("linclient.entity.mob",
                    m.getName().getString(), Format.fmt(m.getHealth()), Format.fmt(m.getMaxHealth())));
            shownMob++;
        }

        lines.add(I18n.t("linclient.entity.mobcount", mobs.size()));
        return lines;
    }
}
