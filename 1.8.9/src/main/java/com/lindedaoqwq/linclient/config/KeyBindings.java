package com.lindedaoqwq.linclient.config;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Key bindings. Registered during FML initialization via {@link #register()}.
 *
 * OPEN_CONFIG is bound to Right Shift (KEY_RSHIFT) so the config GUI can be opened without
 * conflicting with Left Shift (sneak). OPEN_HOME (F8) opens the standalone Home screen.
 * Neither is gated by the mod's master switch, so the UI is always reachable.
 */
public class KeyBindings {
    public static final KeyBinding OPEN_CONFIG = new KeyBinding(
            "key.linclient.openConfig", Keyboard.KEY_RSHIFT, "key.categories.linclient");
    public static final KeyBinding OPEN_HOME = new KeyBinding(
            "key.linclient.openHome", Keyboard.KEY_F8, "key.categories.linclient");
    public static final KeyBinding TOGGLE_HUD = new KeyBinding(
            "key.linclient.toggleHud", Keyboard.KEY_H, "key.categories.linclient");
    public static final KeyBinding TOGGLE_MOD = new KeyBinding(
            "key.linclient.toggleMod", Keyboard.KEY_NONE, "key.categories.linclient");
    public static final KeyBinding ZOOM = new KeyBinding(
            "key.linclient.zoom", Keyboard.KEY_C, "key.categories.linclient");
    public static final KeyBinding FREELOOK = new KeyBinding(
            "key.linclient.freelook", Keyboard.KEY_F, "key.categories.linclient");

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
        ClientRegistry.registerKeyBinding(OPEN_HOME);
        ClientRegistry.registerKeyBinding(TOGGLE_HUD);
        ClientRegistry.registerKeyBinding(TOGGLE_MOD);
        ClientRegistry.registerKeyBinding(ZOOM);
        ClientRegistry.registerKeyBinding(FREELOOK);
    }
}
