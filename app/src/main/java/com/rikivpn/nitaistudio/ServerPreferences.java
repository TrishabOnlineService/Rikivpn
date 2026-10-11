package com.rikivpn.nitaistudio;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists the user's selected server across app restarts.
 * Uses SharedPreferences (the app's existing persistence system - no new
 * storage dependency such as DataStore is required for a single small value).
 */
public final class ServerPreferences {

    private static final String PREFS_NAME = "riki_server_prefs";
    private static final String KEY_SELECTED_SERVER = "selected_server_key";
    private static final String DEFAULT_SERVER_KEY = "free:japan"; // matches assets/free_vpn/japan.ovpn

    private ServerPreferences() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setSelectedServerKey(Context context, String serverKey) {
        prefs(context).edit().putString(KEY_SELECTED_SERVER, serverKey).apply();
    }

    public static String getSelectedServerKey(Context context) {
        return prefs(context).getString(KEY_SELECTED_SERVER, DEFAULT_SERVER_KEY);
    }
}
