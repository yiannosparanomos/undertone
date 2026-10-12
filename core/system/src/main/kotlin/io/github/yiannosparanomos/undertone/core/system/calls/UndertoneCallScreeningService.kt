package io.github.yiannosparanomos.undertone.core.system.calls

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.domain.engine.CallOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Silences incoming calls the user's ring tier does not allow.
 *
 * **Calls are never rejected** (invariant 5): `setRejectCall` and `setDisallowCall` are always
 * false, so a silenced call still reaches the dialer and still shows as missed. The M1 spike
 * confirmed `setSilenceCall(true)` suppresses the ringtone *and* vibration on Android 10, 13 and
 * 15, so nothing else needs muting (ADR 0006).
 *
 * Telecom gives this service only a moment to answer, so the work is done inline on an IO
 * dispatcher and the call is always responded to — including on failure, where it is allowed to
 * ring. A bug here must never swallow somebody's phone call.
 */
class UndertoneCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var settings: UndertoneSettingsStore
    private lateinit var pipeline: CallPipeline
    private lateinit var contacts: ContactLookup

    override fun onCreate() {
        super.onCreate()
        settings = UndertoneSettingsStore.get(applicationContext)
        pipeline =
            CallPipeline(
                settings = settings,
                history = HistoryStore.from(UndertoneDatabase.get(applicationContext)),
            )
        contacts = ContactLookup(applicationContext)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        if (number.isNullOrBlank()) {
            // Withheld number: nothing to look up, so let the tier decide on an unknown caller.
            respond(callDetails, silence = false)
            return
        }

        scope.launch {
            val silence =
                runCatching {
                    val caller = contacts.lookup(number)
                    val decision =
                        pipeline.onIncomingCall(
                            number = number,
                            isContact = caller.isContact,
                            isOnAllowList = contacts.isOnAllowList(caller, number, settings.allowList.first()),
                            at = LocalDateTime.now(),
                        )
                    // Decision and tier only: never the number, never the contact's name.
                    Log.i(TAG, "Call -> ${decision.outcome} (${decision.reason}, tier=${decision.tier})")
                    decision.outcome == CallOutcome.SILENT
                }.getOrElse {
                    // If UnderTone cannot decide, the phone rings. Failing loud beats a missed
                    // call the user never learns about.
                    Log.w(TAG, "Call screening failed, letting it ring: ${it.javaClass.simpleName}")
                    false
                }
            respond(callDetails, silence)
        }
    }

    private fun respond(
        callDetails: Call.Details,
        silence: Boolean,
    ) {
        val response =
            CallResponse
                .Builder()
                // Both false, always: UnderTone silences, it never rejects (invariant 5).
                .setDisallowCall(false)
                .setRejectCall(false)
                // The call still belongs in the log and in the missed-call notification.
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .setSilenceCall(silence)
                .build()
        runCatching { respondToCall(callDetails, response) }
            .onFailure { Log.w(TAG, "Could not respond to a call: ${it.javaClass.simpleName}") }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "UndertoneCalls"
    }
}
