package io.github.yiannosparanomos.undertone.core.system.watchdog

import android.content.Context

/**
 * A tiny record of whether the notification listener is running.
 *
 * Deliberately not in the encrypted database: the watchdog has to read this after a reboot or a
 * force-stop, when opening the database would mean unwrapping the Keystore key for no reason.
 * It holds one boolean and one timestamp, no content.
 */
class ListenerHeartbeat(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val wasAlive: Boolean
        get() = prefs.getBoolean(KEY_ALIVE, false)

    val lastSeenAtMillis: Long
        get() = prefs.getLong(KEY_LAST_SEEN, 0L)

    fun markAlive() =
        prefs
            .edit()
            .putBoolean(KEY_ALIVE, true)
            .putLong(KEY_LAST_SEEN, System.currentTimeMillis())
            .apply()

    /** The listener went away on purpose or otherwise; the watchdog will act on this. */
    fun markStopped() = prefs.edit().putBoolean(KEY_ALIVE, false).apply()

    /** Set when the watchdog has told the user something went wrong, so it only says it once. */
    var warningShown: Boolean
        get() = prefs.getBoolean(KEY_WARNED, false)
        set(value) = prefs.edit().putBoolean(KEY_WARNED, value).apply()

    private companion object {
        const val FILE = "undertone-heartbeat"
        const val KEY_ALIVE = "alive"
        const val KEY_LAST_SEEN = "lastSeen"
        const val KEY_WARNED = "warned"
    }
}
