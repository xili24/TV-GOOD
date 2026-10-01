package com.xili.macstb.ui;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.xili.macstb.net.StalkerClient;

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends Activity {
    private static final String MAG_UA = "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG254 stbapp ver: 4 rev: 2721 Mobile Safari/533.3";
    private static final String MAG_X_UA = "Model: MAG254; Link: Ethernet";

    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar loading;
    private TextView nameView;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int retryCount = 0;
    private int resolveGeneration = 0;
    private String cmd;
    private String portal;
    private String api;
    private String mac;
    private String token;
    private volatile boolean destroyed;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        cmd = getIntent().getStringExtra("cmd");
        portal = getIntent().getStringExtra("portal");
        api = getIntent().getStringExtra("api");
        mac = getIntent().getStringExtra("mac");
        token = getIntent().getStringExtra("token");
        buildUi(getIntent().getStringExtra("name"));
        resolveAndPlay(false);
    }

    private void buildUi(String channelName) {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(android.graphics.Color.BLACK);

        playerView = new PlayerView(this);
        playerView.setUseController(false);
        playerView.setControllerAutoShow(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setFocusable(false);
        playerView.setClickable(false);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        nameView = Ui.label(this, channelName == null ? "Live TV" : channelName, 20, Ui.TEXT);
        nameView.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0);
        nameView.setBackground(Ui.rounded(0xCC10151D, Ui.dp(this, 9)));
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-2, Ui.dp(this, 52), Gravity.TOP | Gravity.LEFT);
        np.leftMargin = Ui.dp(this, 24);
        np.topMargin = Ui.dp(this, 22);
        root.addView(nameView, np);

        loading = new ProgressBar(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54), Gravity.CENTER);
        root.addView(loading, lp);
        setContentView(root);

        main.postDelayed(() -> {
            if (!destroyed && nameView != null) nameView.setVisibility(View.GONE);
        }, 2200);
    }

    private void resolveAndPlay(boolean refreshSession) {
        final int generation = ++resolveGeneration;
        loading.setVisibility(View.VISIBLE);
        io.execute(() -> {
            try {
                StalkerClient c = new StalkerClient(mac);
                if (refreshSession) {
                    com.xili.macstb.model.PortalSession s = c.login(portal);
                    api = s.apiUrl;
                    token = s.token;
                } else {
                    c.resume(api, token);
                }
                String url = c.createLink(cmd);
                if (destroyed || generation != resolveGeneration) return;
                runOnUiThread(() -> {
                    if (!destroyed && generation == resolveGeneration) startPlayer(url, generation);
                });
            } catch (Exception e) {
                if (destroyed || generation != resolveGeneration) return;
                runOnUiThread(() -> {
                    if (destroyed || generation != resolveGeneration) return;
                    loading.setVisibility(View.GONE);
                    toast("Stream error: " + safe(e.getMessage()));
                });
            }
        });
    }

    private void startPlayer(String url, int generation) {
        if (destroyed || generation != resolveGeneration) return;
        releasePlayer();

        DefaultRenderersFactory renderers = new DefaultRenderersFactory(this)
                .setEnableDecoderFallback(true);

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", MAG_UA);
        headers.put("X-User-Agent", MAG_X_UA);
        headers.put("Cookie", "mac=" + cookie(mac) + "; stb_lang=en; timezone=Europe%2FBelgrade;");
        String referer = portalReferer(api);
        if (!referer.isEmpty()) headers.put("Referer", referer);

        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent(MAG_UA)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(20000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers);
        DefaultDataSource.Factory data = new DefaultDataSource.Factory(this, http);
        DefaultMediaSourceFactory mediaSources = new DefaultMediaSourceFactory(data);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(2500, 15000, 1000, 2000)
                .build();

        player = new ExoPlayer.Builder(this, renderers)
                .setMediaSourceFactory(mediaSources)
                .setLoadControl(loadControl)
                .build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (destroyed || generation != resolveGeneration) return;
                loading.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                if (state == Player.STATE_READY) retryCount = 0;
            }

            @Override public void onPlayerError(PlaybackException error) {
                if (destroyed || generation != resolveGeneration) return;
                if (retryCount < 2) {
                    retryCount++;
                    releasePlayer();
                    loading.setVisibility(View.VISIBLE);
                    final boolean refreshSession = retryCount >= 2;
                    main.postDelayed(() -> {
                        if (!destroyed) resolveAndPlay(refreshSession);
                    }, 700L);
                } else {
                    loading.setVisibility(View.GONE);
                    toast("This channel is currently unavailable.");
                }
            }
        });

        MediaItem item = new MediaItem.Builder().setUri(Uri.parse(url)).build();
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    private void releasePlayer() {
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }

    private static String cookie(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return ""; }
    }

    private static String portalReferer(String api) {
        try {
            if (api == null || api.isEmpty()) return "";
            URI u = URI.create(api);
            if (u.getRawAuthority() == null) return "";
            String p = u.getPath() == null ? "" : u.getPath();
            if (p.endsWith("/server/load.php")) p = p.substring(0, p.length() - "/server/load.php".length()) + "/c/";
            else if (p.endsWith("/portal.php")) p = p.substring(0, p.length() - "/portal.php".length()) + "/c/";
            else if (!p.endsWith("/")) p += "/";
            return u.getScheme() + "://" + u.getRawAuthority() + p;
        } catch (Exception e) {
            return "";
        }
    }

    private String safe(String s) { return s == null || s.isEmpty() ? "unable to open channel" : s; }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    @Override protected void onStop() {
        resolveGeneration++;
        releasePlayer();
        super.onStop();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        resolveGeneration++;
        main.removeCallbacksAndMessages(null);
        releasePlayer();
        io.shutdownNow();
        super.onDestroy();
    }
}
