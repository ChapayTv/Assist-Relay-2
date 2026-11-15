package me.vladimir.assistrelay;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String DEFAULT_TARGET = "com.openai.chatgpt";
    private static final String FILE = "relay";
    private static final String KEY_TARGET = "target_package";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean getChime(Context c) {
        return sp(c).getBoolean("chime", true);
    }

    static void setChime(Context c, boolean on) {
        sp(c).edit().putBoolean("chime", on).apply();
    }

    static int getLead(Context c) {
        return sp(c).getInt("lead_ms", 300);
    }

    static void setLead(Context c, int ms) {
        sp(c).edit().putInt("lead_ms", ms).apply();
    }

    static String getLastLog(Context c) {
        return sp(c).getString("last_log", "ещё не запускалось");
    }

    static void setLastLog(Context c, String s) {
        sp(c).edit().putString("last_log", s).apply();
    }

    static String getTarget(Context c) {
        return sp(c).getString(KEY_TARGET, DEFAULT_TARGET);
    }

    static void setTarget(Context c, String pkg) {
        sp(c).edit().putString(KEY_TARGET, pkg).apply();
    }
}
