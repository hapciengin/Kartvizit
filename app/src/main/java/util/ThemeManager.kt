package com.qrtasima.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFERENCES_NAME = "theme_prefs"
    private const val PREF_KEY_THEME = "theme_option"

    enum class Theme(val key: String, val mode: Int) {
        LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
        DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeKey = prefs.getString(PREF_KEY_THEME, Theme.SYSTEM.key)
        val theme = Theme.values().find { it.key == themeKey } ?: Theme.SYSTEM
        AppCompatDelegate.setDefaultNightMode(theme.mode)
    }

    fun setTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_THEME, theme.key).apply()
        AppCompatDelegate.setDefaultNightMode(theme.mode)
    }

    fun getCurrentTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val themeKey = prefs.getString(PREF_KEY_THEME, Theme.SYSTEM.key)
        return Theme.values().find { it.key == themeKey } ?: Theme.SYSTEM
    }
}