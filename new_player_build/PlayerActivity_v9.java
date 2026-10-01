package com.simpleiptv.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.List;

public class PlayerActivity extends Activity {
    private PlayerView playerView;
    private ProgressBar progress;
    private TextView overlay;
    private ExoPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int channelIndex;
    private List<String> candidateUrls;
    private int urlIndex;
    private int retryCount;
    private int playGeneration = 0;
    private int watchdogTicket = 0;
    private int watchdogRecoveries = 0;
    private static final long BUFFERING_WATCHDOG_MS = 9000L;
    private long lastChannelKeyMs = 0L;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppSession.channels.isEmpty()) { finish(); return; }
        setContentView(R.layout.activity_player);
        playerView = findViewById(R.id.playerView);
        progress = findViewById(R.id.playerProgress);
        overlay = findViewById(R.id.channelOverlay);
        channelIndex = Math.max(0, Math.min(AppSession.currentIndex, AppSession.channels.size() - 1));
        initPlayer();
        playChannel(channelIndex);
    }

    private void initPlayer() {
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(this).setEnableDecoderFallback(true);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(2500, 15000, 900, 1600).build();
        player = new ExoPlayer.Builder(this).setRenderersFactory(renderers).setLoadControl(loadControl).build();
        playerView.setPlayer(player);
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (progress != null) progress.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                if (state == Player.STATE_BUFFERING) {
                    scheduleBufferingWatchdog(playGeneration);
                } else {
                    watchdogTicket++;
                }
                if (state == Player.STATE_READY) {
                    retryCount = 0;
                    watchdogRecoveries = 0;
                }
            }
            @Override public void onPlayerError(PlaybackException error) {
                if (progress != null) progress.setVisibility(View.VISIBLE);
                scheduleRetryOrFallback(playGeneration);
            }
        });
    }

    private void playChannel(int index) {
        if (player == null || AppSession.channels.isEmpty()) return;
        handler.removeCallbacksAndMessages(null);
        playGeneration++;
        watchdogTicket++;
        watchdogRecoveries = 0;
        channelIndex = (index + AppSession.channels.size()) % AppSession.channels.size();
        AppSession.currentIndex = channelIndex;
        Channel channel = AppSession.channels.get(channelIndex);
        candidateUrls = channel.candidateUrls(ServerConfig.PORTAL_BASE_URL, AppSession.username, AppSession.password);
        urlIndex = 0;
        retryCount = 0;
        showOverlay(String.format("%03d  %s", channelIndex + 1, channel.name));
        prepareCurrentUrl(playGeneration);
    }

    private void prepareCurrentUrl(int generation) {
        if (generation != playGeneration || player == null || candidateUrls == null || candidateUrls.isEmpty()) return;
        String url = candidateUrls.get(Math.min(urlIndex, candidateUrls.size() - 1));
        try {
            player.stop();
            player.clearMediaItems();
            player.setMediaItem(MediaItem.fromUri(url));
            player.prepare();
            player.play();
        } catch (Throwable e) {
            scheduleRetryOrFallback(generation);
        }
    }

    private void scheduleBufferingWatchdog(int generation) {
        final int ticket = ++watchdogTicket;
        handler.postDelayed(() -> {
            if (generation != playGeneration || ticket != watchdogTicket || player == null) return;
            if (player.getPlaybackState() != Player.STATE_BUFFERING) return;

            watchdogRecoveries++;
            showOverlay("Rilidhje automatike...");
            if (watchdogRecoveries <= 2) {
                prepareCurrentUrl(generation);
            } else {
                retryCount = 2;
                scheduleRetryOrFallback(generation);
            }
        }, BUFFERING_WATCHDOG_MS);
    }

    private void scheduleRetryOrFallback(int generation) {
        if (generation != playGeneration) return;
        watchdogTicket++;
        if (retryCount < 2) {
            int delay = 900 + retryCount * 900;
            retryCount++;
            handler.postDelayed(() -> prepareCurrentUrl(generation), delay);
            return;
        }
        if (candidateUrls != null && urlIndex + 1 < candidateUrls.size()) {
            urlIndex++;
            retryCount = 0;
            handler.postDelayed(() -> prepareCurrentUrl(generation), 600);
            return;
        }
        if (progress != null) progress.setVisibility(View.GONE);
        showOverlay("Kanali nuk po hapet");
    }

    private void showOverlay(String text) {
        if (overlay == null) return;
        overlay.setText(text);
        overlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideOverlay);
        handler.postDelayed(hideOverlay, 2500);
    }

    private final Runnable hideOverlay = () -> { if (overlay != null) overlay.setVisibility(View.GONE); };

    private boolean allowChannelChange() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastChannelKeyMs < 280) return false;
        lastChannelKeyMs = now;
        return true;
    }

    private void nextChannel() { if (allowChannelChange()) playChannel(channelIndex + 1); }
    private void previousChannel() { if (allowChannelChange()) playChannel(channelIndex - 1); }

    private void openChannelList() {
        handler.removeCallbacksAndMessages(null);
        playGeneration++;
        watchdogTicket++;
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        startActivity(new Intent(this, ChannelListActivity.class));
        finish();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
                nextChannel(); return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                previousChannel(); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                openChannelList(); return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override protected void onStart() {
        super.onStart();
        if (player != null && player.getMediaItemCount() > 0) player.play();
    }

    @Override protected void onDestroy() {
        playGeneration++;
        watchdogTicket++;
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.stop();
            player.clearMediaItems();
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
