package com.xilitv.ibo;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private String primary;
    private String fallback;
    private boolean usedFallback;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        primary = getIntent().getStringExtra("url");
        fallback = getIntent().getStringExtra("fallback");

        PlayerView view = new PlayerView(this);
        view.setUseController(true);
        view.setKeepScreenOn(true);
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(view);

        player = new ExoPlayer.Builder(this).build();
        view.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                if (!usedFallback
                        && fallback != null
                        && !fallback.isEmpty()
                        && !fallback.equals(primary)) {
                    usedFallback = true;
                    play(fallback);
                } else {
                    Toast.makeText(
                            PlayerActivity.this,
                            "Kanali nuk mund të luhet.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

        play(primary);
    }

    private void play(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Stream URL mungon.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
