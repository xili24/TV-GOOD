package com.xilitv.ibo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = Ui.vertical(this);
        root.setPadding(
                Ui.dp(this, 56),
                Ui.dp(this, 40),
                Ui.dp(this, 56),
                Ui.dp(this, 40)
        );

        TextView title = Ui.title(this, "SETTINGS", 30);
        TextView mac = info("Device MAC", DeviceIdentity.mac(this));
        TextView portal = info("Portal", AppPrefs.portal(this));
        TextView user = info("Username", AppPrefs.user(this));
        TextView expiry = info(
                "Expiry",
                AppPrefs.expiry(this).isEmpty() ? "Unknown" : AppPrefs.expiry(this)
        );

        TextView logout = Ui.card(this, "REMOVE PLAYLIST / LOGOUT");
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(Ui.dp(this, 520), Ui.dp(this, 68));
        lp.topMargin = Ui.dp(this, 30);
        logout.setLayoutParams(lp);

        logout.setOnClickListener(v -> {
            AppPrefs.clear(this);

            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(i);
            finish();
        });

        root.addView(title);
        root.addView(mac);
        root.addView(portal);
        root.addView(user);
        root.addView(expiry);
        root.addView(logout);

        setContentView(root);
        logout.requestFocus();
    }

    private TextView info(String label, String value) {
        TextView v = Ui.title(this, label + "\n" + value, 18);
        v.setTextColor(Ui.MUTED);
        v.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Ui.dp(this, 78)
                );
        lp.topMargin = Ui.dp(this, 12);
        v.setLayoutParams(lp);
        return v;
    }
}
