package io.github.yiannosparanomos.undertone.core.domain.engine

import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.CalendarEvent
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class RuleBoundariesTest {
    // Monday 2026-10-12.
    private val monday = LocalDateTime.of(2026, 10, 12, 0, 0)

    private fun stateWith(
        hours: List<WeeklyWindow> = emptyList(),
        events: List<CalendarEvent> = emptyList(),
        override: Override? = null,
    ) = RuleState(
        profile = Profile.MY_RULES,
        settings =
            UndertoneSettings(
                appRules =
                    mapOf(
                        "com.example.chat" to
                            AppRule("com.example.chat", Level.RING, Level.HIDDEN, hours),
                    ),
            ),
        events = events,
        override = override,
    )

    @Test
    fun `with no rules at all nothing is scheduled`() {
        assertNull(RuleBoundaries.next(monday.plusHours(10), stateWith()))
    }

    @Test
    fun `the next boundary is the start of today's window`() {
        val state = stateWith(listOf(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))))
        assertEquals(monday.withHour(9), RuleBoundaries.next(monday.withHour(7), state))
    }

    @Test
    fun `inside a window the next boundary is its end`() {
        val state = stateWith(listOf(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))))
        assertEquals(monday.withHour(18), RuleBoundaries.next(monday.withHour(12), state))
    }

    @Test
    fun `after the last window today the next boundary is tomorrow's start`() {
        val state = stateWith(listOf(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))))
        assertEquals(monday.plusDays(1).withHour(9), RuleBoundaries.next(monday.withHour(20), state))
    }

    @Test
    fun `a weekend-only window is found across the week`() {
        val state = stateWith(listOf(WeeklyWindow(WeeklyWindow.WEEKEND, LocalTime.of(10, 0), LocalTime.of(12, 0))))
        // From Monday the next change is Saturday at 10:00.
        assertEquals(
            LocalDateTime.of(2026, 10, 17, 10, 0),
            RuleBoundaries.next(monday.withHour(9), state),
        )
    }

    @Test
    fun `an overnight window ends the following morning`() {
        val state = stateWith(listOf(WeeklyWindow(setOf(DayOfWeek.SUNDAY), LocalTime.of(22, 0), LocalTime.of(7, 0))))
        // Sunday night ran into Monday: the next boundary from Monday 02:00 is Monday 07:00.
        assertEquals(monday.withHour(7), RuleBoundaries.next(monday.withHour(2), state))
    }

    @Test
    fun `a calendar event contributes both its start and its end`() {
        val event =
            CalendarEvent(
                id = "e1",
                seriesId = null,
                calendarId = "work",
                title = "Standup",
                start = monday.withHour(9).withMinute(30),
                end = monday.withHour(9).withMinute(45),
            )
        val boundaries = RuleBoundaries.all(monday.withHour(8), stateWith(events = listOf(event)))
        assertEquals(listOf(event.start, event.end), boundaries)
    }

    @Test
    fun `an override's end is a boundary`() {
        val until = monday.withHour(13).withMinute(20)
        assertEquals(until, RuleBoundaries.next(monday.withHour(12), stateWith(override = Override.Quiet(until))))
    }

    @Test
    fun `boundaries already past are not returned`() {
        val state = stateWith(listOf(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))))
        assertTrue(RuleBoundaries.all(monday.withHour(12), state).all { it.isAfter(monday.withHour(12)) })
    }

    @Test
    fun `the exact boundary moment is not its own next boundary`() {
        // At 18:00 sharp the next thing to wake for is tomorrow, not this instant again:
        // otherwise the alarm would re-fire forever.
        val state = stateWith(listOf(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))))
        assertEquals(monday.plusDays(1).withHour(9), RuleBoundaries.next(monday.withHour(18), state))
    }

    @Test
    fun `duplicate boundaries from several apps collapse to one`() {
        val window = WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))
        val state =
            RuleState(
                profile = Profile.MY_RULES,
                settings =
                    UndertoneSettings(
                        appRules =
                            mapOf(
                                "a" to AppRule("a", Level.RING, Level.HIDDEN, listOf(window)),
                                "b" to AppRule("b", Level.RING, Level.HIDDEN, listOf(window)),
                            ),
                    ),
            )
        val todays = RuleBoundaries.all(monday.withHour(8), state).filter { it.toLocalDate() == monday.toLocalDate() }
        assertEquals(listOf(monday.withHour(9), monday.withHour(18)), todays)
    }

    @Test
    fun `the all-apps default rule's hours count too`() {
        val state =
            RuleState(
                profile = Profile.MY_RULES,
                settings =
                    UndertoneSettings(
                        defaultRule =
                            AppRule(
                                AppRule.ALL_APPS,
                                Level.RING,
                                Level.SILENT,
                                listOf(WeeklyWindow(WeeklyWindow.EVERY_DAY, LocalTime.of(8, 0), LocalTime.of(23, 0))),
                            ),
                    ),
            )
        assertEquals(monday.withHour(8), RuleBoundaries.next(monday.withHour(7), state))
    }
}
