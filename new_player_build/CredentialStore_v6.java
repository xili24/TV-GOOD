package com.simpleiptv.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CredentialStore {
    private static final String PREFS = "trio_tv_secure_login";
    private static final String KEY_USER = "user";
    private static final String KEY_PASS = "pass";
    private static final String KEY_ALIAS = "trio_tv_login_key_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private CredentialStore() {}

    public static void save(Context context, String username, String password) throws Exception {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY_USER, encrypt(username)).putString(KEY_PASS, encrypt(password)).apply();
    }

    public static Credentials load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String u = p.getString(KEY_USER, null);
        String pw = p.getString(KEY_PASS, null);
        if (u == null || pw == null) return null;
        try {
            return new Credentials(decrypt(u), decrypt(pw));
        } catch (Throwable e) {
            clear(context);
            return null;
        }
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) return (SecretKey) ks.getKey(KEY_ALIAS, null);

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] packed = new byte[1 + iv.length + encrypted.length];
        packed[0] = (byte) iv.length;
        System.arraycopy(iv, 0, packed, 1, iv.length);
        System.arraycopy(encrypted, 0, packed, 1 + iv.length, encrypted.length);
        return Base64.encodeToString(packed, Base64.NO_WRAP);
    }

    private static String decrypt(String encoded) throws Exception {
        byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
        int ivLength = packed[0] & 0xFF;
        if (ivLength <= 0 || packed.length <= 1 + ivLength) throw new IllegalArgumentException("Invalid encrypted login");
        byte[] iv = new byte[ivLength];
        byte[] encrypted = new byte[packed.length - 1 - ivLength];
        System.arraycopy(packed, 1, iv, 0, ivLength);
        System.arraycopy(packed, 1 + ivLength, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    public static final class Credentials {
        public final String username;
        public final String password;
        public Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
