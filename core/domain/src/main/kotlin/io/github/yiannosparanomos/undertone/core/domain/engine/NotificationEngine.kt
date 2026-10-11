package io.github.yiannosparanomos.undertone.core.domain.engine

import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.stricterOf
import java.time.LocalDateTime

/**
 * Decides what happens to a notification. Pure function of its inputs: no Android, no clock.
 *
 * Precedence inside My rules (strongest first):
 * 1. A blocked app stays blocked, whatever else applies.
 * 2. An active manual override.
 * 3. Calendar rules and the app's weekly schedule: the stricter of the two wins.
 */
object NotificationEngine {
    /** How far ahead to look for the moment a hidden notification can be released. */
    private const val RELEASE_LOOKAHEAD_DAYS = 8L

    fun decide(
        packageName: String,
        at: LocalDateTime,
        state: RuleState,
    ): NotificationDecision {
        val decision = decideAt(packageName, at, state)
        return if (decision.outcome == NotificationOutcome.HIDDEN) {
            decision.copy(releaseAt = findReleaseTime(packageName, at, state))
        } else {
            decision
        }
    }

    private fun decideAt(
        packageName: String,
        at: LocalDateTime,
        state: RuleState,
    ): NotificationDecision {
        when (state.profile) {
            Profile.ALLOW_ALL ->
                return NotificationDecision(NotificationOutcome.PASS_THROUGH, DecisionReason.ALLOW_ALL_PROFILE)
            Profile.SYSTEM_DND ->
                return NotificationDecision(NotificationOutcome.PASS_THROUGH, DecisionReason.SYSTEM_DND_PROFILE)
            Profile.MY_RULES -> Unit
        }

        val rule = state.settings.ruleFor(packageName)
        val scheduled = rule.levelAt(at)
        if (scheduled == Level.BLOCKED) {
            return NotificationDecision(NotificationOutcome.BLOCKED, DecisionReason.BLOCKED_APP)
        }

        when (val override = state.activeOverride(at)) {
            is Override.Quiet -> {
                val level = if (packageName in override.ringApps) Level.RING else override.level
                return NotificationDecision(NotificationOutcome.of(level), DecisionReason.MANUAL_OVERRIDE)
            }
            is Override.LetThrough ->
                return NotificationDecision(NotificationOutcome.RING, DecisionReason.MANUAL_OVERRIDE)
            null -> Unit
        }

        val scheduleReason =
            if (packageName in state.settings.appRules) {
                DecisionReason.APP_SCHEDULE
            } else {
                DecisionReason.ALL_APPS_DEFAULT
            }
        val calendarLevel =
            state
                .activeCalendarRules(at)
                .map { it.levelFor(packageName) }
                .reduceOrNull(::stricterOf)

        return if (calendarLevel != null && stricterOf(calendarLevel, scheduled) != scheduled) {
            NotificationDecision(NotificationOutcome.of(calendarLevel), DecisionReason.CALENDAR_RULE)
        } else {
            NotificationDecision(NotificationOutcome.of(scheduled), scheduleReason)
        }
    }

    /** The first moment after [at] when this app's notifications are no longer hidden. */
    private fun findReleaseTime(
        packageName: String,
        at: LocalDateTime,
        state: RuleState,
    ): LocalDateTime? {
        val rule = state.settings.ruleFor(packageName)
        val candidates =
            buildList {
                // Start one day back: an overnight window that began yesterday ends today.
                for (day in -1..RELEASE_LOOKAHEAD_DAYS) {
                    val date = at.toLocalDate().plusDays(day)
                    rule.hours.forEach { addAll(it.boundariesOn(date)) }
                }
                state.events.forEach { add(it.end) }
                state.activeOverride(at)?.let { add(it.until) }
            }
        return candidates
            .filter { it.isAfter(at) }
            .distinct()
            .sorted()
            .firstOrNull { decideAt(packageName, it, state).outcome != NotificationOutcome.HIDDEN }
    }
}
