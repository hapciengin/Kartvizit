package com.qrtasima.util

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "qrtasima_app_prefs"
    private const val KEY_USER_IDENTIFIER = "user_identifier"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUserIdentifier(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_IDENTIFIER, null)
    }

    fun setUserIdentifier(context: Context, identifier: String) {
        getPrefs(context).edit().putString(KEY_USER_IDENTIFIER, identifier).apply()
    }
}