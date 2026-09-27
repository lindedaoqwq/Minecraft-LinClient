package com.lindedaoqwq.linclient.hud.modules;

import com.lindedaoqwq.linclient.hud.HudModule;
import com.lindedaoqwq.linclient.util.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lists active bosses (from the vanilla boss overlay) with a health percentage.
 *
 * Note: Mojang's {@link BossHealthOverlay} does not expose the live boss map through a public
 * API, so we read its {@code events} field reflectively. This is client-side only and does not
 * touch the server. No Mixin / coremod is used.
 */
public class BossModule extends HudModule {
    public BossModule() {
        super("boss", "linclient.module.boss");
    }

    private static final Field EVENTS_FIELD;
    static {
        Field f = null;
        try {
            f = BossHealthOverlay.class.getDeclaredField("events");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            f = null;
        }
        EVENTS_FIELD = f;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, BossEvent> getEvents(BossHealthOverlay overlay) {
        if (EVENTS_FIELD == null) return Collections.emptyMap();
        try {
            Object v = EVENTS_FIELD.get(overlay);
            if (v instanceof Map) return (Map<UUID, BossEvent>) v;
        } catch (IllegalAccessException ignored) {
            // fall through
        }
        return Collections.emptyMap();
    }

    @Override
    public List<String> getLines(Minecraft mc) {
        List<String> lines = lines();
        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        if (overlay == null) return lines;

        for (Map.Entry<UUID, BossEvent> entry : getEvents(overlay).entrySet()) {
            BossEvent e = entry.getValue();
            lines.add(I18n.t("linclient.boss.name", e.getName().getString()));
            lines.add(I18n.t("linclient.boss.hp", (int) (e.getProgress() * 100.0f)));
        }
        return lines;
    }
}
