package com.simpleiptv.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private EditText username;
    private EditText password;
    private Button loginButton;
    private ProgressBar progress;
    private TextView error;
    private volatile boolean destroyed = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        progress = findViewById(R.id.loginProgress);
        error = findViewById(R.id.loginError);
        loginButton.setOnClickListener(v -> loginFromFields());

        CredentialStore.Credentials saved = CredentialStore.load(this);
        if (saved != null && !TextUtils.isEmpty(saved.username) && !TextUtils.isEmpty(saved.password)) {
            username.setText(saved.username);
            password.setText(saved.password);
            error.setText("Duke kontrolluar abonimin...");
            performLogin(saved.username, saved.password);
        } else {
            username.requestFocus();
        }
    }

    private void loginFromFields() {
        String u = username.getText().toString().trim();
        String p = password.getText().toString();
        error.setText("");
        if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
            error.setText("Shkruaj username dhe password.");
            return;
        }
        performLogin(u, p);
    }

    private void performLogin(String u, String p) {
        setBusy(true);
        io.execute(() -> {
            try {
                XtreamClient client = new XtreamClient(ServerConfig.PORTAL_BASE_URL);
                XtreamClient.AccountInfo account = client.getAccountInfo(u, p);

                if (!account.authenticated) {
                    CredentialStore.clear(this);
                    postError("Username ose password gabim.", true);
                    return;
                }
                if (account.isExpired()) {
                    postExpired();
                    return;
                }
                if (account.isBlocked()) {
                    postError("Abonimi juaj nuk është aktiv. Kontaktoni furnizuesin.", false);
                    return;
                }

                List<Channel> channels = client.getLiveStreams(u, p);
                if (channels.isEmpty()) {
                    postError("Nuk u gjet asnjë kanal live.", false);
                    return;
                }

                AppSession.username = u;
                AppSession.password = p;
                AppSession.channels.clear();
                AppSession.channels.addAll(channels);
                AppSession.currentIndex = 0;
                try { CredentialStore.save(this, u, p); } catch (Throwable ignored) {}

                if (!destroyed) runOnUiThread(() -> {
                    if (destroyed) return;
                    setBusy(false);
                    startActivity(new Intent(this, ChannelListActivity.class));
                    finish();
                });
            } catch (Throwable e) {
                String msg = e.getMessage();
                postError(msg == null || msg.trim().isEmpty() ? "Login dështoi. Kontrollo internetin." : msg, false);
            }
        });
    }

    private void postExpired() {
        if (destroyed) return;
        runOnUiThread(() -> {
            if (destroyed) return;
            setBusy(false);
            error.setText("ABONIMI JUAJ KA MBARUAR\nKontaktoni furnizuesin për rinovim.");
            error.setTextSize(20);
            error.setVisibility(View.VISIBLE);
            loginButton.requestFocus();
        });
    }

    private void postError(String message, boolean focusUsername) {
        if (destroyed) return;
        runOnUiThread(() -> {
            if (destroyed) return;
            setBusy(false);
            error.setText(message);
            if (focusUsername) username.requestFocus(); else loginButton.requestFocus();
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!busy);
        username.setEnabled(!busy);
        password.setEnabled(!busy);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        io.shutdownNow();
        super.onDestroy();
    }
}
