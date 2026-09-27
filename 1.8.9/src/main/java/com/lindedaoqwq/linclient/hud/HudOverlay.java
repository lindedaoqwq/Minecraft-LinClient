package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.hud.modules.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders every enabled {@link HudModule} over the game (called from the HUD render event).
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

    public static void render(Minecraft mc, ScaledResolution res) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();
        for (HudModule m : MODULES) {
            m.render(mc, w, h);
        }
    }
}
