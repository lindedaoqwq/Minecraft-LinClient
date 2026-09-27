package com.lindedaoqwq.linclient.state;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

/**
 * Runtime, non-persistent client state used by the HUD modules and the event handler.
 * Nothing here is sent to or read from the server.
 */
public class ClientState {
    public static boolean modActive = true;
    public static boolean hudEnabled = true;
    public static boolean hudEditMode = false;

    // Movement
    public static double speed = 0.0;        // blocks / second
    public static double distance = 0.0;     // total horizontal distance travelled
    public static double prevX = 0, prevY = 0, prevZ = 0;
    public static boolean hasPrev = false;

    // Input / clicks
    public static boolean leftHeld = false, rightHeld = false;
    public static final ArrayDeque<Long> leftClickTimes = new ArrayDeque<>();
    public static final ArrayDeque<Long> rightClickTimes = new ArrayDeque<>();

    // Health / damage
    public static float lastPlayerHealth = 0;
    public static boolean wasDead = false;
    public static float lastDamageTaken = 0;
    public static long lastDamageTakenTime = 0;

    // Death
    public static long lastDeathTime = 0;
    public static String lastDeathDim = "";
    public static int lastDeathX = 0, lastDeathY = 0, lastDeathZ = 0;

    // Combat
    public static int combo = 0;
    public static long comboExpire = 0;
    public static float lastDamageDealt = 0;
    public static long lastDamageDealtTime = 0;

    // FPS: read from Minecraft's own debugFPS field (identical to the F3 counter) via reflection,
    // with a sliding-window fallback if reflection ever fails.
    public static int fps = 0;
    private static Field debugFpsField = null;
    private static boolean fpsReflectionReady = false;
    private static int frameCounter = 0;
    private static long fpsTimestamp = System.currentTimeMillis();

    // HUD drag state
    public static String draggingId = null;
    public static int dragOffsetX = 0, dragOffsetY = 0;

    public static void refreshFps(Minecraft mc) {
        if (!fpsReflectionReady) {
            try {
                Field f = mc.getClass().getDeclaredField("debugFPS");
                f.setAccessible(true);
                debugFpsField = f;
                fpsReflectionReady = true;
            } catch (Exception e) {
                fpsReflectionReady = false;
            }
        }
        if (fpsReflectionReady && debugFpsField != null) {
            try {
                fps = ((Integer) debugFpsField.get(mc)).intValue();
            } catch (Exception e) {
                fps = 0;
            }
        }
        if (fps <= 0) {
            frameCounter++;
            long now = System.currentTimeMillis();
            if (now - fpsTimestamp >= 1000L) {
                fps = frameCounter;
                frameCounter = 0;
                fpsTimestamp = now;
            }
        }
    }

    public static void addLeftClick() {
        long now = System.currentTimeMillis();
        leftClickTimes.addLast(now);
        prune(leftClickTimes, now);
    }

    public static void addRightClick() {
        long now = System.currentTimeMillis();
        rightClickTimes.addLast(now);
        prune(rightClickTimes, now);
    }

    public static int leftCps() {
        return leftClickTimes.size();
    }

    public static int rightCps() {
        return rightClickTimes.size();
    }

    private static void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst();
    }
}
