package io.github.yiannosparanomos.undertone.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A recurring weekly time window, e.g. Mon–Fri 09:00–18:00.
 *
 * If [end] is not after [start] the window runs overnight: it starts on one of [days]
 * and ends the following morning (e.g. Fri 22:00 – Sat 07:00).
 */
data class WeeklyWindow(
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
) {
    init {
        require(days.isNotEmpty()) { "A weekly window needs at least one day" }
    }

    private val overnight: Boolean get() = !end.isAfter(start)

    /** True if [at] falls inside this window. Start is inclusive, end is exclusive. */
    fun contains(at: LocalDateTime): Boolean {
        val time = at.toLocalTime()
        val day = at.dayOfWeek
        return if (!overnight) {
            day in days && !time.isBefore(start) && time.isBefore(end)
        } else {
            (day in days && !time.isBefore(start)) ||
                (day.minus(1) in days && time.isBefore(end))
        }
    }

    /** Every start and end instant of this window on [date]. Used to find when a level changes. */
    fun boundariesOn(date: LocalDate): List<LocalDateTime> {
        if (date.dayOfWeek !in days) return emptyList()
        val startAt = date.atTime(start)
        val endAt = if (overnight) date.plusDays(1).atTime(end) else date.atTime(end)
        return listOf(startAt, endAt)
    }

    companion object {
        val WEEKDAYS: Set<DayOfWeek> =
            setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            )
        val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    }
}
