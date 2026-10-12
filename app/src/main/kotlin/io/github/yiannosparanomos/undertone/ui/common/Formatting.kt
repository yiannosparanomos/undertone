package io.github.yiannosparanomos.undertone.ui.common

import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Turning rules and times into the plain sentences the UI direction asks for.
 *
 * Kept out of the composables so the wording can be checked by a test rather than by eye.
 */
object Formatting {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    /** "Mon 09:00", for a moment the user is waiting for. */
    fun moment(
        at: LocalDateTime,
        now: LocalDateTime,
        locale: Locale = Locale.getDefault(),
    ): String {
        val time = at.format(timeFormat)
        return when {
            at.toLocalDate() == now.toLocalDate() -> time
            at.toLocalDate() == now.toLocalDate().plusDays(1) -> "tomorrow $time"
            else -> "${at.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} $time"
        }
    }

    /** "Mon-Fri 09:00-18:00". */
    fun window(
        window: WeeklyWindow,
        locale: Locale = Locale.getDefault(),
    ): String {
        val days =
            when (window.days) {
                WeeklyWindow.WEEKDAYS -> "Mon-Fri"
                WeeklyWindow.WEEKEND -> "Sat-Sun"
                WeeklyWindow.EVERY_DAY -> "Every day"
                else ->
                    window.days
                        .sortedBy { it.value }
                        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, locale) }
            }
        return "$days ${window.start.format(timeFormat)}-${window.end.format(timeFormat)}"
    }

    /** "Mon-Fri 09:00-18:00, Sat-Sun 10:00-12:00", or "Always" when no hours are set. */
    fun windows(
        windows: List<WeeklyWindow>,
        locale: Locale = Locale.getDefault(),
    ): String = if (windows.isEmpty()) "Always" else windows.joinToString(", ") { window(it, locale) }
}
