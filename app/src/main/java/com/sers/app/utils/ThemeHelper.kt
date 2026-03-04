package com.sers.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * ThemeHelper — Utility for saving and applying Dark Mode preference.
 * Saves the user's choice in SharedPreferences so it persists across app restarts.
 */
object ThemeHelper {

    private const val PREFS_NAME = "sers_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        applyTheme(enabled)
    }

    fun applyTheme(enabled: Boolean) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun applySavedTheme(context: Context) {
        applyTheme(isDarkModeEnabled(context))
    }
}
