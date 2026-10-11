package io.github.yiannosparanomos.undertone.core.system.notification

import android.os.Build

/**
 * The part of the notification listener a snooze strategy needs. Narrow on purpose, so the
 * strategies can be tested without a live `NotificationListenerService`.
 */
interface SnoozeHost {
    fun snoozeNotification(
        key: String,
        durationMs: Long,
    )

    /** Keys the system is currently holding snoozed for us. */
    fun snoozedKeys(): List<String>
}

/**
 * How a held notification is put away and brought back.
 *
 * The M1 spike (ADR 0006) found that re-snoozing an already-snoozed notification with a 1 ms
 * duration re-posts the **original** on API 33 and 35 — keeping its actions and `PendingIntent`s,
 * so tapping a released notification still opens the source app at the right place. On API 29
 * nothing releases early at all; there the caller has to post a copy instead, and the copy
 * cannot carry the original's actions.
 *
 * The boundary between 30 and 32 was never measured, so [forThisDevice] takes the fallback below
 * 33. The fallback works everywhere, so erring that way costs fidelity, never correctness.
 */
sealed interface SnoozeStrategy {
    /** Puts a notification away until [durationMs] from now. */
    fun snooze(
        host: SnoozeHost,
        key: String,
        durationMs: Long,
    )

    /**
     * Brings it back now.
     *
     * @return true if the original notification was restored. False means the caller must post a
     *   copy itself: there is no way to release early on this Android version.
     */
    fun releaseEarly(
        host: SnoozeHost,
        key: String,
    ): Boolean

    /** API 33+: re-snoozing with a near-zero duration re-posts the original. */
    data object ReSnoozeToRelease : SnoozeStrategy {
        private const val IMMEDIATE_MS = 1L

        override fun snooze(
            host: SnoozeHost,
            key: String,
            durationMs: Long,
        ) = host.snoozeNotification(key, durationMs)

        override fun releaseEarly(
            host: SnoozeHost,
            key: String,
        ): Boolean {
            if (key !in host.snoozedKeys()) return false
            host.snoozeNotification(key, IMMEDIATE_MS)
            return true
        }
    }

    /** API 29-32: the notification stays snoozed; the caller re-posts a copy. */
    data object RepostCopy : SnoozeStrategy {
        override fun snooze(
            host: SnoozeHost,
            key: String,
            durationMs: Long,
        ) = host.snoozeNotification(key, durationMs)

        override fun releaseEarly(
            host: SnoozeHost,
            key: String,
        ): Boolean = false
    }

    companion object {
        /** API level from which re-snoozing releases a notification early (ADR 0006). */
        const val EARLY_RELEASE_FROM_SDK = Build.VERSION_CODES.TIRAMISU

        fun forThisDevice(sdkInt: Int = Build.VERSION.SDK_INT): SnoozeStrategy =
            if (sdkInt >= EARLY_RELEASE_FROM_SDK) ReSnoozeToRelease else RepostCopy
    }
}
