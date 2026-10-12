package io.github.yiannosparanomos.undertone.core.system.calendar

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import io.github.yiannosparanomos.undertone.core.domain.model.CalendarEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reads events from the calendars already synced to the phone (spec §4): no account login, no
 * network, read-only.
 *
 * Event titles are read because a rule can match on them, and they are content: they go into
 * the engine and the encrypted database, never into a log. Only the window the engine is about
 * to consider is queried, so there is no reason to hold a copy of the calendar.
 */
class CalendarReader(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : CalendarEventSource {
    override suspend fun eventsAround(at: LocalDateTime): List<CalendarEvent> {
        // A day either side: enough for an all-day event or one running over midnight to be seen.
        val from =
            at
                .minusDays(1)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        val to =
            at
                .plusDays(1)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        return query(from, to)
    }

    /** Calendars the user could pick in the Rules screen. */
    fun calendars(): List<Calendar> =
        try {
            context.contentResolver
                .query(
                    CalendarContract.Calendars.CONTENT_URI,
                    arrayOf(
                        CalendarContract.Calendars._ID,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        CalendarContract.Calendars.ACCOUNT_NAME,
                    ),
                    "${CalendarContract.Calendars.VISIBLE} = 1",
                    null,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                )?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                Calendar(
                                    id = cursor.getLong(0).toString(),
                                    displayName = cursor.getString(1) ?: cursor.getString(2) ?: "Calendar",
                                ),
                            )
                        }
                    }
                } ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "Calendar access denied")
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not list calendars: ${e.javaClass.simpleName}")
            emptyList()
        }

    data class Calendar(
        val id: String,
        val displayName: String,
    )

    private fun query(
        fromMillis: Long,
        toMillis: Long,
    ): List<CalendarEvent> {
        // CalendarContract.Instances expands recurring events, which is what a rule on a series
        // needs: every occurrence, not the master row.
        val uri =
            CalendarContract.Instances.CONTENT_URI
                .buildUpon()
                .appendPath(fromMillis.toString())
                .appendPath(toMillis.toString())
                .build()
        return try {
            context.contentResolver
                .query(uri, PROJECTION, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toEvent()) } }
                ?: emptyList()
        } catch (e: SecurityException) {
            // Permission revoked: no events, so no calendar rule matches. The Health check
            // screen reports it rather than the app pretending the rules still work.
            Log.w(TAG, "Calendar access denied; no calendar rules will apply")
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read calendar events: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    private fun Cursor.toEvent(): CalendarEvent {
        val allDay = getInt(5) != 0
        return CalendarEvent(
            id = getString(0) ?: getLong(1).toString(),
            // EVENT_ID is shared by every occurrence of a recurring event, which is exactly
            // what EventMatch.Series matches on.
            seriesId = getLong(1).toString(),
            calendarId = getLong(2).toString(),
            title = getString(3) ?: "",
            start = Instant.ofEpochMilli(getLong(4)).atZone(zone).toLocalDateTime(),
            end = Instant.ofEpochMilli(getLong(6)).atZone(zone).toLocalDateTime(),
            allDay = allDay,
        )
    }

    private companion object {
        const val TAG = "UndertoneCalendar"

        val PROJECTION =
            arrayOf(
                CalendarContract.Instances._ID,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.END,
            )
    }
}
