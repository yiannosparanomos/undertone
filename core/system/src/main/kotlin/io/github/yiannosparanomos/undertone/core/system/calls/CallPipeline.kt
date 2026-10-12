package io.github.yiannosparanomos.undertone.core.system.calls

import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.data.settings.profileOrDefault
import io.github.yiannosparanomos.undertone.core.data.settings.toDomain
import io.github.yiannosparanomos.undertone.core.domain.engine.CallDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.CallEngine
import io.github.yiannosparanomos.undertone.core.domain.engine.IncomingCall
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.system.calendar.CalendarEventSource
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime

/**
 * One incoming call in, one decision out.
 *
 * Android-free so every path is testable. The caller is already resolved to contact and
 * allow-list flags by [ContactLookup], because the engine must not touch the contacts provider.
 *
 * The number passes through here into the encrypted database — the repeat-caller rule needs to
 * recognise the same caller ringing again — and is never logged (invariant 2).
 */
class CallPipeline(
    private val settings: UndertoneSettingsStore,
    private val history: HistoryStore,
    private val calendar: CalendarEventSource = CalendarEventSource.None,
) {
    /**
     * Decides whether a call rings and records it for the repeat-caller rule.
     *
     * The call is recorded whatever the outcome: a silenced call still counts towards the caller
     * trying again, which is the whole point of the rule.
     */
    suspend fun onIncomingCall(
        number: String,
        isContact: Boolean,
        isOnAllowList: Boolean,
        at: LocalDateTime = LocalDateTime.now(),
    ): CallDecision {
        val stored = settings.current()
        val state =
            RuleState(
                profile = stored.profileOrDefault(),
                settings = stored.settings.toDomain(),
                events = calendar.eventsAround(at),
                override = stored.override?.toDomain()?.takeIf { it.isActiveAt(at) },
            )

        val call =
            IncomingCall(
                number = number,
                isContact = isContact,
                isOnAllowList = isOnAllowList,
                at = at,
            )

        // Only the repeat window matters, so only that much history is read back.
        val window = state.settings.repeatCallers.window
        val recent = history.recentCallsFrom(number, at, window.plus(GRACE))

        val decision = CallEngine.decide(call, state, recent)
        history.record(call, decision)
        return decision
    }

    private companion object {
        /**
         * A little past the window, so a call that arrived a moment before the boundary is still
         * considered. The engine does the exact comparison; this only decides how much to load.
         */
        val GRACE: Duration = Duration.ofSeconds(30)
    }
}
