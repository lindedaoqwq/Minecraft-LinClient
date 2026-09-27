package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Armor & tool durability, arrow count, bow/crossbow draw, backpack summary, nearby drops. */
public class ItemModule extends HudModule {
    public ItemModule() {
        super("item", "linclient.module.item");
    }

    private static int pct(ItemStack s) {
        int max = s.getMaxDamageValue();
        if (max <= 0) return 100;
        return (int) ((1.0 - s.getDamageValue() / (double) max) * 100.0);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        if (mc.player == null) return lines;

        for (ItemStack s : mc.player.getArmorSlots()) {
            if (!s.isEmpty() && s.isDamageableItem()) {
                lines.add(I18n.t("linclient.item.armor", s.getHoverName().getString(), pct(s)));
            }
        }

        ItemStack mh = mc.player.getMainHandItem();
        if (!mh.isEmpty() && mh.isDamageableItem()) {
            lines.add(I18n.t("linclient.item.tool", mh.getHoverName().getString(), pct(mh), mh.getMaxDamageValue() - mh.getDamageValue()));
        }

        int arrows = mc.player.getInventory().countItem(Items.ARROW)
                + mc.player.getInventory().countItem(Items.TIPPED_ARROW)
                + mc.player.getInventory().countItem(Items.SPECTRAL_ARROW);
        lines.add(I18n.t("linclient.item.arrows", arrows));

        ItemStack use = mc.player.getUseItem();
        if (use.getItem() instanceof BowItem) {
            float prog = Math.min(1.0f, mc.player.getTicksUsingItem() / 20.0f);
            lines.add(I18n.t("linclient.item.bow", (int) (prog * 100.0f)));
        } else if (use.getItem() instanceof CrossbowItem) {
            lines.add(I18n.t("linclient.item.crossbow", CrossbowItem.isCharged(use) ? "✔" : "✘"));
        }

        var inv = mc.player.getInventory();
        int slots = 0, total = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty()) {
                slots++;
                total += s.getCount();
            }
        }
        lines.add(I18n.t("linclient.item.backpack", slots, inv.items.size(), total));

        if (mc.level != null) {
            List<ItemEntity> drops = new ArrayList<>();
            for (Entity e : mc.level.getEntities().getAll()) {
                if (e instanceof ItemEntity ie) drops.add(ie);
            }
            drops.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));
            int n = 0;
            for (ItemEntity ie : drops) {
                if (n >= 3) break;
                ItemStack it = ie.getItem();
                lines.add(I18n.t("linclient.item.drop", it.getHoverName().getString(), it.getCount()));
                n++;
            }
        }
        return lines;
    }
}
