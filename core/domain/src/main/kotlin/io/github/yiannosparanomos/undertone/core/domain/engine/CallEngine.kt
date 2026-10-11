package io.github.yiannosparanomos.undertone.core.domain.engine

import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatGroup
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.stricterOf
import java.time.LocalDateTime

/**
 * An incoming call as the platform layer reports it. Whether the caller is on the allow list
 * (starred contacts, or the expanded list) is resolved by the platform layer.
 */
data class IncomingCall(
    val number: String,
    val isContact: Boolean,
    val isOnAllowList: Boolean,
    val at: LocalDateTime,
)

/** A call that already happened, used by the repeat-caller rule. */
data class PastCall(
    val number: String,
    val at: LocalDateTime,
)

/**
 * Decides whether a call rings. Calls are never rejected: a call that doesn't ring
 * comes through silently and shows as a missed call.
 *
 * 1. The ring tier: an active override's tier, otherwise the stricter of the default
 *    tier and any running calendar rule's tier.
 * 2. If the tier doesn't allow the caller, the repeat-caller rule can still let it ring.
 */
object CallEngine {
    fun decide(
        call: IncomingCall,
        state: RuleState,
        recentCalls: List<PastCall>,
    ): CallDecision {
        when (state.profile) {
            Profile.ALLOW_ALL -> return CallDecision(CallOutcome.PASS_THROUGH, DecisionReason.ALLOW_ALL_PROFILE)
            Profile.SYSTEM_DND -> return CallDecision(CallOutcome.PASS_THROUGH, DecisionReason.SYSTEM_DND_PROFILE)
            Profile.MY_RULES -> Unit
        }

        val tier = effectiveTier(call.at, state)
        if (tierAllows(tier, call)) {
            return CallDecision(CallOutcome.RING, DecisionReason.RING_TIER, tier)
        }
        if (isRepeatCall(call, state, recentCalls)) {
            return CallDecision(CallOutcome.RING, DecisionReason.REPEAT_CALLER, tier)
        }
        return CallDecision(CallOutcome.SILENT, DecisionReason.TIER_SILENCED, tier)
    }

    fun effectiveTier(
        at: LocalDateTime,
        state: RuleState,
    ): CallTier =
        when (val override = state.activeOverride(at)) {
            is Override.Quiet -> override.callTier
            is Override.LetThrough -> override.callTier
            null ->
                state
                    .activeCalendarRules(at)
                    .map { it.callTier }
                    .fold(state.settings.defaultCallTier, ::stricterOf)
        }

    private fun tierAllows(
        tier: CallTier,
        call: IncomingCall,
    ): Boolean =
        when (tier) {
            CallTier.EVERYONE -> true
            CallTier.CONTACTS -> call.isContact
            CallTier.ALLOW_LIST -> call.isOnAllowList
            CallTier.NOBODY -> false
        }

    private fun isRepeatCall(
        call: IncomingCall,
        state: RuleState,
        recentCalls: List<PastCall>,
    ): Boolean {
        val repeat = state.settings.repeatCallers
        if (!repeat.enabled) return false
        val inGroup =
            when (repeat.group) {
                RepeatGroup.EVERYONE -> true
                RepeatGroup.ALLOW_LIST -> call.isOnAllowList
            }
        if (!inGroup) return false

        val windowStart = call.at.minus(repeat.window)
        val number = normalize(call.number)
        val earlierCalls =
            recentCalls.count {
                normalize(it.number) == number && !it.at.isBefore(windowStart) && it.at.isBefore(call.at)
            }
        return earlierCalls >= repeat.callsNeeded - 1
    }

    /** Compares numbers by digits only, so "+30 210 123 4567" matches "+302101234567". */
    private fun normalize(number: String): String = number.filter { it.isDigit() }
}
