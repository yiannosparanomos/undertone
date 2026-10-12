package io.github.yiannosparanomos.undertone.core.system.watchdog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.data.settings.profileOrDefault
import io.github.yiannosparanomos.undertone.core.data.settings.toDomain
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.system.dnd.DndController
import io.github.yiannosparanomos.undertone.core.system.schedule.BoundaryAlarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Wakes UnderTone after a reboot and at every rule boundary, and runs the watchdog each time.
 *
 * Boot matters because the user's rules have to apply again without them opening the app
 * (device checklist D4), and because an alarm scheduled before the reboot is gone.
 */
class UndertoneReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        Log.i(TAG, "Received $action")

        val appContext = context.applicationContext
        val healthy = Watchdog.from(appContext).check()
        if (!healthy) return

        // goAsync would be wrong here: this is quick, and a dropped broadcast is better than a
        // receiver held open across a reboot storm. The scope is deliberately short-lived.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching {
                val settings = UndertoneSettingsStore.get(appContext)
                val stored = settings.current()
                val now = LocalDateTime.now()
                val state =
                    RuleState(
                        profile = stored.profileOrDefault(),
                        settings = stored.settings.toDomain(),
                        override = stored.override?.toDomain()?.takeIf { it.isActiveAt(now) },
                    )

                DndController.from(appContext).apply(state.profile)
                BoundaryAlarms.from(appContext).scheduleNext(state, now)

                // Retention is swept here rather than on a timer of its own: the boundary alarm
                // already fires at least daily for any real set of rules, and a reboot always
                // fires it.
                val history = HistoryStore.from(UndertoneDatabase.get(appContext))
                val expired = history.deleteExpired(now, settings.retentionDays.first())
                if (expired.total > 0) Log.i(TAG, "Retention removed ${expired.total} rows")
            }.onFailure { Log.w(TAG, "Boundary work failed: ${it.javaClass.simpleName}") }
        }
    }

    private companion object {
        const val TAG = "UndertoneReceiver"
    }
}
