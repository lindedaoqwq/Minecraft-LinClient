package com.lindedaoqwq.linclient.event;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.gui.ClickGuiScreen;
import com.lindedaoqwq.linclient.gui.HomeScreen;
import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.hud.HudOverlay;
import com.lindedaoqwq.linclient.state.ClientState;
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

/**
 * All client-side Forge events are handled here. Nothing reads hidden server data or changes
 * server-side behaviour.
 */
public class ClientEvents {
    private float originalGamma = 0.5f;
    private boolean fullbrightActive = false;
    private int origClouds = 0;
    private boolean cloudsOff = false;

    private static final int BTN_LINCLIENT_INGAME = 997;
    private static final int BTN_LINCLIENT_MAINMENU = 998;

    // Last scaled resolution, captured from the HUD render event (avoids constructing
    // ScaledResolution in the mouse handler across versions).
    private static int lastW = 0, lastH = 0;

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        ClientState.refreshFps(mc);   // read FPS from the same source as the F3 debug screen
        if (event.resolution != null) {
            lastW = event.resolution.getScaledWidth();
            lastH = event.resolution.getScaledHeight();
        }
        if (!ClientState.modActive) return;
        if (!ClientState.hudEnabled && !ClientState.hudEditMode) return;
        if (mc.currentScreen != null) return;           // a screen draws its own overlay
        if (mc.gameSettings.showDebugInfo) return;      // don't fight the F3 debug screen
        HudOverlay.render(mc);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // UI open keys are NOT gated by the master switch, so the config is always reachable.
        if (KeyBindings.OPEN_CONFIG.isPressed()) {
            if (ClientState.hudEditMode) {
                ClientState.hudEditMode = false;
                mc.displayGuiScreen(new ClickGuiScreen());
            } else if (mc.currentScreen == null) {
                mc.displayGuiScreen(new ClickGuiScreen());
            }
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
        int btn = event.getButton();
        boolean state = event.isButtonstate();
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

        Minecraft mc = Minecraft.getMinecraft();
        if (ClientState.hudEditMode && mc.currentScreen == null) {
            int mx = event.getX() * lastW / mc.displayWidth;
            int my = (mc.displayHeight - event.getY()) * lastH / mc.displayHeight;
            if (btn == 0 && state) {
                String id = HudOverlay.hitTest(mx, my);
                if (id != null) {
                    HudModule m = HudOverlay.get(id);
                    if (m != null) {
                        ClientState.draggingId = id;
                        ClientState.dragOffsetX = mx - m.posX;
                        ClientState.dragOffsetY = my - m.posY;
                        m.dragging = true;
                    }
                }
                event.setCanceled(true);
            } else if (btn == -1 && ClientState.draggingId != null) {
                HudModule m = HudOverlay.get(ClientState.draggingId);
                if (m != null) {
                    int w = m.rect(mc).w, h = m.rect(mc).h;
                    m.posX = clamp(mx - ClientState.dragOffsetX, 0, lastW - w);
                    m.posY = clamp(my - ClientState.dragOffsetY, 0, lastH - h);
                }
                event.setCanceled(true);
            } else if (btn == 0 && !state && ClientState.draggingId != null) {
                HudModule m = HudOverlay.get(ClientState.draggingId);
                if (m != null) {
                    ModConfig.setModulePos(m.id, m.posX, m.posY);
                    m.dragging = false;
                }
                ClientState.draggingId = null;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        RenderGameOverlayEvent.ElementType type = event.type;
        if (ModConfig.hideHealth && type == RenderGameOverlayEvent.ElementType.HEALTH) event.setCanceled(true);
        if (ModConfig.hideArmor && type == RenderGameOverlayEvent.ElementType.ARMOR) event.setCanceled(true);
        if (ModConfig.hideFood && type == RenderGameOverlayEvent.ElementType.FOOD) event.setCanceled(true);
        if (ModConfig.hideAir && type == RenderGameOverlayEvent.ElementType.AIR) event.setCanceled(true);
        if (ModConfig.hideHotbar && type == RenderGameOverlayEvent.ElementType.HOTBAR) event.setCanceled(true);
        if (ModConfig.hideExp && (type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                || type == RenderGameOverlayEvent.ElementType.JUMPBAR)) event.setCanceled(true);
        if (ModConfig.hideCrosshair && type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) event.setCanceled(true);
        if (ModConfig.hideBoss && (type == RenderGameOverlayEvent.ElementType.BOSSHEALTH
                || type == RenderGameOverlayEvent.ElementType.BOSSINFO)) event.setCanceled(true);
        if (ModConfig.hidePotion && type == RenderGameOverlayEvent.ElementType.POTION_ICONS) event.setCanceled(true);
        if (ModConfig.hideVignette && type == RenderGameOverlayEvent.ElementType.VIGNETTE) event.setCanceled(true);
        if (ModConfig.hidePortal && type == RenderGameOverlayEvent.ElementType.PORTAL) event.setCanceled(true);
        if (ModConfig.hideHelmet && type == RenderGameOverlayEvent.ElementType.HELMENT) event.setCanceled(true);
        if (ModConfig.hideJumpbar && type == RenderGameOverlayEvent.ElementType.JUMPBAR) event.setCanceled(true);
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
            event.buttonList.add(new GuiButton(BTN_LINCLIENT_INGAME, w / 2 - 100, h - 28, 200, 20,
                    com.lindedaoqwq.linclient.util.I18n.t("linclient.gui.openconfig")));
        } else if (gui instanceof GuiOptions) {
            event.buttonList.add(new GuiButton(BTN_LINCLIENT_INGAME, w / 2 - 100, h - 28, 200, 20,
                    com.lindedaoqwq.linclient.util.I18n.t("linclient.gui.openconfig")));
        } else if (gui instanceof GuiMainMenu) {
            event.buttonList.add(new GuiButton(BTN_LINCLIENT_MAINMENU, w / 2 - 100, h - 28, 200, 20,
                    "LinClient"));
        }
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

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
