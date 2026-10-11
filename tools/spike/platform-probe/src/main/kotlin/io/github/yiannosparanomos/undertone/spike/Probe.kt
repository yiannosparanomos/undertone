package io.github.yiannosparanomos.undertone.spike

import android.util.Log

/**
 * Throwaway M1 probe logging.
 *
 * Privacy invariant 2 still applies here: this logs package names, decisions, API results and
 * booleans only. Never a notification title, body, contact name or phone number -- not even in a
 * build that gets deleted.
 */
object Probe {
    const val TAG = "UTSPIKE"

    fun log(
        probe: String,
        vararg pairs: Pair<String, Any?>,
    ) {
        val body = pairs.joinToString(" ") { (k, v) -> "$k=$v" }
        Log.i(TAG, "[$probe] $body")
    }
}
