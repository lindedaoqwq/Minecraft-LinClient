package com.lindedaoqwq.linclient.core;

import java.util.ArrayList;
import java.util.List;

/** Module registry with bilingual labels + one-line descriptions.
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
        public final String id, cat, label, labelZh, desc, descZh;
        public final List<Setting> settings = new ArrayList<Setting>();
        Def(String id, String cat, String label, String labelZh, String desc, String descZh) {
            this.id = id; this.cat = cat; this.label = label; this.labelZh = labelZh;
            this.desc = desc; this.descZh = descZh;
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
    public static String desc(Def d, boolean zh) { return zh ? d.descZh : d.desc; }

    public static final List<Def> ALL = new ArrayList<Def>();

    private static Def add(String id, String cat, String label, String labelZh,
                           String desc, String descZh) {
        Def d = new Def(id, cat, label, labelZh, desc, descZh);
        ALL.add(d);
        return d;
    }

    static {
        add("targethud", "combat", "Target HUD", "\u76ee\u6807 HUD",
                "\u663e\u793a\u6240\u7784\u51c6\u76ee\u6807\u7684\u8840\u91cf", "Health of the entity you look at")
                .set(Setting.color("\u989c\u8272", "Colour", 0xFFE02F2F));
        add("combo", "combat", "Combo Counter", "\u8fde\u51fb\u8ba1\u6570",
                "\u7edf\u8ba1\u8fde\u7eed\u547d\u4e2d\u6b21\u6570", "Counts consecutive hits")
                .set(Setting.slider("combo.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("reach", "combat", "Reach Display", "\u653b\u51fb\u8ddd\u79bb",
                "\u663e\u793a\u4e0a\u6b21\u653b\u51fb\u7684\u8ddd\u79bb", "Last attack distance")
                .set(Setting.slider("reach.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("damage", "combat", "Damage Indicator", "\u4f24\u5bb3\u63d0\u793a",
                "\u663e\u793a\u5bf9\u76ee\u6807\u9020\u6210\u7684\u4f24\u5bb3", "Damage dealt to your target")
                .set(Setting.slider("damage.time", "\u663e\u793a\u65f6\u957f(\u79d2)", "Show time (s)", 1, 5, 1, 2));
        add("nohurtcam", "combat", "No Hurt Cam", "\u53d7\u4f24\u4e0d\u6296\u52a8",
                "\u53d7\u4f24\u65f6\u955c\u5934\u4e0d\u518d\u6447\u6643", "No screen shake when hurt");
        add("sprint", "movement", "Toggle Sprint", "\u81ea\u52a8\u75be\u8dd1",
                "\u524d\u8fdb\u65f6\u81ea\u52a8\u75be\u8dd1", "Sprint automatically when moving");
        add("freelook", "movement", "Freelook", "\u81ea\u7531\u89c6\u89d2",
                "\u6309\u4f4f\u5feb\u6377\u952e\u81ea\u7531\u73af\u987e", "Look around freely (hold key)");
        add("zoom", "movement", "Zoom", "\u5feb\u901f\u7f29\u653e",
                "\u6309\u4f4f\u5feb\u6377\u952e\u7f29\u5c0f\u89c6\u91ce", "Hold a key to zoom out")
                .set(Setting.slider("zoom.zoom", "\u7f29\u653e\u500d\u7387", "Zoom level", 2, 10, 1, 5));
        add("bobbing", "movement", "Minimal Bobbing", "\u89c6\u89d2\u6447\u6643\u6700\u5c0f\u5316",
                "\u51cf\u5c11\u884c\u8d70\u65f6\u7684\u89c6\u89d2\u6447\u6643", "Reduces view bobbing");
        add("potions", "player", "Potion Status", "\u836f\u6c34\u6548\u679c",
                "\u663e\u793a\u8eab\u4e0a\u7684\u836f\u6c34\u6548\u679c\u4e0e\u5269\u4f59\u65f6\u95f4", "Active potion effects & timers");
        add("coords", "player", "Coordinates", "\u5750\u6807\u65b9\u4f4d",
                "\u663e\u793a\u5750\u6807\u4e0e\u671d\u5411", "Position and facing direction");
        add("reconnect", "player", "Auto Reconnect", "\u81ea\u52a8\u91cd\u8fde",
                "\u6389\u7ebf\u540e\u81ea\u52a8\u91cd\u8fde\u670d\u52a1\u5668", "Reconnect after disconnect")
                .set(Setting.slider("reconnect.delay", "\u91cd\u8fde\u5ef6\u8fdf(\u79d2)", "Reconnect delay (s)", 3, 10, 1, 5));
        add("keystrokes", "render", "Keystrokes", "\u6309\u952e\u663e\u793a",
                "\u663e\u793a WASD \u4e0e\u9f20\u6807\u6309\u952e\u72b6\u6001", "WASD & mouse buttons display")
                .set(Setting.color("\u9ad8\u4eae\u989c\u8272", "Highlight colour", 0xFF274053));
        add("cps", "render", "CPS Counter", "CPS \u8ba1\u6570",
                "\u7edf\u8ba1\u6bcf\u79d2\u5de6\u53f3\u952e\u70b9\u51fb\u6570", "Clicks per second (LMB/RMB)");
        add("pingfps", "render", "Ping & FPS", "\u5ef6\u8fdf\u4e0e\u5e27\u7387",
                "\u663e\u793a\u5f53\u524d\u5ef6\u8fdf\u4e0e\u5e27\u7387", "Latency and frames per second");
        add("armor", "render", "Armor Status", "\u76d4\u7532\u72b6\u6001",
                "\u663e\u793a\u7a7f\u6234\u62a4\u7532\u4e0e\u8010\u4e45\u5ea6", "Worn armor and durability");
        add("crosshair", "render", "Custom Crosshair", "\u81ea\u5b9a\u4e49\u51c6\u661f",
                "\u66ff\u6362\u51c6\u661f\uff0c\u53ef\u8c03\u989c\u8272\u5927\u5c0f", "Custom crosshair, colour & size")
                .set(Setting.color("\u989c\u8272", "Colour", 0xFF3AA6F0))
                .set(Setting.slider("crosshair.size", "\u51c6\u661f\u5927\u5c0f", "Crosshair size", 2, 8, 1, 4));
        add("fullbright", "render", "Fullbright", "\u5168\u4eae",
                "\u70b9\u4eae\u9ed1\u6697\u89d2\u843d\uff0c\u65e0\u9700\u706b\u628a", "See in the dark");
        add("blockoutline", "render", "Block Outline", "\u65b9\u5757\u63cf\u8fb9",
                "\u81ea\u5b9a\u4e49\u65b9\u5757\u9009\u4e2d\u63cf\u8fb9", "Custom block selection outline")
                .set(Setting.color("\u63cf\u8fb9\u989c\u8272", "Outline colour", 0xFF3BA9F0))
                .set(Setting.slider("blockoutline.width", "\u63cf\u8fb9\u5bbd\u5ea6", "Outline width", 1, 5, 1, 2));
        add("lowfire", "render", "Low Fire", "\u4f4e\u706b\u7130",
                "\u964d\u4f4e\u706b\u7130\u5bf9\u89c6\u7ebf\u7684\u906e\u6321", "Less fire blocking the view");
        add("particles", "render", "Particle Control", "\u7c92\u5b50\u63a7\u5236",
                "\u51cf\u5c11\u7c92\u5b50\u63d0\u5347\u5e27\u7387", "Fewer particles, more FPS")
                .set(Setting.slider("particles.keep", "\u7c92\u5b50\u4fdd\u7559(%)", "Particles kept (%)", 0, 100, 5, 25));
        add("weather", "render", "No Weather", "\u65e0\u5929\u6c14",
                "\u4e0d\u518d\u4e0b\u96e8\u4e0b\u96ea", "No rain or snow");
        add("signtext", "render", "Sign Text Culling", "\u544a\u793a\u724c\u6587\u5b57\u5254\u9664",
                "\u8fdc\u5904\u770b\u4e0d\u6e05\u7684\u544a\u793a\u724c\u6587\u5b57\u4e0d\u518d\u7ed8\u5236",
                "Skip sign text too far away to read")
                .set(Setting.slider("signtext.dist", "\u6700\u8fdc\u8ddd\u79bb(\u683c)",
                        "Max distance (blocks)", 16, 96, 8, 0));
        add("armorcache", "render", "Armor Texture Cache", "\u76d4\u7532\u8d34\u56fe\u7f13\u5b58",
                "\u7f13\u5b58\u76d4\u7532\u8d34\u56fe\u8def\u5f84\uff0c\u51cf\u5c11\u6bcf\u5e27\u5b57\u7b26\u4e32\u5f00\u9500",
                "Memoise armour texture paths");
        add("motionblur", "render", "Motion Blur", "\u52a8\u6001\u6a21\u7cca",
                "\u6e38\u6208\u5185\u8fd0\u52a8\u6a21\u7cca\uff0c\u5feb\u901f\u79fb\u52a8\u65f6\u753b\u9762\u4ea7\u751f\u62d6\u5f71",
                "In-game motion blur / trailing on movement")
                .set(Setting.slider("motionblur.amount", "\u6a21\u7cca\u5f3a\u5ea6", "Blur amount", 0.1F, 0.85F, 0.05F, 0.5F));
        add("hud", "other", "HUD Master", "HUD \u603b\u5f00\u5173",
                "\u6240\u6709 HUD \u5143\u7d20\u7684\u603b\u5f00\u5173", "Master toggle for all HUD items");
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
