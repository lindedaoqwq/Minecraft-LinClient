package com.lindedaoqwq.linclient.hud;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.Configuration;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game configuration menu, opened with Right Shift (see {@code KeyBindings.OPEN_CONFIG}).
 *
 * Lists every toggle as a button; clicking flips the value in the Forge config and saves it,
 * so the change takes effect immediately. ESC closes and saves.
 */
public class ConfigMenuScreen extends GuiScreen {
    private static final int COL_W = 300;
    private static final int ROW_H = 20;
    private static final int GAP = 24;

    private final List<Toggle> toggles = new ArrayList<>();

    private static class Toggle {
        final String category;
        final String name;
        final String langKey;
        final GuiButton button;

        Toggle(String category, String name, String langKey, GuiButton button) {
            this.category = category;
            this.name = name;
            this.langKey = langKey;
            this.button = button;
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.toggles.clear();

        int x = this.width / 2 - COL_W / 2;
        int y = 44;

        y = addToggle(ModConfig.CAT_GENERAL, "enableMod", "linclient.config.enableMod", x, y);
        y = addToggle(ModConfig.CAT_GENERAL, "showHud", "linclient.config.showHud", x, y);
        y = addToggle(ModConfig.CAT_VISUAL, "fullbright", "linclient.config.fullbright", x, y);
        y = addToggle(ModConfig.CAT_VISUAL, "disableClouds", "linclient.config.disableClouds", x, y);
        y = addToggle(ModConfig.CAT_VISUAL, "disableFireOverlay", "linclient.config.disableFireOverlay", x, y);
        y = addToggle(ModConfig.CAT_VISUAL, "disableWaterOverlay", "linclient.config.disableWaterOverlay", x, y);
        y = addToggle(ModConfig.CAT_PERF, "dynamicFps", "linclient.config.dynamicFps", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "self_status", "linclient.module.self_status", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "environment", "linclient.module.environment", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "item", "linclient.module.item", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "input", "linclient.module.input", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "other", "linclient.module.other", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "entity", "linclient.module.entity", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "combat", "linclient.module.combat", x, y);
        y = addToggle(ModConfig.CAT_MODULES, "boss", "linclient.module.boss", x, y);
    }

    private int addToggle(String category, String name, String langKey, int x, int y) {
        Configuration cfg = ModConfig.get();
        boolean val = cfg != null && cfg.getBoolean(name, category, false, "");
        String txt = I18n.t(langKey) + ": " + (val ? I18n.t("linclient.on") : I18n.t("linclient.off"));
        GuiButton b = new GuiButton(this.toggles.size(), x, y, COL_W, ROW_H, txt);
        this.buttonList.add(b);
        this.toggles.add(new Toggle(category, name, langKey, b));
        return y + GAP;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == null || button.id < 0 || button.id >= this.toggles.size()) return;
        Toggle t = this.toggles.get(button.id);
        ModConfig.toggle(t.category, t.name);
        boolean val = ModConfig.get().getBoolean(t.name, t.category, false, "");
        button.displayString = I18n.t(t.langKey) + ": " + (val ? I18n.t("linclient.on") : I18n.t("linclient.off"));
        if ("enableMod".equals(t.name)) ClientState.modActive = val;
        if ("showHud".equals(t.name)) ClientState.hudEnabled = val;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.t("linclient.config.title"), this.width / 2, 16, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj, I18n.t("linclient.config.hint"), this.width / 2, this.height - 22, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            ModConfig.save();
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
