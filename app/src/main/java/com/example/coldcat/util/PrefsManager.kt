package com.example.coldcat.util

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {

    private const val PREFS_NAME = "coldcat_prefs"
    private const val KEY_VPN_ENABLED = "vpn_enabled"
    private const val KEY_ACCESSIBILITY_PROMPTED = "accessibility_prompted"
    private const val KEY_BLOCK_ACTIVE = "block_active"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setVpnEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_VPN_ENABLED, enabled).apply()

    fun isVpnEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VPN_ENABLED, false)

    fun setAccessibilityPrompted(context: Context, prompted: Boolean) =
        prefs(context).edit().putBoolean(KEY_ACCESSIBILITY_PROMPTED, prompted).apply()

    fun wasAccessibilityPrompted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACCESSIBILITY_PROMPTED, false)

    fun setBlockActive(context: Context, active: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_ACTIVE, active).apply()

    fun isBlockActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_ACTIVE, false)
}