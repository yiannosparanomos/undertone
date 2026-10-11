package io.github.yiannosparanomos.undertone.core.system.calendar

import io.github.yiannosparanomos.undertone.core.domain.model.CalendarEvent
import java.time.LocalDateTime

/**
 * Where the engine's calendar events come from.
 *
 * An interface because the real reader needs `READ_CALENDAR` and the calendar provider, which
 * arrives with the calendar rules UI in M5. Until then [None] keeps the pipeline honest: no
 * events, so no calendar rule ever matches, rather than a half-working reader.
 */
fun interface CalendarEventSource {
    /** Events overlapping the window the engine is about to consider. */
    suspend fun eventsAround(at: LocalDateTime): List<CalendarEvent>

    companion object {
        val None = CalendarEventSource { emptyList() }
    }
}
