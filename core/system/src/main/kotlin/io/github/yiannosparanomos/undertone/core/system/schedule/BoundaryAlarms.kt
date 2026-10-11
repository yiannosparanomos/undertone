package io.github.yiannosparanomos.undertone.core.system.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.yiannosparanomos.undertone.core.domain.engine.RuleBoundaries
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Wakes UnderTone at the moment the rules next change — 18:00 when work hours end, the end of a
 * calendar event, the end of an override — so DND and the held inbox are re-evaluated on time
 * rather than at the next notification.
 *
 * Exact alarms because the whole point is the boundary: a minute late and the phone has already
 * rung, or stayed silent when it should not have. Only one alarm is ever outstanding; each firing
 * schedules the next one, so there is nothing to clean up and nothing to drift.
 */
class BoundaryAlarms(
    private val context: Context,
    private val alarmManager: AlarmManager,
) {
    /** Whether the system will honour an exact alarm. False on API 31+ until the user allows it. */
    val canScheduleExact: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

    /**
     * Schedules a wake-up for the next boundary in [state], replacing any pending one.
     *
     * @return the moment scheduled, or null when nothing in the next week changes anything.
     */
    fun scheduleNext(
        state: RuleState,
        at: LocalDateTime = LocalDateTime.now(),
    ): LocalDateTime? {
        val next =
            RuleBoundaries.next(at, state) ?: run {
                cancel()
                return null
            }
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = pendingIntent()

        if (canScheduleExact) {
            // setExactAndAllowWhileIdle survives Doze, which is where a sleeping phone spends
            // most of the night -- exactly when "hours start at 07:00" has to work.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            // Without the exact-alarm permission an inexact alarm is still far better than
            // nothing; the Health check screen asks the user to grant it.
            Log.w(TAG, "No exact alarm permission; falling back to an inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
        Log.i(TAG, "Next rule boundary at $next")
        return next
    }

    fun cancel() = alarmManager.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(ACTION_BOUNDARY).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val TAG = "UndertoneAlarms"
        private const val REQUEST_CODE = 2001

        /** Broadcast when a rule boundary is reached. Internal to the app: never exported. */
        const val ACTION_BOUNDARY = "io.github.yiannosparanomos.undertone.RULE_BOUNDARY"

        fun from(context: Context): BoundaryAlarms =
            BoundaryAlarms(context, context.getSystemService(AlarmManager::class.java))
    }
}
