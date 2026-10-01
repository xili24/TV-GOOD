package com.xilitv.ibo;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    private static final String NAME = "xili_ibo";
    private AppPrefs() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static void saveLogin(Context c, String portal, String user, String pass, String expiry) {
        p(c).edit()
                .putString("portal", portal)
                .putString("user", user)
                .putString("pass", pass)
                .putString("expiry", expiry == null ? "" : expiry)
                .apply();
    }

    static String portal(Context c) { return p(c).getString("portal", ""); }
    static String user(Context c) { return p(c).getString("user", ""); }
    static String pass(Context c) { return p(c).getString("pass", ""); }
    static String expiry(Context c) { return p(c).getString("expiry", ""); }
    static boolean hasLogin(Context c) {
        return !portal(c).isEmpty() && !user(c).isEmpty() && !pass(c).isEmpty();
    }
    static void clear(Context c) { p(c).edit().clear().apply(); }
}
