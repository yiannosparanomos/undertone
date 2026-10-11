package io.github.yiannosparanomos.undertone.spike

import android.content.Context

/** Flags the adb receiver flips, read by the services. */
object ProbeState {
    private const val PREFS = "probe"
    private const val KEY_SILENCE = "silence_calls"

    fun silenceCalls(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SILENCE, true)

    fun setSilenceCalls(
        context: Context,
        value: Boolean,
    ) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SILENCE, value)
            .apply()
    }
}
