package com.lindedaoqwq.linclient.state;

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

    // Input
    public static boolean leftDown = false, rightDown = false;
    public static long lastInputTime = 0;
    public static final ArrayDeque<Long> leftClickTimes = new ArrayDeque<>();
    public static final ArrayDeque<Long> rightClickTimes = new ArrayDeque<>();
    public static double cpsLeft = 0, cpsRight = 0;

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

    public static void addLeftClick() {
        long now = System.currentTimeMillis();
        leftClickTimes.addLast(now);
        prune(leftClickTimes, now);
        cpsLeft = leftClickTimes.size();
    }

    public static void addRightClick() {
        long now = System.currentTimeMillis();
        rightClickTimes.addLast(now);
        prune(rightClickTimes, now);
        cpsRight = rightClickTimes.size();
    }

    private static void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst();
    }
}
