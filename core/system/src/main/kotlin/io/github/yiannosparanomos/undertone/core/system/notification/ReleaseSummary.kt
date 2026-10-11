package io.github.yiannosparanomos.undertone.core.system.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * The one quiet notification that says held notifications have been released (spec §3).
 *
 * It carries a **count, never content**: "4 notifications released", not who they were from.
 * That keeps invariant 2 intact even on a lock screen, where a summary is most likely to be read
 * by someone other than the owner of the phone.
 */
class ReleaseSummary(
    private val context: Context,
    private val notificationManager: NotificationManager,
) {
    fun show(count: Int) {
        if (count <= 0) return
        ensureChannel()
        val notification =
            Notification
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(
                    if (count == 1) "1 notification released" else "$count notifications released",
                ).setAutoCancel(true)
                // No sound of its own: the channel is IMPORTANCE_LOW, and the released
                // notifications already make their own noise if they belong to Ring apps.
                .setOnlyAlertOnce(true)
                .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun clear() = notificationManager.cancel(NOTIFICATION_ID)

    private fun ensureChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Released notifications", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Tells you when held notifications have been let through."
                setShowBadge(false)
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "undertone.release"
        private const val NOTIFICATION_ID = 1001

        fun from(context: Context): ReleaseSummary =
            ReleaseSummary(context, context.getSystemService(NotificationManager::class.java))
    }
}
