package com.lindedaoqwq.linclient.config;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Key bindings, registered on the MOD event bus via {@link #register(RegisterKeyMappingsEvent)}.
 */
public class KeyBindings {
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.linclient.toggleHud", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.linclient");

    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.linclient.openEditor", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.linclient");

    public static final KeyMapping TOGGLE_MOD = new KeyMapping(
            "key.linclient.toggleMod", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.linclient");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HUD);
        event.register(OPEN_EDITOR);
        event.register(TOGGLE_MOD);
    }
}
