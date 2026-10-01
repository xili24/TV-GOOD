package com.xilitv.ibo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    private EditText portal;
    private EditText user;
    private EditText pass;
    private TextView login;
    private ProgressBar loading;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (AppPrefs.hasLogin(this)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        LinearLayout root = Ui.vertical(this);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 90), Ui.dp(this, 40), Ui.dp(this, 90), Ui.dp(this, 40));

        LinearLayout box = Ui.vertical(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Ui.dp(this, 34), Ui.dp(this, 30), Ui.dp(this, 34), Ui.dp(this, 28));
        box.setBackground(Ui.bg(Ui.PANEL, 18, this));
        LinearLayout.LayoutParams boxLp =
                new LinearLayout.LayoutParams(Ui.dp(this, 620), ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView brand = Ui.title(this, "XILI TV", 38);
        brand.setGravity(Gravity.CENTER);
        TextView subtitle = Ui.title(this, "IPTV PLAYER", 18);
        subtitle.setTextColor(Ui.MUTED);
        subtitle.setGravity(Gravity.CENTER);

        portal = Ui.input(this, "Portal URL  (http://server:port)", false);
        user = Ui.input(this, "Username", false);
        pass = Ui.input(this, "Password", true);

        login = Ui.card(this, "LOGIN");
        LinearLayout.LayoutParams loginLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 64));
        loginLp.topMargin = Ui.dp(this, 18);
        login.setLayoutParams(loginLp);
        login.setOnClickListener(v -> doLogin());

        loading = new ProgressBar(this);
        loading.setVisibility(ProgressBar.GONE);
        LinearLayout.LayoutParams loadLp =
                new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        loadLp.gravity = Gravity.CENTER_HORIZONTAL;
        loadLp.topMargin = Ui.dp(this, 18);

        TextView mac = Ui.title(this, "Device MAC: " + DeviceIdentity.mac(this), 15);
        mac.setTextColor(Ui.MUTED);
        mac.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams macLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        macLp.topMargin = Ui.dp(this, 18);
        mac.setLayoutParams(macLp);

        box.addView(brand);
        box.addView(subtitle);
        box.addView(portal);
        box.addView(user);
        box.addView(pass);
        box.addView(login);
        box.addView(loading, loadLp);
        box.addView(mac);
        root.addView(box, boxLp);
        setContentView(root);
        portal.requestFocus();
    }

    private void doLogin() {
        String p = portal.getText().toString().trim();
        String u = user.getText().toString().trim();
        String pw = pass.getText().toString();
        if (p.isEmpty() || u.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "Plotëso Portal URL, Username dhe Password.", Toast.LENGTH_SHORT).show();
            return;
        }

        login.setEnabled(false);
        loading.setVisibility(ProgressBar.VISIBLE);

        new Thread(() -> {
            try {
                XtreamClient api = new XtreamClient(p, u, pw);
                JSONObject root = api.authenticate();
                JSONObject info = root.optJSONObject("user_info");
                if (info == null) throw new IllegalStateException("Përgjigje e pavlefshme nga portali.");

                String auth = info.optString("auth", "0");
                String status = info.optString("status", "");
                if (!"1".equals(auth) && !"Active".equalsIgnoreCase(status)) {
                    String msg = "Expired".equalsIgnoreCase(status) || "Disabled".equalsIgnoreCase(status)
                            ? "Abonimi juaj ka mbaruar ose nuk është aktiv."
                            : "Login i pasaktë ose abonim joaktiv.";
                    throw new IllegalStateException(msg);
                }

                String exp = info.optString("exp_date", "");
                runOnUiThread(() -> {
                    AppPrefs.saveLogin(this, p, u, pw, exp);
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    login.setEnabled(true);
                    loading.setVisibility(ProgressBar.GONE);
                    Toast.makeText(this,
                            e.getMessage() == null ? "Connection failed." : e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
