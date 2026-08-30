package com.example.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    private static final String PREFS_NAME = "sharex_settings";
    private static final String KEY_ORIGINAL_DEFAULT = "original_quality_default";
    private static final String KEY_WIFI_AUTO_DOWNLOAD = "wifi_auto_download";

    private AppPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isOriginalQualityDefault(Context context) {
        return prefs(context).getBoolean(KEY_ORIGINAL_DEFAULT, true);
    }

    public static void setOriginalQualityDefault(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ORIGINAL_DEFAULT, enabled).apply();
    }

    public static boolean isWifiAutoDownloadEnabled(Context context) {
        return prefs(context).getBoolean(KEY_WIFI_AUTO_DOWNLOAD, true);
    }

    public static void setWifiAutoDownloadEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_WIFI_AUTO_DOWNLOAD, enabled).apply();
    }
}
