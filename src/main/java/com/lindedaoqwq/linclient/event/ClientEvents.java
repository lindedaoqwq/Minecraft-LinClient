package com.lindedaoqwq.linclient.event;

import com.lindedaoqwq.linclient.LinClient;
import com.lindedaoqwq.linclient.config.KeyBindings;
import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.hud.HudEditorScreen;
import com.lindedaoqwq.linclient.hud.HudOverlay;
import com.lindedaoqwq.linclient.hud.modules.CombatModule;
import com.lindedaoqwq.linclient.state.ClientState;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * All client-side Forge events are handled here. Nothing in this class (or anywhere in the mod)
 * reads hidden server data or changes server-side behaviour.
 */
public class ClientEvents {

    // Runtime master switches (independent of the config file).
    public static boolean modActive = true;
    public static boolean hudEnabled = true;

    // Visual-toggle bookkeeping.
    private boolean fullbrightActive = false;
    private double originalGamma = 0.5;
    private boolean cloudsOff = false;
    private CloudStatus origClouds = CloudStatus.FANCY;

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!modActive || !hudEnabled) return;
        if (mc.options.renderDebug && !ModConfig.SHOW_HUD_IN_DEBUG.get()) return;
        if (mc.screen != null) return; // a screen (incl. the editor) draws its own overlay
        HudOverlay.render(event.getGuiGraphics(), mc);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        // Key bindings (polled every tick so clicks don't latch).
        if (KeyBindings.TOGGLE_MOD.consumeClick()) modActive = !modActive;
        if (modActive && KeyBindings.TOGGLE_HUD.consumeClick()) hudEnabled = !hudEnabled;
        if (modActive && KeyBindings.OPEN_EDITOR.consumeClick() && mc.screen == null) {
            mc.setScreen(new HudEditorScreen());
        }

        if (mc.player == null || mc.level == null) return;

        updateClientState(mc);
        applyVisuals(mc);
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            ClientState.lastInputTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseButton event) {
        int action = event.getAction();
        int button = event.getButton();
        if (action == GLFW.GLFW_PRESS) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                ClientState.leftDown = true;
                ClientState.addClick(true);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                ClientState.rightDown = true;
                ClientState.addClick(false);
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) ClientState.leftDown = false;
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) ClientState.rightDown = false;
        }
        ClientState.lastInputTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onScreenEffect(RenderBlockScreenEffectEvent event) {
        RenderBlockScreenEffectEvent.OverlayType t = event.getOverlayType();
        if (t == RenderBlockScreenEffectEvent.OverlayType.FIRE && ModConfig.DISABLE_FIRE_OVERLAY.get()) event.setCanceled(true);
        if (t == RenderBlockScreenEffectEvent.OverlayType.WATER && ModConfig.DISABLE_WATER_OVERLAY.get()) event.setCanceled(true);
    }

    private void updateClientState(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        long now = System.currentTimeMillis();

        Vec3 pos = p.position();
        if (ClientState.prevPos != null) {
            double dx = pos.x - ClientState.prevPos.x;
            double dz = pos.z - ClientState.prevPos.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            ClientState.speed = d * 20.0;
            ClientState.distance += d;
            if (d > 0.001) ClientState.lastInputTime = now;
        }
        ClientState.prevPos = pos;

        float hp = p.getHealth() + p.getAbsorptionAmount();
        if (ClientState.lastPlayerHealth > 0.0f && hp < ClientState.lastPlayerHealth - 0.01f) {
            ClientState.lastDamageTaken = ClientState.lastPlayerHealth - hp;
            ClientState.lastDamageTakenTime = now;
        }
        ClientState.lastPlayerHealth = hp;

        if (p.isDeadOrDying()) {
            if (!ClientState.wasDead) {
                ResourceLocation dim = mc.level.dimension().location();
                BlockPos bp = p.blockPosition();
                ClientState.recordDeath(dim.toString(), bp.getX(), bp.getY(), bp.getZ());
            }
            ClientState.wasDead = true;
        } else {
            if (ClientState.wasDead) ClientState.lastPlayerHealth = p.getHealth() + p.getAbsorptionAmount();
            ClientState.wasDead = false;
        }

        LivingEntity t = CombatModule.findTarget(mc);
        if (t != null) {
            float thp = t.getHealth();
            if (ClientState.lastTarget == t && ClientState.lastTargetHealth > thp + 0.01f) {
                ClientState.lastDamageDealt = ClientState.lastTargetHealth - thp;
                ClientState.lastDamageDealtTime = now;
                ClientState.combo++;
                ClientState.comboExpire = now + 2000L;
            }
            ClientState.lastTarget = t;
            ClientState.lastTargetHealth = thp;
        } else {
            ClientState.lastTarget = null;
        }
        if (now > ClientState.comboExpire) ClientState.combo = 0;
    }

    private void applyVisuals(Minecraft mc) {
        // Fullbright (gamma)
        if (ModConfig.FULLBRIGHT.get()) {
            if (!fullbrightActive) {
                originalGamma = mc.options.gamma().get();
                fullbrightActive = true;
            }
            mc.options.gamma().set(1.0);
        } else if (fullbrightActive) {
            mc.options.gamma().set(originalGamma);
            fullbrightActive = false;
        }

        // Clouds (renderClouds is an OptionInstance<CloudStatus> field in 1.20.1)
        if (ModConfig.DISABLE_CLOUDS.get()) {
            if (!cloudsOff) {
                origClouds = mc.options.renderClouds.get();
                cloudsOff = true;
            }
            mc.options.renderClouds.set(CloudStatus.OFF);
        } else if (cloudsOff) {
            mc.options.renderClouds.set(origClouds);
            cloudsOff = false;
        }

        // Weather (client-side only)
        if (ModConfig.DISABLE_WEATHER.get() && mc.level != null) {
            mc.level.setRainLevel(0.0f);
            mc.level.setThunderLevel(0.0f);
        }

        // Dynamic FPS: auto-adjust render distance. Render distance is a client setting only.
        if (ModConfig.DYNAMIC_FPS.get() && mc.level != null) {
            int fps = mc.getFps();
            int cur = mc.options.renderDistance().get();
            if (fps < ModConfig.DYNAMIC_FPS_MIN.get() && cur > ModConfig.DYNAMIC_FPS_MIN_DIST.get()) {
                mc.options.renderDistance().set(ModConfig.DYNAMIC_FPS_MIN_DIST.get());
            } else if (fps > ModConfig.DYNAMIC_FPS_RESTORE.get() && cur < ModConfig.DYNAMIC_FPS_MAX_DIST.get()) {
                mc.options.renderDistance().set(ModConfig.DYNAMIC_FPS_MAX_DIST.get());
            }
        }
    }
}
