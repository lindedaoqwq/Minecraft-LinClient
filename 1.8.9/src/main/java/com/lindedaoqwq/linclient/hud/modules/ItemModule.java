package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemModule extends HudModule {
    public ItemModule() {
        super("item", "linclient.module.item", 0xFFAA00);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        EntityPlayerSP p = mc.thePlayer;
        if (p == null) return l;

        ItemStack held = p.getHeldItem();
        if (held != null && held.getItem() != null) {
            l.add(I18n.t("linclient.item.held", held.getDisplayName()));
            int max = held.getMaxDamage();
            if (max > 0) l.add(I18n.t("linclient.item.durability", held.getItemDamage(), max));
        } else {
            l.add(I18n.t("linclient.item.held", I18n.t("linclient.none")));
        }

        int total = 0, stacks = 0;
        for (ItemStack s : p.inventory.mainInventory) {
            if (s != null && s.getItem() != null && s.stackSize > 0) {
                total += s.stackSize;
                stacks++;
            }
        }
        l.add(I18n.t("linclient.item.total", total));
        l.add(I18n.t("linclient.item.stacks", stacks));
        return l;
    }
}
