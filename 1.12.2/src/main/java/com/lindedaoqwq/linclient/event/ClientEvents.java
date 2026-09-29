package com.lindedaoqwq.linclient.event;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.gui.ClickGuiScreen;
import com.lindedaoqwq.linclient.gui.LinMainMenu;
import com.lindedaoqwq.linclient.hud.Hud;
import com.lindedaoqwq.linclient.hud.HudEditorScreen;
import com.lindedaoqwq.linclient.state.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.Map;

/** All client-side Forge events. Client-only; nothing touches server behaviour. */
public class ClientEvents {
    private float originalGamma = 0.5f;
    private boolean fullbrightActive = false;

    // Visual toggle state.
    private boolean bobbingChanged = false;

    // Smart FPS (unfocused throttle).
    private static Field fpsLimitField = null;
    private static boolean fpsLimitDiscovered = false;
    private int savedFpsLimit;
    private boolean focusLow = false;

    // Reconnect.
    private long reconnectAt = 0;
    private GuiScreen lastDisconnectScreen = null;

    // Damage tracking.
    private Entity pendingTarget = null;
    private float pendingHealth = -1F;

    // Freelook.
    private float realYaw, realPitch, flYaw, flPitch;
    private boolean flActive = false;

    private boolean langSynced = false;

    private static final int BTN_LINCLIENT = 997;

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

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        // Count one frame per ALL overlay pass (fires exactly once per rendered frame).
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            ClientState.refreshFps();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!ClientState.modActive || !ClientState.hudEnabled || !Modules.on("hud")) return;
        if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) return;
        Hud.render(mc, false);
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (Modules.on("crosshair") && event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Sync the config language once from the game language.
        if (!langSynced) {
            langSynced = true;
            try {
                ModConfig.setLang(mc.getLanguageManager().getCurrentLanguage().getLanguageCode());
            } catch (Throwable ignored) { }
        }

        if (KeyBindings.OPEN_CONFIG.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new ClickGuiScreen());
        }
        if (KeyBindings.OPEN_HOME.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new HudEditorScreen());
        }
        if (KeyBindings.TOGGLE_MOD.isPressed()) {
            ClientState.modActive = !ClientState.modActive;
        }
        if (KeyBindings.TOGGLE_HUD.isPressed()) {
            ClientState.hudEnabled = !ClientState.hudEnabled;
        }

        applyMovement(mc);
        applyVisuals(mc);
        tickReconnect(mc);
        tickDamageTracking(mc);
        updatePing(mc);
    }

    private void updatePing(Minecraft mc) {
        ClientState.ping = 0;
        if (mc.getConnection() != null && mc.player != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
            if (info != null) ClientState.ping = info.getResponseTime();
        }
    }

    private void applyMovement(Minecraft mc) {
        if (mc.player == null) return;
        // Auto sprint.
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        boolean wantSprint = Modules.on("sprint") && mc.currentScreen == null
                && mc.gameSettings.keyBindForward.isKeyDown()
                && !mc.player.isSneaking() && !mc.player.capabilities.isFlying;
        KeyBinding.setKeyBindState(sprint.getKeyCode(), wantSprint);
    }

    private void applyVisuals(Minecraft mc) {
        if (mc.world == null) return;

        // Fullbright.
        if (Modules.on("fullbright")) {
            if (!fullbrightActive) {
                originalGamma = mc.gameSettings.gammaSetting;
                fullbrightActive = true;
            }
            mc.gameSettings.gammaSetting = 1.0f;
        } else if (fullbrightActive) {
            mc.gameSettings.gammaSetting = originalGamma;
            fullbrightActive = false;
        }

        // Particle control via the vanilla "minimal particles" setting (no SRG hook in 1.12.2).
        if (Modules.on("particles") && mc.gameSettings.particleSetting != 2) {
            mc.gameSettings.particleSetting = 2;
        }

        // Minimal view bobbing (vanilla off; restore when toggled off).
        if (Modules.on("bobbing") && mc.gameSettings.viewBobbing) {
            mc.gameSettings.viewBobbing = false;
            bobbingChanged = true;
        } else if (!Modules.on("bobbing") && bobbingChanged) {
            mc.gameSettings.viewBobbing = true;
            bobbingChanged = false;
        }

        // Weather off.
        if (Modules.on("weather")) {
            mc.world.setRainStrength(0.0f);
            mc.world.setThunderStrength(0.0f);
        }

        // Smart FPS: throttle when unfocused (reflection for version-safe field access).
        Field fl = findFpsLimitField(mc.gameSettings.getClass());
        boolean active = Display.isActive();
        if (Modules.on("smartfps") && fl != null) {
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

        // Dynamic FPS (force-enabled): only lowers render distance on sustained low FPS,
        // never touches the user's own setting otherwise.
        if (Modules.on("dynfps")) {
            int fps = ClientState.fps;
            int cur = mc.gameSettings.renderDistanceChunks;
            if (fps > 0 && fps < 30 && cur > 4) {
                mc.gameSettings.renderDistanceChunks = 4;
            }
        }
    }

    private void tickReconnect(Minecraft mc) {
        if (!(Modules.on("reconnect")) || mc.currentScreen == null
                || !(mc.currentScreen instanceof GuiDisconnected)) {
            lastDisconnectScreen = null;
            return;
        }
        if (mc.currentScreen != lastDisconnectScreen) {
            lastDisconnectScreen = mc.currentScreen;
            reconnectAt = System.currentTimeMillis() + 5000L;
            return;
        }
        if (System.currentTimeMillis() < reconnectAt) return;
        reconnectAt = System.currentTimeMillis() + 5000L;
        try {
            ServerDataBox.connect(mc.currentScreen);
        } catch (Throwable ignored) { }
    }

    /** Reflection helper: finds the ServerData stored inside GuiDisconnected and reconnects. */
    private static class ServerDataBox {
        static void connect(GuiScreen screen) throws Exception {
            for (Field f : screen.getClass().getDeclaredFields()) {
                if (f.getType().getSimpleName().equals("ServerData")) {
                    f.setAccessible(true);
                    Object data = f.get(screen);
                    if (data != null) {
                        FMLClientHandler.instance().connectToServer(screen,
                                (net.minecraft.client.multiplayer.ServerData) data);
                    }
                    return;
                }
            }
        }
    }

    private void tickDamageTracking(Minecraft mc) {
        if (pendingTarget == null) return;
        if (!(pendingTarget instanceof EntityLivingBase) || ((Entity) pendingTarget).isDead) {
            pendingTarget = null;
            return;
        }
        float now = ((EntityLivingBase) pendingTarget).getHealth();
        if (now < pendingHealth - 0.01F) {
            ClientState.lastDamageDealt = pendingHealth - now;
            ClientState.combo++;
            ClientState.comboExpire = System.currentTimeMillis() + 2000L;
            ClientState.reachExpire = System.currentTimeMillis() + 2000L;
            pendingTarget = null;
        } else if (now > pendingHealth) {
            pendingHealth = now;   // healed in between; keep waiting
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || event.getEntity() != mc.player) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        ClientState.reach = (float) mc.player.getDistance(event.getTarget());
        ClientState.reachExpire = System.currentTimeMillis() + 2000L;
        pendingTarget = event.getTarget();
        pendingHealth = ((EntityLivingBase) event.getTarget()).getHealth();
    }

    private static Field fovSrcField, fovNewField;
    private static boolean fovDiscovered = false;

    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        if (!(Modules.on("zoom") && KeyBindings.ZOOM.isKeyDown())) return;
        if (!fovDiscovered) {
            fovDiscovered = true;
            for (Field f : event.getClass().getDeclaredFields()) {
                if (f.getName().equals("fov") || f.getName().equals("newFov")) f.setAccessible(true);
                if (f.getName().equals("fov")) fovSrcField = f;
                if (f.getName().equals("newFov")) fovNewField = f;
            }
        }
        try {
            if (fovSrcField != null && fovNewField != null) {
                fovNewField.setFloat(event, fovSrcField.getFloat(event) * 0.2F);
            }
        } catch (Exception ignored) { }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) { flActive = false; return; }
        boolean down = Modules.on("freelook") && KeyBindings.FREELOOK.isKeyDown();
        if (event.phase == TickEvent.Phase.START) {
            if (down) {
                if (!flActive) {
                    flActive = true;
                    realYaw = mc.player.rotationYaw;
                    realPitch = mc.player.rotationPitch;
                    flYaw = realYaw;
                    flPitch = realPitch;
                }
                mc.player.rotationYaw = flYaw;
                mc.player.rotationPitch = flPitch;
            }
        } else if (flActive) {
            if (down) {
                mc.player.rotationYaw = realYaw;
                mc.player.rotationPitch = realPitch;
            } else {
                flActive = false;
            }
        }
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
    }

    @SubscribeEvent
    public void onBlockHighlight(DrawBlockHighlightEvent event) {
        if (!Modules.on("blockoutline")) return;
        Minecraft mc = Minecraft.getMinecraft();
        RayTraceResult mop = event.getTarget();
        if (mop == null || mop.typeOfHit != RayTraceResult.Type.BLOCK
                || mop.getBlockPos() == null || mc.player == null) return;
        int x = mop.getBlockPos().getX(), y = mop.getBlockPos().getY(), z = mop.getBlockPos().getZ();
        EntityPlayerSP p = mc.player;
        double dx = p.lastTickPosX + (p.posX - p.lastTickPosX) * event.getPartialTicks();
        double dy = p.lastTickPosY + (p.posY - p.lastTickPosY) * event.getPartialTicks();
        double dz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * event.getPartialTicks();
        event.setCanceled(true);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2.0F);
        float[] rgb = argb(ModConfig.color("blockoutline", 0xFF3BA9F0));
        GL11.glColor4f(rgb[0], rgb[1], rgb[2], 0.9F);
        GL11.glTranslated(-dx, -dy, -dz);
        GL11.glBegin(GL11.GL_LINES);
        float x1 = x - 0.005F, y1 = y - 0.005F, z1 = z - 0.005F;
        float x2 = x + 1.005F, y2 = y + 1.005F, z2 = z + 1.005F;
        // bottom rectangle
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x2, y1, z1);
        GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x2, y1, z2);
        GL11.glVertex3f(x2, y1, z2); GL11.glVertex3f(x1, y1, z2);
        GL11.glVertex3f(x1, y1, z2); GL11.glVertex3f(x1, y1, z1);
        // top rectangle
        GL11.glVertex3f(x1, y2, z1); GL11.glVertex3f(x2, y2, z1);
        GL11.glVertex3f(x2, y2, z1); GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x2, y2, z2); GL11.glVertex3f(x1, y2, z2);
        GL11.glVertex3f(x1, y2, z2); GL11.glVertex3f(x1, y2, z1);
        // verticals
        GL11.glVertex3f(x1, y1, z1); GL11.glVertex3f(x1, y2, z1);
        GL11.glVertex3f(x2, y1, z1); GL11.glVertex3f(x2, y2, z1);
        GL11.glVertex3f(x2, y1, z2); GL11.glVertex3f(x2, y2, z2);
        GL11.glVertex3f(x1, y1, z2); GL11.glVertex3f(x1, y2, z2);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glPopMatrix();
    }

    /** ARGB -> {r,g,b} floats in [0,1]. */
    private static float[] argb(int c) {
        return new float[]{((c >> 16) & 0xFF) / 255F, ((c >> 8) & 0xFF) / 255F, (c & 0xFF) / 255F};
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.getOverlayType() == RenderBlockOverlayEvent.OverlayType.FIRE && Modules.on("lowfire")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Clean, non-reentrant replacement of the vanilla main menu.
        if (event.getGui() instanceof GuiMainMenu) {
            event.setGui(new LinMainMenu());
        }
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (gui instanceof GuiIngameMenu || gui instanceof GuiOptions) {
            int bw = 120, bh = 20;
            int x = gui.width - bw - 6;
            int y = 6;
            event.getButtonList().add(new GuiButton(BTN_LINCLIENT, x, y, bw, bh, "LinClient"));
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getButton() != null && event.getButton().id == BTN_LINCLIENT) {
            Minecraft.getMinecraft().displayGuiScreen(new ClickGuiScreen());
        }
    }
}
