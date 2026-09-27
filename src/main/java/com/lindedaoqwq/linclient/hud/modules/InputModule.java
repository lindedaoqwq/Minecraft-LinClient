package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.util.List;

/** Live WASD / jump / sneak / sprint state, CPS (left & right), and mouse-button state. */
public class InputModule extends HudModule {
    public InputModule() {
        super("input", "linclient.module.input");
    }

    private static String on(boolean b) {
        return b ? "■" : "□";
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        Options o = mc.options;

        lines.add(I18n.t("linclient.input.move",
                on(o.keyUp.isDown()), on(o.keyLeft.isDown()), on(o.keyDown.isDown()), on(o.keyRight.isDown())));
        lines.add(I18n.t("linclient.input.actions",
                on(o.keyJump.isDown()), on(o.keyShift.isDown()), on(o.keySprint.isDown())));
        lines.add(I18n.t("linclient.input.cps", ClientState.getCps(true), ClientState.getCps(false)));
        lines.add(I18n.t("linclient.input.mouse", on(ClientState.leftDown), on(ClientState.rightDown)));

        return lines;
    }
}
