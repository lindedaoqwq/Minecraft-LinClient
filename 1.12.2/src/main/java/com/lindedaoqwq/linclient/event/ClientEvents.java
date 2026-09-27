package com.lindedaoqwq.linclient.event;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.hud.ConfigMenuScreen;
import com.lindedaoqwq.linclient.hud.HudOverlay;
import com.lindedaoqwq.linclient.state.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * All client-side Forge events are handled here. Nothing reads hidden server data or changes
 * server-side behaviour.
 */
public class ClientEvents {
    private float originalGamma = 0.5f;
    private boolean fullbrightActive = false;
    private int origClouds = 0;
    private boolean cloudsOff = false;

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        ClientState.onFrame();   // count a rendered frame for FPS
        if (!ClientState.modActive || !ClientState.hudEnabled) return;
        if (mc.currentScreen != null) return;           // a screen draws its own overlay
        if (mc.gameSettings.showDebugInfo) return;      // don't fight the F3 debug screen
        ScaledResolution res = event.getResolution();
        HudOverlay.render(mc, res);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Key bindings (polled every tick so presses don't latch).
        if (KeyBindings.TOGGLE_MOD.isPressed()) {
            ClientState.modActive = !ClientState.modActive;
            ModConfig.enableMod = ClientState.modActive;
        }
        if (ClientState.modActive && KeyBindings.TOGGLE_HUD.isPressed()) {
            ClientState.hudEnabled = !ClientState.hudEnabled;
        }
        if (ClientState.modActive && KeyBindings.OPEN_CONFIG.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new ConfigMenuScreen());
        }

        if (mc.player == null || mc.world == null) return;

        updateClientState(mc);
        applyVisuals(mc);
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.getOverlayType() == RenderBlockOverlayEvent.OverlayType.FIRE && ModConfig.disableFireOverlay) {
            event.setCanceled(true);
        }
        if (event.getOverlayType() == RenderBlockOverlayEvent.OverlayType.WATER && ModConfig.disableWaterOverlay) {
            event.setCanceled(true);
        }
    }

    private void updateClientState(Minecraft mc) {
        net.minecraft.client.entity.EntityPlayerSP p = mc.player;
        if (p == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        double dx = p.posX - ClientState.prevX;
        double dz = p.posZ - ClientState.prevZ;
        if (ClientState.hasPrev) {
            double d = Math.sqrt(dx * dx + dz * dz);
            ClientState.speed = d * 20.0;
            ClientState.distance += d;
        }
        ClientState.prevX = p.posX;
        ClientState.prevY = p.posY;
        ClientState.prevZ = p.posZ;
        ClientState.hasPrev = true;

        float hp = p.getHealth() + p.getAbsorptionAmount();
        if (ClientState.lastPlayerHealth > 0.0f && hp < ClientState.lastPlayerHealth - 0.01f) {
            ClientState.lastDamageTaken = ClientState.lastPlayerHealth - hp;
            ClientState.lastDamageTakenTime = now;
        }
        ClientState.lastPlayerHealth = hp;

        if (p.isDead) {
            if (!ClientState.wasDead) {
                ClientState.lastDeathTime = now;
                ClientState.lastDeathDim = mc.world.provider.getDimensionType().getName();
                ClientState.lastDeathX = (int) p.posX;
                ClientState.lastDeathY = (int) p.posY;
                ClientState.lastDeathZ = (int) p.posZ;
            }
            ClientState.wasDead = true;
        } else {
            if (ClientState.wasDead) ClientState.lastPlayerHealth = p.getHealth() + p.getAbsorptionAmount();
            ClientState.wasDead = false;
        }

        if (now > ClientState.comboExpire) ClientState.combo = 0;
    }

    private void applyVisuals(Minecraft mc) {
        // Fullbright (gamma)
        if (ModConfig.fullbright) {
            if (!fullbrightActive) {
                originalGamma = mc.gameSettings.gammaSetting;
                fullbrightActive = true;
            }
            mc.gameSettings.gammaSetting = 1.0f;
        } else if (fullbrightActive) {
            mc.gameSettings.gammaSetting = originalGamma;
            fullbrightActive = false;
        }

        // Clouds (1.12.2: GameSettings.clouds is an int flag 0=OFF/1=FAST/2=FANCY)
        if (ModConfig.disableClouds) {
            if (!cloudsOff) {
                origClouds = mc.gameSettings.clouds;
                cloudsOff = true;
            }
            mc.gameSettings.clouds = 0;
        } else if (cloudsOff) {
            mc.gameSettings.clouds = origClouds;
            cloudsOff = false;
        }

        // Dynamic FPS: auto-adjust render distance (client setting only).
        if (ModConfig.dynamicFps && mc.world != null) {
            int fps = ClientState.fps;
            int cur = mc.gameSettings.renderDistanceChunks;
            if (fps < ModConfig.dynamicFpsMin && cur > ModConfig.dynamicFpsMinDist) {
                mc.gameSettings.renderDistanceChunks = ModConfig.dynamicFpsMinDist;
            } else if (fps > ModConfig.dynamicFpsRestore && cur < ModConfig.dynamicFpsMaxDist) {
                mc.gameSettings.renderDistanceChunks = ModConfig.dynamicFpsMaxDist;
            }
        }
    }
}
