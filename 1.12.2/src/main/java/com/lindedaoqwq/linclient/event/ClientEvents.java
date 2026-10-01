package com.lindedaoqwq.linclient.event;

import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import com.lindedaoqwq.linclient.hooks.EngineHooks;
import com.lindedaoqwq.linclient.gui.ClickGuiScreen;
import com.lindedaoqwq.linclient.gui.LinMainMenu;
import com.lindedaoqwq.linclient.hud.Hud;
import com.lindedaoqwq.linclient.hud.HudEditorScreen;
import com.lindedaoqwq.linclient.state.ClientState;
import com.lindedaoqwq.linclient.util.MotionBlur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;

/** All client-side Forge events. Client-only; nothing touches server behaviour. */
public class ClientEvents {
    private float originalGamma = 0.5f;
    private boolean fullbrightActive = false;

    // Damage tracking (combo / reach read-outs).
    private Entity pendingTarget = null;
    private float pendingHealth = -1F;

    // World-switch detection: resets session stats so nothing leaks across worlds.
    private WorldClient lastWorld = null;

    // Sprint reset: force a fresh sprint press so sent attack resets sprint state.
    private boolean sprintResetPending = false;

    private boolean langSynced = false;

    private static final int BTN_LINCLIENT = 997;

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        // Count one frame per ALL overlay pass (fires exactly once per rendered frame).
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            ClientState.refreshFps();
            EngineHooks.onFrame();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!ClientState.modActive || !ClientState.hudEnabled) return;
        if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) return;
        Hud.render(mc, false);
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        // One motion-blur pass per frame, before the HUD is drawn on top.
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            MotionBlur.render(Minecraft.getMinecraft().displayWidth,
                    Minecraft.getMinecraft().displayHeight);
        }
        if (Modules.on("crosshair") && event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Reset session stats when the world changes (server -> singleplayer, disconnect, ...).
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            ClientState.ping = 0;
            ClientState.combo = 0;
            ClientState.comboExpire = 0;
            ClientState.reach = 0;
            ClientState.reachExpire = 0;
            ClientState.distance = 0;
            ClientState.hasPrev = false;
            com.lindedaoqwq.linclient.hooks.EngineHooks.clearCaches();
            com.lindedaoqwq.linclient.util.MotionBlur.reset();
        }

        // Sync the config language once from the game language.
        if (!langSynced) {
            langSynced = true;
            try {
                ModConfig.setLang(mc.gameSettings.language);
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
        tickModules(mc);
        tickDamageTracking(mc);
        updatePing(mc);
    }

    private void updatePing(Minecraft mc) {
        ClientState.ping = 0;
        if (mc.getConnection() != null && mc.player != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
            if (info != null && info.getResponseTime() > 0) ClientState.ping = info.getResponseTime();
        }
    }

    private void applyMovement(Minecraft mc) {
        EntityPlayerSP p = mc.player;
        if (p == null) return;
        // Toggle sprint: hold the sprint key while moving forward.
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        boolean wantSprint = Modules.on("togglesprint") && mc.currentScreen == null
                && mc.gameSettings.keyBindForward.isKeyDown()
                && !p.isSneaking() && !p.capabilities.isFlying;
        KeyBinding.setKeyBindState(sprint.getKeyCode(), wantSprint);

        // Sprint reset: re-press sprint next tick so a hit resets momentum (W-tap helper).
        if (sprintResetPending) {
            sprintResetPending = false;
            KeyBinding.setKeyBindState(sprint.getKeyCode(), false);
        }
    }

    private void tickModules(Minecraft mc) {
        EntityPlayerSP p = mc.player;
        if (p == null) return;
        if (!ClientState.hasPrev) {
            ClientState.prevX = p.posX;
            ClientState.prevY = p.posY;
            ClientState.prevZ = p.posZ;
            ClientState.hasPrev = true;
            return;
        }
        double dx = p.posX - ClientState.prevX;
        double dz = p.posZ - ClientState.prevZ;
        double dh = Math.sqrt(dx * dx + dz * dz);
        ClientState.distance += dh;
        // Per-tick horizontal delta -> blocks per second (20 ticks/s).
        ClientState.speed = dh * 20.0;
        ClientState.prevX = p.posX;
        ClientState.prevY = p.posY;
        ClientState.prevZ = p.posZ;
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
    }

    private void tickDamageTracking(Minecraft mc) {
        long nowMs = System.currentTimeMillis();
        // Combo resets after the configured timeout with no successful hit.
        if (ClientState.combo > 0 && nowMs > ClientState.comboExpire) {
            ClientState.combo = 0;
            ClientState.comboExpire = 0;
        }
        if (pendingTarget == null) return;
        if (!(pendingTarget instanceof EntityLivingBase) || ((Entity) pendingTarget).isDead) {
            pendingTarget = null;
            return;
        }
        float now = ((EntityLivingBase) pendingTarget).getHealth();
        if (now < pendingHealth - 0.01F) {
            ClientState.combo++;
            ClientState.comboExpire = nowMs + (long) (ModConfig.value("combo.time", 2F) * 1000F);
            ClientState.reachExpire = nowMs + (long) (ModConfig.value("reach.time", 2F) * 1000F);
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
        ClientState.reachExpire = System.currentTimeMillis() + (long) (ModConfig.value("reach.time", 2F) * 1000F);
        pendingTarget = event.getTarget();
        pendingHealth = ((EntityLivingBase) event.getTarget()).getHealth();
        if (Modules.on("sprintreset")) sprintResetPending = true;
    }

    /** Getting hit resets the combo counter and triggers the hit-colour flash. */
    @SubscribeEvent
    public void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && event.getEntity() == mc.player) {
            ClientState.combo = 0;
            ClientState.comboExpire = 0;
            ClientState.lastDamageTakenTime = System.currentTimeMillis();
        }
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
                float zoom = Math.max(1.5F, ModConfig.value("zoom.zoom", 5F));
                fovNewField.setFloat(event, fovSrcField.getFloat(event) / zoom);
            }
        } catch (Exception ignored) { }
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
        if (!Modules.on("blockoverlay")) return;
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
        GL11.glLineWidth(Math.max(1F, ModConfig.value("blockoverlay.width", 2F)));
        float[] rgb = argb(ModConfig.color("blockoverlay", 0xFF3BA9F0));
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
