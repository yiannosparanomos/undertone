package io.github.yiannosparanomos.undertone.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * The adb driver. Every probe step is one broadcast, so the whole spike runs headless:
 *
 *   adb shell am broadcast -a io.github.yiannosparanomos.undertone.spike.CMD \
 *     -n io.github.yiannosparanomos.undertone.spike/.ProbeReceiver --es cmd post
 */
class ProbeReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (val cmd = intent.getStringExtra("cmd")) {
            "post" -> post(context, intent.getIntExtra("id", 1))
            "snooze" ->
                ProbeListenerService.instance?.snooze(intent.getLongExtra("ms", 600_000L))
                    ?: Probe.log("snooze", "error" to "listener not connected")
            "release-early" ->
                ProbeListenerService.instance?.releaseEarly()
                    ?: Probe.log("release-early", "error" to "listener not connected")
            "snoozed-list" ->
                ProbeListenerService.instance?.reportSnoozed()
                    ?: Probe.log("snoozed-list", "error" to "listener not connected")
            "release-zero" ->
                ProbeListenerService.instance?.releaseZero()
                    ?: Probe.log("release-zero", "error" to "listener not connected")
            "cancel-snoozed" ->
                ProbeListenerService.instance?.cancelSnoozed()
                    ?: Probe.log("cancel-snoozed", "error" to "listener not connected")
            "dnd" -> dnd(context, intent.getStringExtra("filter") ?: "all")
            "sound" -> SoundProbe.play(context, intent.getStringExtra("usage") ?: "notification")
            "silence-calls" -> {
                val value = intent.getBooleanExtra("value", true)
                ProbeState.setSilenceCalls(context, value)
                Probe.log("config", "silenceCalls" to value)
            }
            "state" ->
                Probe.log(
                    "state",
                    "listenerConnected" to (ProbeListenerService.instance != null),
                    "hasKey" to (ProbeListenerService.lastOwnKey != null),
                )
            else -> Probe.log("cmd", "unknown" to cmd)
        }
    }

    /**
     * Sets DND the way UnderTone's own controller will: setInterruptionFilter, not a shell
     * command. Works on every API level the app supports, unlike `cmd notification set_dnd`.
     */
    private fun dnd(
        context: Context,
        filter: String,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Probe.log("dnd", "error" to "no policy access")
            return
        }
        val value =
            when (filter) {
                "priority" -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                "none" -> NotificationManager.INTERRUPTION_FILTER_NONE
                "alarms" -> NotificationManager.INTERRUPTION_FILTER_ALARMS
                else -> NotificationManager.INTERRUPTION_FILTER_ALL
            }
        nm.setInterruptionFilter(value)
        Probe.log("dnd", "requested" to filter, "readBack" to nm.currentInterruptionFilter)
    }

    private fun post(
        context: Context,
        id: Int,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Probe", NotificationManager.IMPORTANCE_DEFAULT),
        )
        // Fixed placeholder strings: the probe never posts anything resembling real user content.
        val notification =
            Notification
                .Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("probe")
                .setContentText("probe-$id")
                .setAutoCancel(false)
                .build()
        nm.notify(id, notification)
        Probe.log("post", "id" to id)
    }

    private companion object {
        const val CHANNEL = "probe"
    }
}
