package io.github.yiannosparanomos.undertone.core.system.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.system.calendar.CalendarEventSource
import io.github.yiannosparanomos.undertone.core.system.dnd.DndController
import io.github.yiannosparanomos.undertone.core.system.schedule.BoundaryAlarms
import io.github.yiannosparanomos.undertone.core.system.sound.SoundPlayer
import io.github.yiannosparanomos.undertone.core.system.watchdog.ListenerHeartbeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Receives every notification and carries out what the engine decides.
 *
 * Logging here is package names, decisions and counts only — never a title, a body or a key,
 * since a key embeds the posting app's own tag (invariant 2).
 */
class UndertoneListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var settings: UndertoneSettingsStore
    private lateinit var history: HistoryStore
    private lateinit var pipeline: NotificationPipeline
    private lateinit var dnd: DndController
    private lateinit var summary: ReleaseSummary
    private lateinit var alarms: BoundaryAlarms

    private val snoozeStrategy = SnoozeStrategy.forThisDevice()

    private val host =
        object : SnoozeHost {
            override fun snoozeNotification(
                key: String,
                durationMs: Long,
            ) = this@UndertoneListenerService.snoozeNotification(key, durationMs)

            override fun snoozedKeys(): List<String> =
                runCatching { snoozedNotifications?.map { it.key } ?: emptyList() }.getOrDefault(emptyList())
        }

    override fun onCreate() {
        super.onCreate()
        settings = UndertoneSettingsStore.get(applicationContext)
        history = HistoryStore.from(UndertoneDatabase.get(applicationContext))
        dnd = DndController.from(applicationContext)
        summary = ReleaseSummary.from(applicationContext)
        alarms = BoundaryAlarms.from(applicationContext)
        pipeline =
            NotificationPipeline(
                settings = settings,
                history = history,
                calendar = CalendarEventSource.None,
                ringer = SoundPlayer.from(applicationContext)::play,
            )
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Listener connected")
        ListenerHeartbeat(applicationContext).markAlive()
        scope.launch {
            dnd.apply(settings.profile.first())
            alarms.scheduleNext(pipeline.ruleState(LocalDateTime.now()))
            releaseDue()
        }
    }

    override fun onListenerDisconnected() {
        // Losing the listener means UnderTone can no longer keep its promises, so it must not
        // leave the phone silenced: fail loud (spec §9).
        Log.w(TAG, "Listener disconnected; releasing DND")
        ListenerHeartbeat(applicationContext).markStopped()
        dnd.release()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification.extras
        val postedAt =
            Instant.ofEpochMilli(sbn.postTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val isOngoing =
            sbn.isOngoing || sbn.notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE != 0

        scope.launch {
            val outcome =
                pipeline.onPosted(
                    packageName = packageName,
                    notificationKey = sbn.key,
                    title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString(),
                    text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString(),
                    postedAt = postedAt,
                    isOngoing = isOngoing,
                )
            // Package name and decision only.
            Log.i(TAG, "$packageName -> ${outcome.decision.outcome} (${outcome.decision.reason})")

            when (val action = outcome.action) {
                is NotificationAction.Leave, NotificationAction.PassThrough -> Unit
                is NotificationAction.Cancel -> cancelNotification(sbn.key)
                is NotificationAction.Snooze -> snoozeStrategy.snooze(host, sbn.key, action.durationMs)
            }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return
        val mapped =
            when (reason) {
                REASON_CANCEL -> NotificationPipeline.RemovalReason.SWIPED_AWAY
                REASON_CLICK -> NotificationPipeline.RemovalReason.OPENED
                else -> NotificationPipeline.RemovalReason.OTHER
            }
        scope.launch { pipeline.onRemoved(packageName, sbn.key, mapped) }
    }

    /** Releases every held notification whose moment has come, with one summary. */
    private suspend fun releaseDue(at: LocalDateTime = LocalDateTime.now()) {
        val due = history.heldDueAt(at)
        if (due.isEmpty()) return
        var released = 0
        due.forEach { held ->
            // On API 33+ the original comes back with its actions intact; below that the system
            // keeps it snoozed and the shade gains nothing, so the row is simply dropped and the
            // summary tells the user the count (ADR 0006).
            if (snoozeStrategy.releaseEarly(host, held.notificationKey)) released++
            history.releaseHeld(held.notificationKey)
        }
        Log.i(TAG, "Released $released of ${due.size} held notifications")
        summary.show(due.size)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "UndertoneListener"
    }
}
