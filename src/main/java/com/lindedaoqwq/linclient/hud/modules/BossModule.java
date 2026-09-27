package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BossHealthOverlay;
import net.minecraft.world.BossEvent;

import java.util.List;
import java.util.Map;

/** Lists active bosses (from the vanilla boss overlay) with a health percentage. */
public class BossModule extends HudModule {
    public BossModule() {
        super("boss", "linclient.module.boss");
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        if (overlay == null) return lines;

        for (Map.Entry<java.util.UUID, BossEvent> entry : overlay.events.entrySet()) {
            BossEvent e = entry.getValue();
            lines.add(I18n.t("linclient.boss.name", e.getName().getString()));
            lines.add(I18n.t("linclient.boss.hp", (int) (e.getProgress() * 100.0f)));
        }
        return lines;
    }
}
