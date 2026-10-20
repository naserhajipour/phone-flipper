package com.phoneflipper.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * All application state lives in a single private SharedPreferences file.
 * Nothing is ever sent off the device; the app has no network permission.
 */
public final class Prefs {

    private static final String FILE = "flipper_prefs";

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN = "en";
    public static final String LANG_FA = "fa";

    public static final String UNIT_UT = "ut";
    public static final String UNIT_MG = "mg";

    private static final String KEY_LANG = "language";
    private static final String KEY_KEEP_ON = "keep_screen_on";
    private static final String KEY_HAPTICS = "haptics";
    private static final String KEY_UNIT = "emf_unit";
    private static final String KEY_NFC_HISTORY = "nfc_history";
    private static final String KEY_CONSENT = "safety_accepted";
    private static final String KEY_BRIDGE_ADDRESS = "bridge_address";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String language(Context c) {
        return sp(c).getString(KEY_LANG, LANG_SYSTEM);
    }

    public static void setLanguage(Context c, String lang) {
        sp(c).edit().putString(KEY_LANG, lang).apply();
    }

    public static boolean keepScreenOn(Context c) {
        return sp(c).getBoolean(KEY_KEEP_ON, true);
    }

    public static void setKeepScreenOn(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_KEEP_ON, on).apply();
    }

    public static boolean haptics(Context c) {
        return sp(c).getBoolean(KEY_HAPTICS, true);
    }

    public static void setHaptics(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_HAPTICS, on).apply();
    }

    public static String emfUnit(Context c) {
        return sp(c).getString(KEY_UNIT, UNIT_UT);
    }

    public static void setEmfUnit(Context c, String unit) {
        sp(c).edit().putString(KEY_UNIT, unit).apply();
    }

    public static boolean safetyAccepted(Context c) {
        return sp(c).getBoolean(KEY_CONSENT, false);
    }

    public static void setSafetyAccepted(Context c, boolean accepted) {
        sp(c).edit().putBoolean(KEY_CONSENT, accepted).apply();
    }

    public static String bridgeAddress(Context c) {
        return sp(c).getString(KEY_BRIDGE_ADDRESS, "");
    }

    public static void setBridgeAddress(Context c, String address) {
        sp(c).edit().putString(KEY_BRIDGE_ADDRESS, address).apply();
    }

    // ------------------------------------------------------------- NFC history

    public static class NfcRecord {
        public final String uid;
        public final String tech;
        public final String payload;
        public final long timestamp;

        public NfcRecord(String uid, String tech, String payload, long timestamp) {
            this.uid = uid;
            this.tech = tech;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }

    public static List<NfcRecord> nfcHistory(Context c) {
        List<NfcRecord> out = new ArrayList<>();
        String raw = sp(c).getString(KEY_NFC_HISTORY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new NfcRecord(
                        o.optString("uid"),
                        o.optString("tech"),
                        o.optString("payload"),
                        o.optLong("time")));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static void addNfcRecord(Context c, NfcRecord record) {
        List<NfcRecord> list = nfcHistory(c);
        list.add(0, record);
        while (list.size() > 40) {
            list.remove(list.size() - 1);
        }
        JSONArray arr = new JSONArray();
        try {
            for (NfcRecord r : list) {
                JSONObject o = new JSONObject();
                o.put("uid", r.uid);
                o.put("tech", r.tech);
                o.put("payload", r.payload);
                o.put("time", r.timestamp);
                arr.put(o);
            }
        } catch (JSONException ignored) {
        }
        sp(c).edit().putString(KEY_NFC_HISTORY, arr.toString()).apply();
    }

    public static void clearNfcHistory(Context c) {
        sp(c).edit().remove(KEY_NFC_HISTORY).apply();
    }

    public static void clearAll(Context c) {
        sp(c).edit().clear().apply();
    }
}
