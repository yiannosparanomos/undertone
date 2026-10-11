package io.github.yiannosparanomos.undertone.spike

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Probe 3: can a snoozed notification be released early, or is the fallback (cancel + re-post a
 * copy) needed?
 *
 * The listener records the key of the probe's own notifications so adb commands can act on them
 * without anyone having to read a key off the screen.
 */
class ProbeListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        instance = this
        Probe.log("listener", "connected" to true, "sdk" to android.os.Build.VERSION.SDK_INT)
    }

    override fun onListenerDisconnected() {
        Probe.log("listener", "connected" to false)
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) lastOwnKey = sbn.key
        // Package and id only: no title, no text.
        Probe.log(
            "posted",
            "pkg" to sbn.packageName,
            "id" to sbn.id,
            "ownNotification" to (sbn.packageName == packageName),
        )
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        Probe.log("removed", "pkg" to sbn.packageName, "id" to sbn.id, "reason" to reason)
    }

    fun snooze(durationMs: Long) {
        val key = lastOwnKey
        if (key == null) {
            Probe.log("snooze", "error" to "no key recorded yet")
            return
        }
        snoozeNotification(key, durationMs)
        Probe.log("snooze", "requested" to true, "durationMs" to durationMs)
    }

    /**
     * The open question: there is no public "unsnooze". Two candidates are tried --
     * (a) calling snoozeNotification again with a near-zero duration,
     * (b) reading getSnoozedNotifications() to see whether the system still holds it.
     */
    fun releaseEarly() {
        val key = lastOwnKey
        if (key == null) {
            Probe.log("release-early", "error" to "no key recorded yet")
            return
        }
        val snoozedBefore = snoozedKeys()
        snoozeNotification(key, 1L)
        Probe.log(
            "release-early",
            "attempt" to "re-snooze-1ms",
            "snoozedCountBefore" to snoozedBefore.size,
            "keyWasSnoozed" to snoozedBefore.contains(key),
        )
    }

    /** Candidate (c): snooze with 0 ms. */
    fun releaseZero() {
        val key = lastOwnKey ?: return Probe.log("release-zero", "error" to "no key")
        val before = snoozedKeys()
        snoozeNotification(key, 0L)
        Probe.log("release-zero", "attempt" to "re-snooze-0ms", "wasSnoozed" to before.contains(key))
    }

    /** Candidate (d): cancel the snoozed one outright, then the caller re-posts a copy. */
    fun cancelSnoozed() {
        val key = lastOwnKey ?: return Probe.log("cancel-snoozed", "error" to "no key")
        val before = snoozedKeys()
        cancelNotification(key)
        Probe.log("cancel-snoozed", "attempt" to "cancelNotification", "wasSnoozed" to before.contains(key))
    }

    fun reportSnoozed() {
        val keys = snoozedKeys()
        Probe.log("snoozed-list", "count" to keys.size, "containsOwn" to keys.contains(lastOwnKey))
    }

    private fun snoozedKeys(): List<String> =
        try {
            snoozedNotifications?.map { it.key } ?: emptyList()
        } catch (e: SecurityException) {
            Probe.log("snoozed-list", "error" to e.javaClass.simpleName)
            emptyList()
        }

    companion object {
        @Volatile
        var instance: ProbeListenerService? = null
            private set

        @Volatile
        var lastOwnKey: String? = null
            private set
    }
}
