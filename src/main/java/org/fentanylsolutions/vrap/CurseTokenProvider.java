package org.fentanylsolutions.vrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.dediamondpro.resourcify.config.Config;

public final class CurseTokenProvider {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private static final AtomicBoolean FETCH_RUNNING = new AtomicBoolean(false);
    private static final AtomicBoolean FETCH_AGAIN = new AtomicBoolean(false);
    private static final AtomicBoolean HOOK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicReference<String> TOKEN = new AtomicReference<>("");

    private static final Runnable RESOURCIFY_CONFIG_LOAD_CALLBACK = new Runnable() {

        @Override
        public void run() {
            imposeToken();
        }
    };

    private CurseTokenProvider() {}

    public static void registerResourcifyConfigHook() {
        if (HOOK_REGISTERED.compareAndSet(false, true)) {
            Config.addLoadCallback(RESOURCIFY_CONFIG_LOAD_CALLBACK, true);
        }
    }

    public static void fetchAndApplyAsync(String reason) {
        if (!FETCH_RUNNING.compareAndSet(false, true)) {
            FETCH_AGAIN.set(true);
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                String currentReason = reason;
                do {
                    FETCH_AGAIN.set(false);
                    fetchAndApply(VrapConfig.getCurseForgeTokenUrl(), currentReason);
                    currentReason = "queued config change";
                } while (FETCH_AGAIN.get());
            } finally {
                FETCH_RUNNING.set(false);
                if (FETCH_AGAIN.getAndSet(false)) {
                    fetchAndApplyAsync("queued config change");
                }
            }
        }, "VRAP-CurseForgeTokenFetch");
        thread.setDaemon(true);
        thread.start();
    }

    private static void fetchAndApply(String configuredUrl, String reason) {
        String normalizedUrl = normalizeUrl(configuredUrl);
        if (normalizedUrl.isEmpty()) {
            VintageResourcifyApiPlugin.LOG.warn("Skipping CurseForge token fetch: URL is blank");
            return;
        }
        try {
            TokenResponse response = fetchToken(normalizedUrl);
            if (!sameConfiguredUrl(configuredUrl)) {
                VintageResourcifyApiPlugin.LOG
                    .info("Discarding CurseForge token fetched from stale URL {}", normalizedUrl);
                return;
            }
            if (!response.ok || response.token.isEmpty()) {
                VintageResourcifyApiPlugin.LOG.warn(
                    "CurseForge token endpoint returned ok={} tokenPresent={}",
                    response.ok,
                    !response.token.isEmpty());
                return;
            }
            TOKEN.set(response.token);
            imposeToken();
            VintageResourcifyApiPlugin.LOG
                .info("Fetched and applied CurseForge API token from {} ({})", normalizedUrl, reason);
        } catch (Exception e) {
            VintageResourcifyApiPlugin.LOG.warn("Failed to fetch CurseForge API token from {}", normalizedUrl, e);
        }
    }

    private static TokenResponse fetchToken(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", VintageResourcifyApiPlugin.NAME + "/" + Tags.VERSION);
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream()
                : connection.getErrorStream();
            String body = readBody(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + ": " + body);
            }
            JsonElement parsed = new JsonParser().parse(body);
            if (!parsed.isJsonObject()) {
                throw new IOException("Response is not a JSON object");
            }
            JsonObject object = parsed.getAsJsonObject();
            boolean ok = object.has("ok") && object.get("ok")
                .getAsBoolean();
            String token = object.has("token") && !object.get("token")
                .isJsonNull() ? object.get("token")
                    .getAsString()
                    .trim() : "";
            return new TokenResponse(ok, token);
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    private static void imposeToken() {
        String token = TOKEN.get();
        if (token == null || token.isEmpty()) return;
        Config config = Config.getInstance();
        if (!token.equals(config.getCurseApiToken())) {
            config.setCurseApiToken(token);
        }
    }

    private static boolean sameConfiguredUrl(String original) {
        return normalizeUrl(original).equals(normalizeUrl(VrapConfig.getCurseForgeTokenUrl()));
    }

    private static String normalizeUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.contains("://")) return trimmed;
        return "https://" + trimmed;
    }

    private static final class TokenResponse {

        private final boolean ok;
        private final String token;

        private TokenResponse(boolean ok, String token) {
            this.ok = ok;
            this.token = token == null ? "" : token;
        }
    }
}
