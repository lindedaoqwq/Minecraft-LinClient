package com.lindedaoqwq.linclient.state;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared, transient client-side runtime state. Updated every client tick by {@code ClientEvents}
 * and read by the HUD modules. None of this touches the server.
 */
public final class ClientState {
    private ClientState() {}

    // ---- Input / CPS ----
    public static long lastInputTime = System.currentTimeMillis();
    public static boolean leftDown = false;
    public static boolean rightDown = false;
    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();

    public static void addClick(boolean left) {
        (left ? leftClicks : rightClicks).add(System.currentTimeMillis());
        lastInputTime = System.currentTimeMillis();
    }

    public static int getCps(boolean left) {
        long now = System.currentTimeMillis();
        List<Long> list = left ? leftClicks : rightClicks;
        list.removeIf(t -> now - t > 1000);
        return list.size();
    }

    // ---- Movement ----
    public static double speed = 0.0;       // blocks / second (horizontal)
    public static double distance = 0.0;    // total horizontal blocks travelled
    public static net.minecraft.world.phys.Vec3 prevPos = null;

    // ---- Combat ----
    public static int combo = 0;
    public static long comboExpire = 0;
    public static float lastDamageDealt = 0.0f;
    public static long lastDamageDealtTime = 0;
    public static float lastDamageTaken = 0.0f;
    public static long lastDamageTakenTime = 0;
    public static LivingEntity lastTarget = null;
    public static float lastTargetHealth = 0.0f;
    public static float lastPlayerHealth = 0.0f; // includes absorption
    public static boolean wasDead = false;

    // ---- Death coordinates ----
    public static final class DeathRecord {
        public final String dimension;
        public final int x, y, z;
        public final long time;

        public DeathRecord(String dimension, int x, int y, int z, long time) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    public static final List<DeathRecord> deaths = new ArrayList<>();
    private static final int MAX_DEATHS = 5;

    public static void recordDeath(String dimension, int x, int y, int z) {
        deaths.add(0, new DeathRecord(dimension, x, y, z, System.currentTimeMillis()));
        while (deaths.size() > MAX_DEATHS) deaths.remove(deaths.size() - 1);
    }
}
