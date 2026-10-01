package com.lindedaoqwq.linclient.core;

import java.util.ArrayList;
import java.util.List;

/** Module registry with bilingual labels + one-line descriptions.
 *  Each module can declare real settings (sliders / colour) shown in the ClickGUI.
 *
 *  The module set is deliberately small and focused: every entry is a pure client-side
 *  feature that fits the JSON config + live-settings ClickGUI. Server-dependent, social
 *  and cosmetic ideas are intentionally left out. */
public class Modules {
    /** A single configurable value. Sliders have a numeric range; colours map to ModConfig. */
    public static class Setting {
        public final String id, label, labelZh;
        public final float min, max, step, def;
        public final boolean isColor;
        public final int colorDef;   // default colour shown before the user picks one

        private Setting(String id, String label, String labelZh,
                        float min, float max, float step, float def, boolean isColor, int colorDef) {
            this.id = id; this.label = label; this.labelZh = labelZh;
            this.min = min; this.max = max; this.step = step; this.def = def;
            this.isColor = isColor; this.colorDef = colorDef;
        }

        public static Setting slider(String id, String label, String labelZh,
                                     float min, float max, float step, float def) {
            return new Setting(id, label, labelZh, min, max, step, def, false, 0);
        }

        public static Setting color(String label, String labelZh, int colorDef) {
            return new Setting("color", label, labelZh, 0, 0, 0, 0, true, colorDef);
        }

        public String text(boolean zh) { return zh ? labelZh : label; }
    }

    public static class Def {
        public final String id, cat, label, labelZh, desc, descZh;
        public final List<Setting> settings = new ArrayList<Setting>();
        Def(String id, String cat, String label, String labelZh, String desc, String descZh) {
            this.id = id; this.cat = cat; this.label = label; this.labelZh = labelZh;
            this.desc = desc; this.descZh = descZh;
        }

        /** Chain a setting onto this module definition. */
        public Def set(Setting s) { settings.add(s); return this; }
    }

    public static final String[] CATS = {"hud", "combat", "movement", "player", "render"};

    public static String catLabel(String cat, boolean zh) {
        if (zh) {
            if (cat.equals("hud")) return "HUD";
            if (cat.equals("combat")) return "\u6218\u6597";
            if (cat.equals("movement")) return "\u79fb\u52a8";
            if (cat.equals("player")) return "\u73a9\u5bb6";
            if (cat.equals("render")) return "\u6e32\u67d3";
            return "\u5ba2\u6237\u7aef";
        }
        if (cat.equals("hud")) return "HUD";
        return Character.toUpperCase(cat.charAt(0)) + cat.substring(1);
    }

    public static String label(Def d, boolean zh) { return zh ? d.labelZh : d.label; }
    public static String desc(Def d, boolean zh) { return zh ? d.descZh : d.desc; }

    public static final List<Def> ALL = new ArrayList<Def>();

    private static Def add(String id, String cat, String label, String labelZh,
                           String desc, String descZh) {
        Def d = new Def(id, cat, label, labelZh, desc, descZh);
        ALL.add(d);
        return d;
    }

    static {
        // ---------------- HUD information ----------------
        add("fps", "hud", "FPS", "\u5e27\u7387",
                "\u5b9e\u65f6\u5e27\u7387", "Live frames per second");
        add("cps", "hud", "CPS", "\u70b9\u51fb\u901f\u5ea6",
                "\u5de6\u53f3\u952e\u6bcf\u79d2\u70b9\u51fb\u6570", "Left / right clicks per second");
        add("ping", "hud", "Ping", "\u5ef6\u8fdf",
                "\u5f53\u524d\u670d\u52a1\u5668\u5ef6\u8fdf(ms)", "Current server latency (ms)");
        add("memory", "hud", "Memory", "\u5185\u5b58",
                "\u5df2\u7528\u4e0e\u6700\u5927\u5806\u5185\u5b58", "Used / max heap memory")
                .set(Setting.slider("memory.warn", "\u8b66\u793a\u9608\u503c(%)", "Warn at (%)", 50, 95, 5, 85));
        add("coordinates", "hud", "Coordinates", "\u5750\u6807",
                "\u5750\u6807 XYZ \u4e0e\u671d\u5411", "Position XYZ and facing");
        add("direction", "hud", "Direction", "\u65b9\u4f4d",
                "\u7f57\u76d8\u65b9\u4f4d\u663e\u793a", "Compass direction");
        add("speedometer", "hud", "Speedometer", "\u901f\u5ea6",
                "\u6c34\u5e73\u79fb\u52a8\u901f\u5ea6(\u683c/\u79d2)", "Horizontal speed (blocks/s)")
                .set(Setting.slider("speedometer.scale", "\u500d\u7387", "Scale", 1, 5, 1, 1));
        add("playtime", "hud", "Playtime", "\u6e38\u620f\u65f6\u957f",
                "\u672c\u6b21\u4f1a\u8bdd\u6e38\u620f\u65f6\u957f", "Session play time");
        add("serveraddress", "hud", "Server Address", "\u670d\u52a1\u5668\u5730\u5740",
                "\u5f53\u524d\u8fde\u63a5\u7684\u670d\u52a1\u5668\u5730\u5740", "Address of the server you are on");
        add("keystrokes", "hud", "Keystrokes", "\u6309\u952e\u663e\u793a",
                "\u663e\u793a WASD \u4e0e\u9f20\u6807\u6309\u952e\u72b6\u6001", "WASD & mouse buttons display")
                .set(Setting.color("\u9ad8\u4eae\u989c\u8272", "Highlight colour", 0xFF274053));

        // ---------------- combat / movement ----------------
        add("togglesprint", "movement", "Toggle Sprint", "\u81ea\u52a8\u75be\u8dd1",
                "\u524d\u8fdb\u65f6\u81ea\u52a8\u75be\u8dd1", "Sprint automatically when moving");
        add("sprintreset", "combat", "Sprint Reset", "\u75be\u8dd1\u91cd\u7f6e",
                "\u653b\u51fb\u540e\u91cd\u7f6e\u75be\u8dd1\u72b6\u6001", "Reset sprint on attack (W-tap helper)");
        add("combo", "combat", "Combo Counter", "\u8fde\u51fb\u8ba1\u6570",
                "\u7edf\u8ba1\u8fde\u7eed\u547d\u4e2d\u6b21\u6570", "Counts consecutive hits")
                .set(Setting.slider("combo.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("reach", "combat", "Reach Display", "\u653b\u51fb\u8ddd\u79bb",
                "\u663e\u793a\u4e0a\u6b21\u653b\u51fb\u7684\u8ddd\u79bb", "Last attack distance")
                .set(Setting.slider("reach.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("potion", "player", "Potion Effects", "\u836f\u6c34\u6548\u679c",
                "\u663e\u793a\u8eab\u4e0a\u7684\u836f\u6c34\u6548\u679c\u4e0e\u5269\u4f59\u65f6\u95f4",
                "Active potion effects & timers");

        // ---------------- render ----------------
        add("fullbright", "render", "Fullbright", "\u5168\u4eae",
                "\u70b9\u4eae\u9ed1\u6697\u89d2\u843d\uff0c\u65e0\u9700\u706b\u628a", "See in the dark");
        add("crosshair", "render", "Custom Crosshair", "\u81ea\u5b9a\u4e49\u51c6\u661f",
                "\u66ff\u6362\u51c6\u661f\uff0c\u53ef\u8c03\u989c\u8272\u5927\u5c0f", "Custom crosshair, colour & size")
                .set(Setting.color("\u989c\u8272", "Colour", 0xFF3AA6F0))
                .set(Setting.slider("crosshair.size", "\u51c6\u661f\u5927\u5c0f", "Crosshair size", 2, 8, 1, 4));
        add("blockoverlay", "render", "Block Overlay", "\u65b9\u5757\u63cf\u8fb9",
                "\u81ea\u5b9a\u4e49\u65b9\u5757\u9009\u4e2d\u63cf\u8fb9", "Custom block selection outline")
                .set(Setting.color("\u63cf\u8fb9\u989c\u8272", "Outline colour", 0xFF3BA9F0))
                .set(Setting.slider("blockoverlay.width", "\u63cf\u8fb9\u5bbd\u5ea6", "Outline width", 1, 5, 1, 2));
        add("hitcolor", "render", "Hit Colour", "\u53d7\u51fb\u67d3\u8272",
                "\u53d7\u51fb\u65f6\u5c4f\u5e55\u8fb9\u7f18\u6cdb\u7ea2", "Red screen flash when you take damage")
                .set(Setting.color("\u989c\u8272", "Colour", 0xFFE02F2F))
                .set(Setting.slider("hitcolor.alpha", "\u5f3a\u5ea6", "Strength", 10, 100, 5, 45));
        add("motionblur", "render", "Motion Blur", "\u52a8\u6001\u6a21\u7cca",
                "\u6e38\u620f\u5185\u8fd0\u52a8\u6a21\u7cca\uff0c\u5feb\u901f\u79fb\u52a8\u65f6\u753b\u9762\u4ea7\u751f\u62d6\u5f71",
                "In-game motion blur / trailing on movement")
                .set(Setting.slider("motionblur.amount", "\u6a21\u7cca\u5f3a\u5ea6", "Blur amount", 0.1F, 0.85F, 0.05F, 0.5F));
        add("zoom", "render", "Zoom", "\u5feb\u901f\u7f29\u653e",
                "\u6309\u4f4f\u5feb\u6377\u952e\u7f29\u5c0f\u89c6\u91ce", "Hold a key to zoom in")
                .set(Setting.slider("zoom.zoom", "\u7f29\u653e\u500d\u7387", "Zoom level", 2, 10, 1, 5));
    }

    public static Def byId(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return null;
    }

    public static boolean on(String id) {
        return com.lindedaoqwq.linclient.config.ModConfig.on(id);
    }
}
