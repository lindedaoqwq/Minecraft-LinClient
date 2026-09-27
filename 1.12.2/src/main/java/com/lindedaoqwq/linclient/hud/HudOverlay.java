package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.hud.modules.*;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders every enabled {@link HudModule} over the game (called from the HUD render event), and
 * provides hit-testing / lookup used by the in-game drag editor.
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
        for (HudModule m : MODULES) {
            m.posX = ModConfig.getModulePosX(m.id);
            m.posY = ModConfig.getModulePosY(m.id);
        }
    }

    public static List<HudModule> all() {
        return MODULES;
    }

    public static HudModule get(String id) {
        for (HudModule m : MODULES) if (m.id.equals(id)) return m;
        return null;
    }

    public static String hitTest(int mx, int my) {
        return hitTest(mx, my, false);
    }

    /** Hit-test modules; when {@code all} is true disabled modules are included (HUD editor). */
    public static String hitTest(int mx, int my, boolean all) {
        for (int i = MODULES.size() - 1; i >= 0; i--) {
            HudModule m = MODULES.get(i);
            if ((all || m.isEnabled()) && m.contains(mx, my)) return m.id;
        }
        return null;
    }

    public static void render(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        for (HudModule m : MODULES) {
            m.render(mc);
        }
    }

    /** Renders every module including disabled ones (dimmed), used by the HUD layout editor. */
    public static void renderAll(Minecraft mc) {
        for (HudModule m : MODULES) {
            m.render(mc, true);
        }
    }
}
