package me.jdevlin.reelkilla

import android.content.Context

object NextOpenTimeStore {
    private const val PREFS_NAME = "reelkilla_prefs"
    private const val KEY_NEXT_OPEN_AT = "next_open_at"

    fun getNextOpenAt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getLong(KEY_NEXT_OPEN_AT, 0L)
        return if (value <= 0L) {
            0L
        } else {
            value
        }
    }

    fun setNextOpenAt(context: Context, timestampMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_NEXT_OPEN_AT, timestampMillis)
            .apply()
    }
}
