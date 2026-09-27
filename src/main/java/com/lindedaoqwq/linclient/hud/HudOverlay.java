package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.LayoutConfig;
import com.lindedaoqwq.linclient.hud.modules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders every enabled {@link HudModule}, ordered by its layout "order" value.
 */
public final class HudOverlay {
    private HudOverlay() {}

    private static final List<HudModule> MODULES = new ArrayList<>();

    static {
        MODULES.add(new SelfStatusModule());
        MODULES.add(new EnvironmentModule());
        MODULES.add(new ItemModule());
        MODULES.add(new InputModule());
        MODULES.add(new OtherModule());
        MODULES.add(new EntityModule());
        MODULES.add(new CombatModule());
        MODULES.add(new BossModule());
    }

    public static List<HudModule> all() {
        return MODULES;
    }

    public static void render(GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        List<HudModule> ordered = new ArrayList<>(MODULES);
        ordered.sort(Comparator.comparingInt(m -> LayoutConfig.layoutOf(m.id).order));

        for (HudModule m : ordered) {
            m.render(g, mc.font, mc, w, h);
        }
    }
}
