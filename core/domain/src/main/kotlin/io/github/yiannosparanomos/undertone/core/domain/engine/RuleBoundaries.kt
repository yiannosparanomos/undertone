package io.github.yiannosparanomos.undertone.core.domain.engine

import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import java.time.LocalDateTime

/**
 * When the rules next change.
 *
 * Decisions are made when a notification or a call arrives, but some things have to happen on
 * their own: Do Not Disturb turning on at 22:00, held notifications being released when an app's
 * hours start, an override ending. The platform layer schedules an exact alarm for [next] and
 * re-evaluates when it fires.
 *
 * Pure like the engines: no Android, no clock, time passed in.
 */
object RuleBoundaries {
    /** How far ahead to look. A week covers every weekly window; beyond that there is nothing new. */
    private const val LOOKAHEAD_DAYS = 8L

    /**
     * The first moment after [at] when any rule's effect could differ, or null if nothing in the
     * next week changes anything.
     *
     * Sources: the start and end of every weekly window in every app rule, the end of every
     * calendar event the user has a rule for, and the end of an active override.
     */
    fun next(
        at: LocalDateTime,
        state: RuleState,
    ): LocalDateTime? = all(at, state).firstOrNull()

    /** Every boundary after [at] within the lookahead, in order, without duplicates. */
    fun all(
        at: LocalDateTime,
        state: RuleState,
    ): List<LocalDateTime> {
        val rules = state.settings.appRules.values + state.settings.defaultRule
        return buildList {
            // Start a day back: an overnight window that began yesterday ends today.
            for (day in -1..LOOKAHEAD_DAYS) {
                val date = at.toLocalDate().plusDays(day)
                rules.forEach { rule -> rule.hours.forEach { addAll(it.boundariesOn(date)) } }
            }
            state.events.forEach {
                add(it.start)
                add(it.end)
            }
            state.override?.until?.let { add(it) }
        }.asSequence()
            .filter { it.isAfter(at) }
            .distinct()
            .sorted()
            .toList()
    }
}
