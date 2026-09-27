package com.lindedaoqwq.linclient.config;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Key bindings. Registered during FML initialization via {@link #register()}.
 *
 * OPEN_CONFIG is bound to Right Shift (KEY_RSHIFT) so the in-game config menu can be opened
 * without conflicting with Left Shift (sneak).
 */
public class KeyBindings {
    public static final KeyBinding OPEN_CONFIG = new KeyBinding(
            "key.linclient.openConfig", Keyboard.KEY_RSHIFT, "key.categories.linclient");
    public static final KeyBinding TOGGLE_HUD = new KeyBinding(
            "key.linclient.toggleHud", Keyboard.KEY_H, "key.categories.linclient");
    public static final KeyBinding TOGGLE_MOD = new KeyBinding(
            "key.linclient.toggleMod", Keyboard.KEY_NONE, "key.categories.linclient");

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
        ClientRegistry.registerKeyBinding(TOGGLE_HUD);
        ClientRegistry.registerKeyBinding(TOGGLE_MOD);
    }
}
