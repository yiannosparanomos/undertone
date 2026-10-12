package io.github.yiannosparanomos.undertone.ui.common

import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * The wording the UI shows. Checked here rather than by eye, because "say it in words" is a
 * design principle and these sentences are the product.
 */
class FormattingTest {
    private val uk = Locale.UK

    // Monday 2026-10-12, 12:00.
    private val noon = LocalDateTime.of(2026, 10, 12, 12, 0)

    @Test
    fun `a moment later today is just the time`() {
        assertEquals("18:00", Formatting.moment(noon.withHour(18), noon, uk))
    }

    @Test
    fun `a moment tomorrow says tomorrow`() {
        assertEquals("tomorrow 09:00", Formatting.moment(noon.plusDays(1).withHour(9), noon, uk))
    }

    @Test
    fun `a moment later in the week names the day`() {
        assertEquals("Sat 10:00", Formatting.moment(LocalDateTime.of(2026, 10, 17, 10, 0), noon, uk))
    }

    @Test
    fun `the named day groups read as the spec writes them`() {
        assertEquals(
            "Mon-Fri 09:00-18:00",
            Formatting.window(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0)), uk),
        )
        assertEquals(
            "Sat-Sun 10:00-23:00",
            Formatting.window(WeeklyWindow(WeeklyWindow.WEEKEND, LocalTime.of(10, 0), LocalTime.of(23, 0)), uk),
        )
        assertEquals(
            "Every day 22:00-07:00",
            Formatting.window(WeeklyWindow(WeeklyWindow.EVERY_DAY, LocalTime.of(22, 0), LocalTime.of(7, 0)), uk),
        )
    }

    @Test
    fun `an unusual set of days is listed`() {
        assertEquals(
            "Mon Wed 09:00-10:00",
            Formatting.window(
                WeeklyWindow(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(9, 0), LocalTime.of(10, 0)),
                uk,
            ),
        )
    }

    @Test
    fun `no hours at all reads as Always`() {
        assertEquals("Always", Formatting.windows(emptyList(), uk))
    }

    @Test
    fun `several windows are listed in order`() {
        val windows =
            listOf(
                WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                WeeklyWindow(WeeklyWindow.WEEKEND, LocalTime.of(10, 0), LocalTime.of(23, 0)),
            )
        assertEquals("Mon-Fri 09:00-18:00, Sat-Sun 10:00-23:00", Formatting.windows(windows, uk))
    }
}
