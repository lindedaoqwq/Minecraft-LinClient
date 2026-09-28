package com.lindedaoqwq.linclient.core;

import com.lindedaoqwq.linclient.config.ModConfig;
import java.util.ArrayList;
import java.util.List;

/** Module registry. Labels are shown as-is (English, like mainstream clients). */
public class Modules {
    public static class Def {
        public final String id, cat, label;
        Def(String id, String cat) { this.id = id; this.cat = cat; this.label = id; }
    }

    public static final String[] CATS = {"combat", "movement", "player", "render", "other", "client"};
    public static final List<Def> ALL = new ArrayList<Def>();

    private static Def add(String id, String cat) {
        Def d = new Def(id, cat);
        ALL.add(d);
        return d;
    }

    static {
        add("combo", "combat");
        add("reach", "combat");
        add("targethud", "combat");
        add("damage", "combat");
        add("nohurtcam", "combat");
        add("sprint", "movement");
        add("sneak", "movement");
        add("freelook", "movement");
        add("zoom", "movement");
        add("bobbing", "movement");
        add("potions", "player");
        add("coords", "player");
        add("totem", "player");
        add("reconnect", "player");
        add("keystrokes", "render");
        add("cps", "render");
        add("pingfps", "render");
        add("armor", "render");
        add("crosshair", "render");
        add("fullbright", "render");
        add("blockoutline", "render");
        add("lowfire", "render");
        add("particles", "render");
        add("weather", "render");
        add("hud", "other");
        add("fastgraphics", "client");
        add("smoothlight", "client");
        add("smartfps", "client");
        add("dynfps", "client");
        add("clouds", "client");
    }

    public static boolean on(String id) { return ModConfig.on(id); }
}
