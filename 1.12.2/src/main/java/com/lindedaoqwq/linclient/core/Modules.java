package com.lindedaoqwq.linclient.core;

import java.util.ArrayList;
import java.util.List;

/** Module registry with bilingual labels (zh shown when the game language is Chinese). */
public class Modules {
    public static class Def {
        public final String id, cat, label, labelZh;
        Def(String id, String cat, String label, String labelZh) {
            this.id = id; this.cat = cat; this.label = label; this.labelZh = labelZh;
        }
    }

    public static final String[] CATS = {"combat", "movement", "player", "render", "other", "client"};

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

    private static void add(String id, String cat, String label, String labelZh) {
        ALL.add(new Def(id, cat, label, labelZh));
    }

    static {
        add("targethud", "combat", "Target HUD", "\u76ee\u6807 HUD");
        add("combo", "combat", "Combo Counter", "\u8fde\u51fb\u8ba1\u6570");
        add("reach", "combat", "Reach Display", "\u653b\u51fb\u8ddd\u79bb");
        add("damage", "combat", "Damage Indicator", "\u4f24\u5bb3\u63d0\u793a");
        add("nohurtcam", "combat", "No Hurt Cam", "\u53d7\u4f24\u4e0d\u6296\u52a8");
        add("sprint", "movement", "Toggle Sprint", "\u81ea\u52a8\u75be\u8dd1");
        add("freelook", "movement", "Freelook", "\u81ea\u7531\u89c6\u89d2");
        add("zoom", "movement", "Zoom", "\u5feb\u901f\u7f29\u653e");
        add("bobbing", "movement", "Minimal Bobbing", "\u89c6\u89d2\u6447\u6643\u6700\u5c0f\u5316");
        add("potions", "player", "Potion Status", "\u836f\u6c34\u6548\u679c");
        add("coords", "player", "Coordinates", "\u5750\u6807\u65b9\u4f4d");
        add("reconnect", "player", "Auto Reconnect", "\u81ea\u52a8\u91cd\u8fde");
        add("keystrokes", "render", "Keystrokes", "\u6309\u952e\u663e\u793a");
        add("cps", "render", "CPS Counter", "CPS \u8ba1\u6570");
        add("pingfps", "render", "Ping & FPS", "\u5ef6\u8fdf\u4e0e\u5e27\u7387");
        add("armor", "render", "Armor Status", "\u76d4\u7532\u72b6\u6001");
        add("crosshair", "render", "Custom Crosshair", "\u81ea\u5b9a\u4e49\u51c6\u661f");
        add("fullbright", "render", "Fullbright", "\u5168\u4eae");
        add("blockoutline", "render", "Block Outline", "\u65b9\u5757\u63cf\u8fb9");
        add("lowfire", "render", "Low Fire", "\u4f4e\u706b\u7130");
        add("particles", "render", "Particle Control", "\u7c92\u5b50\u63a7\u5236");
        add("weather", "render", "No Weather", "\u65e0\u5929\u6c14");
        add("hud", "other", "HUD Master", "HUD \u603b\u5f00\u5173");
        add("smartfps", "client", "Smart FPS (unfocused)", "\u667a\u80fd\u5e27\u7387\uff08\u5931\u7126\uff09");
        add("dynfps", "client", "Dynamic FPS", "\u52a8\u6001\u5e27\u7387");
    }

    public static Def byId(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return null;
    }

    public static boolean on(String id) { return com.lindedaoqwq.linclient.config.ModConfig.on(id); }
}
