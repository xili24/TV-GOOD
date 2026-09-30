package com.simpleiptv.tv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class XtreamClient {
    private final String baseUrl;
    public XtreamClient(String baseUrl) { this.baseUrl = baseUrl.replaceAll("/+$", ""); }

    public AccountInfo getAccountInfo(String username, String password) throws Exception {
        JSONObject root = new JSONObject(get(apiUrl(username, password, null)));
        JSONObject info = root.optJSONObject("user_info");
        if (info == null) return new AccountInfo(false, "", 0L);
        return new AccountInfo(info.optInt("auth", 0) == 1, info.optString("status", ""), parseLong(info.opt("exp_date")));
    }

    public boolean authenticate(String username, String password) throws Exception {
        AccountInfo info = getAccountInfo(username, password);
        return info.authenticated && !info.isBlocked() && !info.isExpired();
    }

    public List<Channel> getLiveStreams(String username, String password) throws Exception {
        JSONArray array = new JSONArray(get(apiUrl(username, password, "get_live_streams")));
        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject x = array.optJSONObject(i);
            if (x == null) continue;
            int id = x.optInt("stream_id", -1);
            if (id < 0) continue;
            channels.add(new Channel(id, x.optInt("num", i + 1), x.optString("name", "Channel " + id),
                    x.optString("stream_icon", ""), x.optString("container_extension", ""), x.optString("direct_source", "")));
        }
        channels.sort(Comparator.comparingInt(c -> c.number));
        return channels;
    }

    private String apiUrl(String username, String password, String action) {
        String result = baseUrl + "/player_api.php?username=" + encodeUtf8(username) + "&password=" + encodeUtf8(password);
        if (action != null) result += "&action=" + action;
        return result;
    }

    private static String encodeUtf8(String value) {
        try { return URLEncoder.encode(value == null ? "" : value, "UTF-8"); }
        catch (Exception e) { return value == null ? "" : value; }
    }

    private static long parseLong(Object value) {
        if (value == null || value == JSONObject.NULL) return 0L;
        try {
            String s = String.valueOf(value).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return 0L;
            return Long.parseLong(s);
        } catch (Throwable ignored) { return 0L; }
    }

    private String get(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(ServerConfig.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(ServerConfig.READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "TVPlayer/1.0 AndroidTV");
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) { conn.disconnect(); throw new IllegalStateException("HTTP " + code); }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        } finally { conn.disconnect(); }
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
        return sb.toString();
    }

    public static final class AccountInfo {
        public final boolean authenticated;
        public final String status;
        public final long expDate;
        public AccountInfo(boolean authenticated, String status, long expDate) {
            this.authenticated = authenticated; this.status = status == null ? "" : status; this.expDate = expDate;
        }
        public boolean isExpired() {
            if ("Expired".equalsIgnoreCase(status)) return true;
            return expDate > 0 && (System.currentTimeMillis() / 1000L) >= expDate;
        }
        public boolean isBlocked() {
            return "Disabled".equalsIgnoreCase(status) || "Banned".equalsIgnoreCase(status);
        }
    }
}
