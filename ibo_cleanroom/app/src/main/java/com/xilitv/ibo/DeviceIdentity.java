package com.xilitv.ibo;

import android.content.Context;
import android.provider.Settings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class DeviceIdentity {
    private DeviceIdentity() {}

    static String mac(Context c) {
        try {
            String androidId = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null) androidId = "xili-tv-device";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(("XILI:" + androidId).getBytes(StandardCharsets.UTF_8));
            int[] b = new int[] {0x02, h[0] & 0xff, h[1] & 0xff, h[2] & 0xff, h[3] & 0xff, h[4] & 0xff};
            return String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", b[0], b[1], b[2], b[3], b[4], b[5]);
        } catch (Exception e) {
            return "02:00:00:00:00:01";
        }
    }
}
