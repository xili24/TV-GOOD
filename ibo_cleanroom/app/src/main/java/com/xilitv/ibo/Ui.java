package com.xilitv.ibo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(9, 10, 12);
    static final int PANEL = Color.rgb(28, 30, 34);
    static final int PANEL_FOCUS = Color.rgb(196, 39, 55);
    static final int WHITE = Color.WHITE;
    static final int MUTED = Color.rgb(185, 188, 194);

    private Ui() {}

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable bg(int color, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static TextView title(Context c, String text, float sp) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(WHITE);
        v.setTextSize(sp);
        v.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return v;
    }

    static TextView card(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(WHITE);
        v.setTextSize(23);
        v.setGravity(Gravity.CENTER);
        v.setFocusable(true);
        v.setClickable(true);
        v.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        v.setBackground(bg(PANEL, 14, c));
        v.setOnFocusChangeListener((view, focused) -> {
            view.setBackground(bg(focused ? PANEL_FOCUS : PANEL, 14, c));
            view.animate().scaleX(focused ? 1.045f : 1f).scaleY(focused ? 1.045f : 1f).setDuration(90).start();
        });
        return v;
    }

    static EditText input(Context c, String hint, boolean password) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(WHITE);
        e.setTextSize(19);
        e.setSingleLine(true);
        e.setPadding(dp(c, 18), 0, dp(c, 18), 0);
        e.setBackground(bg(PANEL, 10, c));
        if (password) e.setInputType(0x00000081);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 58));
        lp.topMargin = dp(c, 12);
        e.setLayoutParams(lp);
        return e;
    }

    static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(BG);
        return l;
    }
}
