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

    // Session
    public static long sessionStart = System.currentTimeMillis();   // playtime module

    // Input / clicks
    public static boolean leftHeld = false, rightHeld = false;
    public static final ArrayDeque<Long> leftClickTimes = new ArrayDeque<>();
    public static final ArrayDeque<Long> rightClickTimes = new ArrayDeque<>();

    // Damage taken (hitcolour screen flash)
    public static long lastDamageTakenTime = 0;

    // Combat
    public static int combo = 0;
    public static long comboExpire = 0;
    public static float reach = 0;            // last attack distance (blocks)
    public static long reachExpire = 0;       // when reach display expires
    public static int ping = 0;               // self latency, ms

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
        prune(leftClickTimes, System.currentTimeMillis());
        return leftClickTimes.size();
    }

    public static int rightCps() {
        prune(rightClickTimes, System.currentTimeMillis());
        return rightClickTimes.size();
    }

    private static void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst();
    }
}
