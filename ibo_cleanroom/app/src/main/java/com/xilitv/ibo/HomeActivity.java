package com.xilitv.ibo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this, 54), Ui.dp(this, 32), Ui.dp(this, 54), Ui.dp(this, 32));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = Ui.title(this, "XILI TV", 34);
        TextView account = Ui.title(this, AppPrefs.user(this), 16);
        account.setTextColor(Ui.MUTED);
        account.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(brand, new LinearLayout.LayoutParams(0, Ui.dp(this, 62), 1f));
        top.addView(account, new LinearLayout.LayoutParams(Ui.dp(this, 300), Ui.dp(this, 62)));

        TextView hint = Ui.title(this, "Choose a section", 17);
        hint.setTextColor(Ui.MUTED);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);

        TextView live = homeCard("LIVE TV");
        TextView movies = homeCard("MOVIES");
        TextView series = homeCard("SERIES");
        TextView settings = homeCard("SETTINGS");

        live.setOnClickListener(v -> open("live"));
        movies.setOnClickListener(v -> open("vod"));
        series.setOnClickListener(v -> open("series"));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        addCard(row, live);
        addCard(row, movies);
        addCard(row, series);
        addCard(row, settings);

        root.addView(top);
        root.addView(hint);
        root.addView(row, rowLp);
        setContentView(root);
        live.requestFocus();
    }

    private TextView homeCard(String text) {
        TextView v = Ui.card(this, text);
        v.setTextSize(26);
        return v;
    }

    private void addCard(LinearLayout row, TextView v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 230), 1f);
        lp.setMargins(Ui.dp(this, 9), Ui.dp(this, 20), Ui.dp(this, 9), Ui.dp(this, 20));
        row.addView(v, lp);
    }

    private void open(String type) {
        Intent i = new Intent(this, BrowseActivity.class);
        i.putExtra("type", type);
        startActivity(i);
    }
}
