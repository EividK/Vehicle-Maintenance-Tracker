package com.example.vehiclehealth.utils

import android.content.Context

object SettingsManager {
    private const val PREFS = "vehicle_prefs"
    private const val KEY_NOTIF = "service_reminders_enabled"

    fun isServiceRemindersEnabled(ctx: Context): Boolean =
        ctx
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIF, true)

    fun setServiceRemindersEnabled(ctx: Context, enabled: Boolean) {
        ctx
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIF, enabled)
            .apply()
    }
}
