package com.lindedaoqwq.linclient.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Self-contained translation helper. Loads our own language files from the mod resources so the
 * UI language can be switched instantly at runtime (no Minecraft restart required).
 *
 * The active language is controlled by {@link #setLang(String)}; the persisted choice lives in
 * ModConfig.language. Falls back to English for any missing key.
 */
public class I18n {
    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> ZH = new HashMap<>();
    private static Map<String, String> active = ZH;
    private static String lang = "zh_CN";

    static {
        load(EN, "/assets/linclient/lang/en_US.lang");
        load(ZH, "/assets/linclient/lang/zh_CN.lang");
        setLang(lang);
    }

    private static void load(Map<String, String> map, String resource) {
        try (InputStream in = I18n.class.getResourceAsStream(resource)) {
            if (in == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx <= 0) continue;
                map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        } catch (Exception ignored) {
            // Missing or malformed language file: keep whatever we have.
        }
    }

    public static void setLang(String l) {
        lang = (l == null) ? "zh_CN" : l;
        active = "zh_CN".equals(lang) ? ZH : EN;
    }

    public static String getLang() {
        return lang;
    }

    public static String t(String key, Object... args) {
        String v = active.get(key);
        if (v == null) v = EN.get(key);
        if (v == null) return key;
        if (args != null && args.length > 0) {
            try {
                v = String.format(v, args);
            } catch (Exception ignored) {
                // Format mismatch: return the raw template.
            }
        }
        return v;
    }
}
