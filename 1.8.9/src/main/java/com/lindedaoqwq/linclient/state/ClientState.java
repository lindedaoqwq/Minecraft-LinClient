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

    // FPS is self-counted every rendered frame and computed once per second. This is reliable and
    // identical across versions (the old reflection read of debugFPS could get stuck at 1).
    public static int fps = 0;
    private static int frameCounter = 0;
    private static long fpsTimestamp = System.currentTimeMillis();

    /** Call once per rendered frame (from the HUD overlay event). Counts frames and reports the
     *  smoothed frames-per-second every ~1s. */
    public static void refreshFps() {
        frameCounter++;
        long now = System.currentTimeMillis();
        long elapsed = now - fpsTimestamp;
        if (elapsed >= 1000L) {
            fps = (int) (frameCounter * 1000L / elapsed);
            frameCounter = 0;
            fpsTimestamp = now;
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
