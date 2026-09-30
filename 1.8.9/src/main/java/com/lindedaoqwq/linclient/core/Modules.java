package com.lindedaoqwq.linclient.core;

import java.util.ArrayList;
import java.util.List;

/** Module registry with bilingual labels (zh shown when the game language is Chinese).
 *  Each module can declare real settings (sliders / colour) shown in the ClickGUI. */
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
        public final String id, cat, label, labelZh;
        public final List<Setting> settings = new ArrayList<Setting>();
        Def(String id, String cat, String label, String labelZh) {
            this.id = id; this.cat = cat; this.label = label; this.labelZh = labelZh;
        }

        /** Chain a setting onto this module definition. */
        public Def set(Setting s) { settings.add(s); return this; }
    }

    public static final String[] CATS = {"combat", "movement", "player", "render", "other"};

    /** Optimisation modules that are always on and hidden from the GUI. */
    public static final java.util.Set<String> FORCE_ON = new java.util.HashSet<String>(
            java.util.Arrays.asList("smartfps", "dynfps"));

    public static String catLabel(String cat, boolean zh) {
        if (zh) {
            if (cat.equals("combat")) return "\u6218\u6597";
            if (cat.equals("movement")) return "\u79fb\u52a8";
            if (cat.equals("player")) return "\u73a9\u5bb6";
            if (cat.equals("render")) return "\u6e32\u67d3";
            if (cat.equals("other")) return "\u5176\u4ed6";
            return "\u5ba2\u6237\u7aef";
        }
        return Character.toUpperCase(cat.charAt(0)) + cat.substring(1);
    }

    public static String label(Def d, boolean zh) { return zh ? d.labelZh : d.label; }

    public static final List<Def> ALL = new ArrayList<Def>();

    private static Def add(String id, String cat, String label, String labelZh) {
        Def d = new Def(id, cat, label, labelZh);
        ALL.add(d);
        return d;
    }

    static {
        add("targethud", "combat", "Target HUD", "\u76ee\u6807 HUD")
                .set(Setting.color("\u989c\u8272", "Colour", 0xFFE02F2F));
        add("combo", "combat", "Combo Counter", "\u8fde\u51fb\u8ba1\u6570")
                .set(Setting.slider("combo.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("reach", "combat", "Reach Display", "\u653b\u51fb\u8ddd\u79bb")
                .set(Setting.slider("reach.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("damage", "combat", "Damage Indicator", "\u4f24\u5bb3\u63d0\u793a")
                .set(Setting.slider("damage.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("nohurtcam", "combat", "No Hurt Cam", "\u53d7\u4f24\u4e0d\u6296\u52a8");
        add("sprint", "movement", "Toggle Sprint", "\u81ea\u52a8\u75be\u8dd1");
        add("freelook", "movement", "Freelook", "\u81ea\u7531\u89c6\u89d2");
        add("zoom", "movement", "Zoom", "\u5feb\u901f\u7f29\u653e")
                .set(Setting.slider("zoom.zoom", "\u7f29\u653e\u500d\u7387", "Zoom level", 2, 10, 1, 5));
        add("bobbing", "movement", "Minimal Bobbing", "\u89c6\u89d2\u6447\u6643\u6700\u5c0f\u5316");
        add("potions", "player", "Potion Status", "\u836f\u6c34\u6548\u679c");
        add("coords", "player", "Coordinates", "\u5750\u6807\u65b9\u4f4d");
        add("reconnect", "player", "Auto Reconnect", "\u81ea\u52a8\u91cd\u8fde")
                .set(Setting.slider("reconnect.delay", "\u91cd\u8fde\u5ef6\u8fdf(\u79d2)", "Reconnect delay (s)", 3, 10, 1, 5));
        add("keystrokes", "render", "Keystrokes", "\u6309\u952e\u663e\u793a")
                .set(Setting.color("\u9ad8\u4eae\u989c\u8272", "Highlight colour", 0xFF274053));
        add("cps", "render", "CPS Counter", "CPS \u8ba1\u6570");
        add("pingfps", "render", "Ping & FPS", "\u5ef6\u8fdf\u4e0e\u5e27\u7387");
        add("armor", "render", "Armor Status", "\u76d4\u7532\u72b6\u6001");
        add("crosshair", "render", "Custom Crosshair", "\u81ea\u5b9a\u4e49\u51c6\u661f")
                .set(Setting.color("\u989c\u8272", "Colour", 0xFF3AA6F0))
                .set(Setting.slider("crosshair.size", "\u51c6\u661f\u5927\u5c0f", "Crosshair size", 2, 8, 1, 4));
        add("fullbright", "render", "Fullbright", "\u5168\u4eae");
        add("blockoutline", "render", "Block Outline", "\u65b9\u5757\u63cf\u8fb9")
                .set(Setting.color("\u63cf\u8fb9\u989c\u8272", "Outline colour", 0xFF3BA9F0))
                .set(Setting.slider("blockoutline.width", "\u63cf\u8fb9\u5bbd\u5ea6", "Outline width", 1, 5, 1, 2));
        add("lowfire", "render", "Low Fire", "\u4f4e\u706b\u7130");
        add("particles", "render", "Particle Control", "\u7c92\u5b50\u63a7\u5236")
                .set(Setting.slider("particles.keep", "\u7c92\u5b50\u4fdd\u7559(%)", "Particles kept (%)", 0, 100, 5, 25));
        add("weather", "render", "No Weather", "\u65e0\u5929\u6c14");
        add("hud", "other", "HUD Master", "HUD \u603b\u5f00\u5173");
        // Optimisation modules are force-enabled (not listed in the GUI):
        // smartfps (unfocused throttle), dynfps (render distance guard).
    }

    public static Def byId(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return null;
    }

    public static boolean on(String id) {
        if (FORCE_ON.contains(id)) return true;
        return com.lindedaoqwq.linclient.config.ModConfig.on(id);
    }
}
