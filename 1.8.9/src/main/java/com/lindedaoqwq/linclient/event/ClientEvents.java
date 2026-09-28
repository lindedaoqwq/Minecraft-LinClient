package com.lindedaoqwq.linclient.event;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.gui.ClickGuiScreen;
import com.lindedaoqwq.linclient.gui.HomeScreen;
import com.lindedaoqwq.linclient.hud.HudOverlay;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Field;

/**
 * All client-side Forge events are handled here. Nothing reads hidden server data or changes
 * server-side behaviour.
 */
public class ClientEvents {
    private float originalGamma = 0.5f;
    private boolean fullbrightActive = false;

    // Save/restore state for visual toggles.
    private int origClouds = 0;
    private boolean cloudsOff = false;
    private boolean origFancy;
    private boolean fastGraphicsActive = false;
    private int origAo;
    private boolean noSmoothActive = false;
    private int origParticle;
    private boolean lowPartActive = false;
    private int savedFpsLimit;
    private boolean focusLow = false;

    // Frame-limit field name differs across Minecraft versions; resolved reflectively at runtime.
    private static Field fpsLimitField = null;
    private static boolean fpsLimitDiscovered = false;

    private static Field findFpsLimitField(Class<?> c) {
        if (fpsLimitDiscovered) return fpsLimitField;
        fpsLimitDiscovered = true;
        for (String name : new String[]{"framerateLimit", "limitFramerate", "frameRateLimit", "frameLimit", "maxFps"}) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                fpsLimitField = f;
                break;
            } catch (Exception ignored) { }
        }
        return fpsLimitField;
    }

    private static final int BTN_LINCLIENT_INGAME = 997;
    private static final int BTN_LINCLIENT_MAINMENU = 998;

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        ClientState.refreshFps();   // self-counted frame rate, reliable across versions
        if (!ClientState.modActive) return;
        if (!ClientState.hudEnabled) return;
        if (mc.currentScreen != null) return;           // screens (incl. the HUD editor) draw their own
        if (mc.gameSettings.showDebugInfo) return;      // don't fight the F3 debug screen
        HudOverlay.render(mc);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // UI open keys are NOT gated by the master switch, so the config is always reachable.
        if (KeyBindings.OPEN_CONFIG.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new ClickGuiScreen());
        }
        if (KeyBindings.OPEN_HOME.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new HomeScreen());
        }

        if (KeyBindings.TOGGLE_MOD.isPressed()) {
            ClientState.modActive = !ClientState.modActive;
            ModConfig.enableMod = ClientState.modActive;
        }
        if (ClientState.modActive && KeyBindings.TOGGLE_HUD.isPressed()) {
            ClientState.hudEnabled = !ClientState.hudEnabled;
        }

        applyAutoSprint(mc);

        if (mc.thePlayer == null || mc.theWorld == null) return;
        updateClientState(mc);
        applyVisuals(mc);
    }

    private void applyAutoSprint(Minecraft mc) {
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        boolean want = false;
        if (ModConfig.autoSprint && mc.thePlayer != null && mc.currentScreen == null) {
            boolean fwd = mc.gameSettings.keyBindForward.isKeyDown();
            boolean sneak = mc.thePlayer.isSneaking();
            boolean flying = mc.thePlayer.capabilities.isFlying;
            want = fwd && !sneak && !flying;
        }
        KeyBinding.setKeyBindState(sprint.getKeyCode(), want);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        // 1.8.9 exposes MouseEvent values as public final fields (getters only exist in 1.12+).
        int btn = event.button;
        boolean state = event.buttonstate;
        if (btn == 0 && state) {
            ClientState.addLeftClick();
            ClientState.leftHeld = true;
        } else if (btn == 0 && !state) {
            ClientState.leftHeld = false;
        }
        if (btn == 1 && state) {
            ClientState.addRightClick();
            ClientState.rightHeld = true;
        } else if (btn == 1 && !state) {
            ClientState.rightHeld = false;
        }
        // HUD module dragging lives in HudEditorScreen, not here.
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        // Only element types that exist in 1.8.9 (no BOSSINFO / POTION_ICONS / VIGNETTE here,
        // and the helmet overlay is spelled HELMET in this version).
        RenderGameOverlayEvent.ElementType type = event.type;
        // Vanilla HUD visibility: a toggle that is OFF hides the element, ON shows it (default = shown).
        if (!ModConfig.showHealth && type == RenderGameOverlayEvent.ElementType.HEALTH) event.setCanceled(true);
        if (!ModConfig.showArmor && type == RenderGameOverlayEvent.ElementType.ARMOR) event.setCanceled(true);
        if (!ModConfig.showFood && type == RenderGameOverlayEvent.ElementType.FOOD) event.setCanceled(true);
        if (!ModConfig.showAir && type == RenderGameOverlayEvent.ElementType.AIR) event.setCanceled(true);
        if (!ModConfig.showHotbar && type == RenderGameOverlayEvent.ElementType.HOTBAR) event.setCanceled(true);
        if (!ModConfig.showExp && (type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                || type == RenderGameOverlayEvent.ElementType.JUMPBAR)) event.setCanceled(true);
        if (!ModConfig.showCrosshair && type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) event.setCanceled(true);
        if (!ModConfig.showBoss && type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) event.setCanceled(true);
        if (!ModConfig.showPortal && type == RenderGameOverlayEvent.ElementType.PORTAL) event.setCanceled(true);
        if (!ModConfig.showHelmet && type == RenderGameOverlayEvent.ElementType.HELMET) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE && ModConfig.disableFireOverlay) {
            event.setCanceled(true);
        }
        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.WATER && ModConfig.disableWaterOverlay) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        int w = gui.width, h = gui.height;
        if (gui instanceof GuiIngameMenu) {
            placeLinClientButton(event, BTN_LINCLIENT_INGAME, I18n.t("linclient.gui.openconfig"), w, h);
        } else if (gui instanceof GuiOptions) {
            placeLinClientButton(event, BTN_LINCLIENT_INGAME, I18n.t("linclient.gui.openconfig"), w, h);
        } else if (gui instanceof GuiMainMenu) {
            placeLinClientButton(event, BTN_LINCLIENT_MAINMENU, "LinClient", w, h);
        }
    }

    /**
     * Adds the LinClient button at the bottom-right, then moves it upward if it would overlap any
     * existing vanilla button (avoids the previous overlap with "Done" / "Quit Game").
     */
    private void placeLinClientButton(GuiScreenEvent.InitGuiEvent.Post event, int id, String label, int w, int h) {
        int bw = 200, bh = 20;
        int x = w - bw - 10;
        int y = h - bh - 4;
        boolean overlap;
        int guard = 0;
        do {
            overlap = false;
            for (GuiButton b : event.buttonList) {
                if (b == null) continue;
                if (b.xPosition < x + bw && b.xPosition + b.width > x
                        && b.yPosition < y + bh && b.yPosition + b.height > y) {
                    overlap = true;
                    break;
                }
            }
            if (overlap) y -= (bh + 6);
        } while (overlap && y > 4 && guard++ < 60);
        event.buttonList.add(new GuiButton(id, x, Math.max(4, y), bw, bh, label));
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        GuiScreen gui = event.gui;
        GuiButton b = event.button;
        if (b == null) return;
        if ((gui instanceof GuiIngameMenu || gui instanceof GuiOptions) && b.id == BTN_LINCLIENT_INGAME) {
            Minecraft.getMinecraft().displayGuiScreen(new ClickGuiScreen());
        } else if (gui instanceof GuiMainMenu && b.id == BTN_LINCLIENT_MAINMENU) {
            Minecraft.getMinecraft().displayGuiScreen(new HomeScreen());
        }
    }

    private void updateClientState(Minecraft mc) {
        net.minecraft.client.entity.EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) return;
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
                ClientState.lastDeathDim = mc.theWorld.provider.getDimensionName();
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
        // Fullbright (gamma).
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

        // Clouds.
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

        // Fast graphics (fancyGraphics = false): much cheaper geometry. Reload renderers once on toggle.
        if (ModConfig.fastGraphics) {
            if (!fastGraphicsActive) {
                origFancy = mc.gameSettings.fancyGraphics;
                fastGraphicsActive = true;
                mc.renderGlobal.loadRenderers();
            }
            mc.gameSettings.fancyGraphics = false;
        } else if (fastGraphicsActive) {
            mc.gameSettings.fancyGraphics = origFancy;
            fastGraphicsActive = false;
            mc.renderGlobal.loadRenderers();
        }

        // Smooth lighting off (ambientOcclusion = 0): large lighting cost reduction.
        if (ModConfig.noSmoothLight) {
            if (!noSmoothActive) {
                origAo = mc.gameSettings.ambientOcclusion;
                noSmoothActive = true;
                mc.renderGlobal.loadRenderers();
            }
            mc.gameSettings.ambientOcclusion = 0;
        } else if (noSmoothActive) {
            mc.gameSettings.ambientOcclusion = origAo;
            noSmoothActive = false;
            mc.renderGlobal.loadRenderers();
        }

        // Minimal particles.
        if (ModConfig.lowParticles) {
            if (!lowPartActive) {
                origParticle = mc.gameSettings.particleSetting;
                lowPartActive = true;
            }
            mc.gameSettings.particleSetting = 2;
        } else if (lowPartActive) {
            mc.gameSettings.particleSetting = origParticle;
            lowPartActive = false;
        }

        // Smart FPS: throttle to a low frame rate when the window is not focused, so the machine
        // spends its budget on chunk loading / background work instead of drawing. The frame-limit
        // field name differs across versions, so it is resolved reflectively.
        Field fl = findFpsLimitField(mc.gameSettings.getClass());
        boolean active = Display.isActive();
        if (ModConfig.smartFps && mc.theWorld != null && fl != null) {
            try {
                if (!active) {
                    if (!focusLow) {
                        savedFpsLimit = (Integer) fl.get(mc.gameSettings);
                        focusLow = true;
                    }
                    fl.set(mc.gameSettings, 10);
                } else if (focusLow) {
                    fl.set(mc.gameSettings, savedFpsLimit);
                    focusLow = false;
                }
            } catch (Exception ignored) { }
        } else if (focusLow && fl != null) {
            try { fl.set(mc.gameSettings, savedFpsLimit); } catch (Exception ignored) { }
            focusLow = false;
        }

        // Dynamic FPS: auto-lower render distance when the frame rate drops.
        if (ModConfig.dynamicFps && mc.theWorld != null) {
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
