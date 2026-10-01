package com.xilitv.ibo;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class XtreamClient {
    private final String base;
    private final String user;
    private final String pass;

    XtreamClient(String base, String user, String pass) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        this.base = b;
        this.user = user == null ? "" : user.trim();
        this.pass = pass == null ? "" : pass;
    }

    JSONObject authenticate() throws Exception {
        return new JSONObject(get(apiUrl(null, null, null)));
    }

    JSONArray array(String action, String key, String value) throws Exception {
        return new JSONArray(get(apiUrl(action, key, value)));
    }

    JSONObject object(String action, String key, String value) throws Exception {
        return new JSONObject(get(apiUrl(action, key, value)));
    }

    String liveUrl(String id, String ext) {
        return stream("live", id, ext == null || ext.isEmpty() ? "ts" : ext);
    }

    String movieUrl(String id, String ext) {
        return stream("movie", id, ext == null || ext.isEmpty() ? "mp4" : ext);
    }

    String seriesUrl(String id, String ext) {
        return stream("series", id, ext == null || ext.isEmpty() ? "mp4" : ext);
    }

    private String stream(String type, String id, String ext) {
        return base + "/" + type + "/" + Uri.encode(user) + "/" + Uri.encode(pass) + "/" + id + "." + ext;
    }

    private String apiUrl(String action, String key, String value) {
        StringBuilder s = new StringBuilder(base)
                .append("/player_api.php?username=").append(Uri.encode(user))
                .append("&password=").append(Uri.encode(pass));
        if (action != null && !action.isEmpty()) s.append("&action=").append(Uri.encode(action));
        if (key != null && value != null) s.append("&").append(Uri.encode(key)).append("=").append(Uri.encode(value));
        return s.toString();
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(18000);
        c.setRequestProperty("User-Agent", "XiliTV/1.0 AndroidTV");
        c.setRequestProperty("Accept", "application/json,*/*");
        c.setInstanceFollowRedirects(true);
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new IllegalStateException("HTTP " + code);
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) out.append(line);
        r.close();
        c.disconnect();
        if (code < 200 || code >= 400) {
            throw new IllegalStateException("HTTP " + code);
        }
        return out.toString();
    }
}
