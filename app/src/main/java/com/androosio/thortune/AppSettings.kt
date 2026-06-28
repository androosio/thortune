package com.androosio.thortune

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    const val PREFS_NAME = "ThorTunePrefs"
    const val JDSP_ENABLED_KEY = "jdspEnabled"
    const val SATURATION_KEY = "saturation"
    const val SECONDARY_PANEL_KEY = "secondaryPanelEnabled"

    /** Neutral display saturation — leaves the panel as the manufacturer calibrated it. */
    const val DEFAULT_SATURATION = 1.0f

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getJdspEnabled(sharedPrefs: SharedPreferences): Boolean {
        return sharedPrefs.getBoolean(JDSP_ENABLED_KEY, false)
    }

    fun setJdspEnabled(sharedPrefs: SharedPreferences, value: Boolean) {
        sharedPrefs.edit().putBoolean(JDSP_ENABLED_KEY, value).apply()
    }

    fun getSaturation(sharedPrefs: SharedPreferences): Float {
        return sharedPrefs.getFloat(SATURATION_KEY, DEFAULT_SATURATION)
    }

    fun setSaturation(sharedPrefs: SharedPreferences, value: Float) {
        sharedPrefs.edit().putFloat(SATURATION_KEY, value).apply()
    }

    /** Whether the live quick-controls panel is shown on the Thor's lower screen. On by default. */
    fun getSecondaryPanelEnabled(sharedPrefs: SharedPreferences): Boolean {
        return sharedPrefs.getBoolean(SECONDARY_PANEL_KEY, true)
    }

    fun setSecondaryPanelEnabled(sharedPrefs: SharedPreferences, value: Boolean) {
        sharedPrefs.edit().putBoolean(SECONDARY_PANEL_KEY, value).apply()
    }
}
