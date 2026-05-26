package org.fentanylsolutions.vrap;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class VrapConfig {

    private static final String CATEGORY_GENERAL = "general";
    private static final String KEY_TOKEN_URL = "curseForgeTokenUrl";
    private static final String DEFAULT_TOKEN_URL = "https://cf.polymc.org/api";

    private static Configuration configuration;
    private static String curseForgeTokenUrl = DEFAULT_TOKEN_URL;

    private VrapConfig() {}

    public static synchronized void load(File file) {
        configuration = new Configuration(file);
        reload();
    }

    public static synchronized void reload() {
        if (configuration == null) return;
        configuration.load();
        curseForgeTokenUrl = configuration.getString(
            KEY_TOKEN_URL,
            CATEGORY_GENERAL,
            DEFAULT_TOKEN_URL,
            "Endpoint returning JSON like {\"ok\":true,\"token\":\"xxx\"}.");
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static synchronized String getCurseForgeTokenUrl() {
        return curseForgeTokenUrl;
    }

    public static synchronized void setCurseForgeTokenUrl(String value) {
        String next = value == null ? "" : value.trim();
        if (next.isEmpty()) {
            next = DEFAULT_TOKEN_URL;
        }
        boolean changed = !next.equals(curseForgeTokenUrl);
        curseForgeTokenUrl = next;
        if (configuration != null) {
            configuration.get(CATEGORY_GENERAL, KEY_TOKEN_URL, DEFAULT_TOKEN_URL)
                .set(next);
            configuration.save();
        }
        if (changed && VintageResourcifyApiPlugin.isResourcifyLoaded()) {
            CurseTokenProvider.fetchAndApplyAsync("config changed");
        }
    }
}
