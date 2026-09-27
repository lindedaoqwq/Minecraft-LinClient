package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.List;

public class EntityModule extends HudModule {
    public EntityModule() {
        super("entity", "linclient.module.entity", 0xFF5555);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        World w = mc.world;
        if (w == null) return l;

        int total = 0, players = 0, mobs = 0, animals = 0, items = 0;
        for (Entity e : w.loadedEntityList) {
            if (e == mc.player) continue;
            total++;
            if (e instanceof EntityPlayer) players++;
            else if (e instanceof EntityMob) mobs++;
            else if (e instanceof EntityAnimal) animals++;
            else if (e instanceof EntityItem) items++;
        }
        l.add(I18n.t("linclient.entity.total", total));
        l.add(I18n.t("linclient.entity.players", players));
        l.add(I18n.t("linclient.entity.mobs", mobs));
        l.add(I18n.t("linclient.entity.animals", animals));
        l.add(I18n.t("linclient.entity.items", items));
        return l;
    }
}
