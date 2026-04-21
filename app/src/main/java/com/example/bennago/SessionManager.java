package com.example.bennago;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME        = "BennaGoSession";
    private static final String KEY_USER_ID      = "user_id";
    private static final String KEY_USER_NAME    = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_IS_ADMIN     = "is_admin";

    private final SharedPreferences        prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUserSession(int userId, String userName, boolean isAdmin) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putBoolean(KEY_IS_ADMIN, isAdmin);
        editor.apply();
    }

    // ✅ Met à jour le nom affiché après modification dans ProfileActivity
    public void updateUserName(String newName) {
        editor.putString(KEY_USER_NAME, newName);
        editor.apply();
    }

    /** Retourne true si un USER normal est connecté */
    public boolean isUserLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                && !prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    /** Retourne true si un ADMIN est connecté */
    public boolean isAdminLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                && prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    public String getUserName() { return prefs.getString(KEY_USER_NAME, ""); }
    public int    getUserId()   { return prefs.getInt(KEY_USER_ID, -1); }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}