package com.androosio.jamesdsptweaks

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    const val PREFS_NAME = "JamesDspTweaksPrefs"
    const val JDSP_ENABLED_KEY = "jdspEnabled"

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getJdspEnabled(sharedPrefs: SharedPreferences): Boolean {
        return sharedPrefs.getBoolean(JDSP_ENABLED_KEY, false)
    }

    fun setJdspEnabled(sharedPrefs: SharedPreferences, value: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean(JDSP_ENABLED_KEY, value)
            apply()
        }
    }
}
