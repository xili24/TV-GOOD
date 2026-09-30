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

    public XtreamClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public boolean authenticate(String username, String password) throws Exception {
        String url = apiUrl(username, password, null);
        JSONObject root = new JSONObject(get(url));
        JSONObject info = root.optJSONObject("user_info");
        if (info == null) return false;
        return info.optInt("auth", 0) == 1
                && !"Disabled".equalsIgnoreCase(info.optString("status"));
    }

    public List<Channel> getLiveStreams(String username, String password) throws Exception {
        String url = apiUrl(username, password, "get_live_streams");
        String response = get(url);
        JSONArray array = new JSONArray(response);

        List<Channel> channels = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject x = array.optJSONObject(i);
            if (x == null) continue;

            int id = x.optInt("stream_id", -1);
            if (id < 0) continue;

            int num = x.optInt("num", i + 1);
            channels.add(new Channel(
                    id,
                    num,
                    x.optString("name", "Channel " + id),
                    x.optString("stream_icon", ""),
                    x.optString("container_extension", ""),
                    x.optString("direct_source", "")
            ));
        }

        channels.sort(Comparator.comparingInt(c -> c.number));
        return channels;
    }

    private String apiUrl(String username, String password, String action) {
        String u = encodeUtf8(username);
        String p = encodeUtf8(password);
        String result = baseUrl + "/player_api.php?username=" + u + "&password=" + p;
        if (action != null) {
            result += "&action=" + action;
        }
        return result;
    }

    private static String encodeUtf8(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return value == null ? "" : value;
        }
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
        InputStream stream = code >= 200 && code < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            conn.disconnect();
            throw new IllegalStateException("HTTP " + code);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            conn.disconnect();
        }

        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }

        return sb.toString();
    }
}
