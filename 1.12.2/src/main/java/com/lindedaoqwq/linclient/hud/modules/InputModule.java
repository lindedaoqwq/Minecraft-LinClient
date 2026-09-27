package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.Format;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;

import java.util.List;

public class InputModule extends HudModule {
    public InputModule() {
        super("input", "linclient.module.input", false, 0xFFFF55);
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> l = lines();
        l.add(I18n.t("linclient.input.cpsL", Format.f(ClientState.cpsLeft)));
        l.add(I18n.t("linclient.input.cpsR", Format.f(ClientState.cpsRight)));
        l.add(I18n.t("linclient.input.left", ClientState.leftDown ? I18n.t("linclient.on") : I18n.t("linclient.off")));
        l.add(I18n.t("linclient.input.right", ClientState.rightDown ? I18n.t("linclient.off") : I18n.t("linclient.on")));
        l.add(I18n.t("linclient.input.sneak", mc.gameSettings.keyBindSneak.isKeyDown() ? I18n.t("linclient.on") : I18n.t("linclient.off")));
        l.add(I18n.t("linclient.input.sprint", mc.gameSettings.keyBindSprint.isKeyDown() ? I18n.t("linclient.on") : I18n.t("linclient.off")));
        l.add(I18n.t("linclient.input.fps", ClientState.fps));
        return l;
    }
}
