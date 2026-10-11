package io.github.yiannosparanomos.undertone.core.system.watchdog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import io.github.yiannosparanomos.undertone.core.system.dnd.DndController

/**
 * Makes UnderTone fail loud.
 *
 * The dangerous failure is not "UnderTone stopped working" — it is "UnderTone stopped working and
 * left Do Not Disturb on", so the phone stays silent and the user misses calls without knowing
 * why. HyperOS will force-stop apps it dislikes, so this is a matter of when, not if (spec §9).
 *
 * On every check, if UnderTone cannot do its job, DND is handed straight back and the user is
 * told once.
 */
class Watchdog(
    private val context: Context,
    private val dnd: DndController,
    private val heartbeat: ListenerHeartbeat,
    private val notificationManager: NotificationManager,
) {
    /**
     * @return true if everything is in order; false if DND was released and the user warned.
     */
    fun check(): Boolean {
        val listenerEnabled = isListenerEnabled(context)
        val healthy = listenerEnabled && dnd.hasPolicyAccess

        if (healthy) {
            heartbeat.warningShown = false
            return true
        }

        Log.w(TAG, "Unhealthy: listenerEnabled=$listenerEnabled policyAccess=${dnd.hasPolicyAccess}")
        dnd.release()
        heartbeat.markStopped()
        if (!heartbeat.warningShown) {
            warn(listenerEnabled)
            heartbeat.warningShown = true
        }
        return false
    }

    private fun warn(listenerEnabled: Boolean) {
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "UnderTone needs attention", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shown when UnderTone has stopped being able to apply your rules."
            },
        )
        val why =
            if (!listenerEnabled) {
                "Notification access is off, so your rules are not being applied."
            } else {
                "Do Not Disturb access is off, so your rules are not being applied."
            }
        notificationManager.notify(
            NOTIFICATION_ID,
            Notification
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("UnderTone has stopped")
                .setContentText("$why Do Not Disturb has been turned off.")
                .setStyle(
                    Notification.BigTextStyle().bigText(
                        "$why Do Not Disturb has been turned off so you do not miss anything. " +
                            "Open UnderTone and run the Health check to fix it.",
                    ),
                ).setAutoCancel(true)
                .setOngoing(false)
                .build(),
        )
    }

    fun clearWarning() = notificationManager.cancel(NOTIFICATION_ID)

    companion object {
        private const val TAG = "UndertoneWatchdog"
        private const val CHANNEL_ID = "undertone.health"
        private const val NOTIFICATION_ID = 1002

        /**
         * Whether the user has granted notification access to our listener.
         *
         * Read from the secure setting rather than from the service, because the whole point is
         * to find out when the service is *not* running.
         */
        fun isListenerEnabled(
            context: Context,
            serviceClassName: String = LISTENER_CLASS,
        ): Boolean {
            val enabled =
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    ?: return false
            val expected = ComponentName(context.packageName, serviceClassName).flattenToString()
            val expectedShort = ComponentName(context.packageName, serviceClassName).flattenToShortString()
            return enabled.split(':').any { it == expected || it == expectedShort }
        }

        const val LISTENER_CLASS =
            "io.github.yiannosparanomos.undertone.core.system.notification.UndertoneListenerService"

        fun from(context: Context): Watchdog =
            Watchdog(
                context = context,
                dnd = DndController.from(context),
                heartbeat = ListenerHeartbeat(context),
                notificationManager = context.getSystemService(NotificationManager::class.java),
            )
    }
}
