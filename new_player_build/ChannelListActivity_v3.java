package com.simpleiptv.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ChannelListActivity extends Activity {
    private ListView list;
    private ArrayAdapter<String> adapter;
    private final List<String> names = new ArrayList<>();
    private int selectedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (AppSession.channels == null || AppSession.channels.isEmpty()) {
                showFatal("Lista e kanaleve është bosh pas login-it.");
                return;
            }
            buildSimpleUi();
        } catch (Throwable t) {
            showFatal("Gabim duke hapur listën: " + t.getClass().getSimpleName() + " - " + safe(t.getMessage()));
        }
    }

    private void buildSimpleUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 10, 15));
        root.setPadding(dp(42), dp(28), dp(42), dp(30));

        TextView header = new TextView(this);
        header.setText("TRIO TV   •   " + AppSession.channels.size() + " KANALE");
        header.setTextColor(Color.WHITE);
        header.setTextSize(26);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, 0, dp(22));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list = new ListView(this);
        list.setBackgroundColor(Color.rgb(7, 10, 15));
        list.setDividerHeight(dp(10));
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setFocusable(true);
        list.setFocusableInTouchMode(true);
        list.setSelector(android.R.color.transparent);

        names.clear();
        for (int i = 0; i < AppSession.channels.size(); i++) {
            Channel c = AppSession.channels.get(i);
            names.add(c == null ? "Kanal" : c.name);
        }

        selectedPosition = AppSession.currentIndex;
        if (selectedPosition < 0 || selectedPosition >= AppSession.channels.size()) {
            selectedPosition = 0;
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                boolean selected = position == selectedPosition;

                String number = String.format("%03d", position + 1);
                v.setText((selected ? "▶  " : "    ") + number + "   " + names.get(position));
                v.setTextColor(Color.WHITE);
                v.setTextSize(selected ? 23 : 21);
                v.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
                v.setGravity(Gravity.CENTER_VERTICAL);
                v.setPadding(dp(28), dp(14), dp(22), dp(14));
                v.setMinHeight(dp(76));
                v.setBackgroundColor(selected
                        ? Color.rgb(28, 105, 225)
                        : Color.rgb(22, 31, 43));
                return v;
            }
        };

        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> open(position));

        list.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < AppSession.channels.size()) {
                    selectedPosition = position;
                    AppSession.currentIndex = position;
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        AppSession.currentIndex = selectedPosition;
        list.setSelection(selectedPosition);
        list.requestFocus();
        adapter.notifyDataSetChanged();
    }

    private void open(int position) {
        try {
            if (position < 0 || position >= AppSession.channels.size()) return;
            selectedPosition = position;
            AppSession.currentIndex = position;
            if (adapter != null) adapter.notifyDataSetChanged();
            startActivity(new Intent(this, PlayerActivity.class));
        } catch (Throwable t) {
            showFatal("Gabim duke hapur kanalin: " + t.getClass().getSimpleName() + " - " + safe(t.getMessage()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (list != null && !AppSession.channels.isEmpty()) {
                int p = AppSession.currentIndex;
                if (p < 0 || p >= AppSession.channels.size()) p = 0;
                selectedPosition = p;
                list.setSelection(p);
                list.requestFocus();
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (list != null && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
            int p = selectedPosition;
            if (p >= 0) {
                open(p);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showFatal(String message) {
        TextView v = new TextView(this);
        v.setText(message + "\n\nShtyp BACK për login.");
        v.setTextColor(Color.WHITE);
        v.setTextSize(20);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(40), dp(40), dp(40), dp(40));
        v.setBackgroundColor(Color.rgb(70, 15, 15));
        setContentView(v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String safe(String s) {
        return s == null ? "pa detaje" : s;
    }
}
