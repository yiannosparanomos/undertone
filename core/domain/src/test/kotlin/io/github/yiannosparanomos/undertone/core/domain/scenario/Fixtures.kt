package io.github.yiannosparanomos.undertone.core.domain.scenario

import io.github.yiannosparanomos.undertone.core.domain.engine.IncomingCall
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.CalendarEvent
import io.github.yiannosparanomos.undertone.core.domain.model.CalendarRule
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.EventMatch
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Package names used across scenarios. Real IDs where known, so the catalog reads naturally. */
object Apps {
    const val DIALER = "com.google.android.dialer"
    const val MESSENGER = "com.facebook.orca"
    const val GMAIL = "com.google.android.gm"
    const val TEAMS = "com.microsoft.teams"
    const val INSTAGRAM = "com.instagram.android"
    const val SPAM_SHOP = "com.example.spamshop"
    const val UNCONFIGURED = "com.example.weather"
}

/** The reference week: Monday 12 October 2026 to Monday 19 October 2026. */
object Week {
    val MON: LocalDate = LocalDate.of(2026, 10, 12)
    val TUE: LocalDate = MON.plusDays(1)
    val FRI: LocalDate = MON.plusDays(4)
    val SAT: LocalDate = MON.plusDays(5)
    val SUN: LocalDate = MON.plusDays(6)
    val NEXT_MON: LocalDate = MON.plusDays(7)
}

fun LocalDate.at(time: String): LocalDateTime = atTime(LocalTime.parse(time))

/** Calendar rules referenced by events below. */
object CalendarRules {
    /** Every "Weekly standup": only the dialer rings, everything else is silent, allow list may call. */
    val STANDUP =
        CalendarRule(
            id = "standup",
            match = EventMatch.Series("standup-series"),
            ringApps = setOf(Apps.DIALER),
            othersLevel = Level.SILENT,
            callTier = CallTier.ALLOW_LIST,
        )

    /** Any event with "focus" in the title: everything hidden, nobody rings. */
    val FOCUS =
        CalendarRule(
            id = "focus",
            match = EventMatch.TitleContains("focus"),
            ringApps = emptySet(),
            othersLevel = Level.HIDDEN,
            callTier = CallTier.NOBODY,
        )

    /** Every event in the Holidays calendar: Teams silent. All-day events are ignored by default. */
    val HOLIDAYS =
        CalendarRule(
            id = "holidays",
            match = EventMatch.WholeCalendar("holidays"),
            ringApps = emptySet(),
            othersLevel = Level.SILENT,
            callTier = CallTier.CONTACTS,
        )

    /** A permissive rule, used to show that calendar rules can only make things stricter. */
    val OPEN_HOUSE =
        CalendarRule(
            id = "open-house",
            match = EventMatch.TitleContains("open house"),
            ringApps = setOf(Apps.TEAMS, Apps.SPAM_SHOP, Apps.MESSENGER),
            othersLevel = Level.RING,
            callTier = CallTier.EVERYONE,
        )
}

object Events {
    fun standup(date: LocalDate) =
        CalendarEvent(
            id = "standup-$date",
            seriesId = "standup-series",
            calendarId = "work",
            title = "Weekly standup",
            start = date.at("10:00"),
            end = date.at("10:30"),
        )

    fun focus(date: LocalDate) =
        CalendarEvent(
            id = "focus-$date",
            seriesId = null,
            calendarId = "work",
            title = "Deep Focus block",
            start = date.at("10:15"),
            end = date.at("12:00"),
        )

    fun vacationAllDay(date: LocalDate) =
        CalendarEvent(
            id = "vacation-$date",
            seriesId = null,
            calendarId = "holidays",
            title = "Vacation",
            start = date.at("00:00"),
            end = date.plusDays(1).at("00:00"),
            allDay = true,
        )

    fun openHouse(date: LocalDate) =
        CalendarEvent(
            id = "open-house-$date",
            seriesId = null,
            calendarId = "personal",
            title = "Open house",
            start = date.at("10:00"),
            end = date.at("14:00"),
        )
}

/**
 * The reference setup, built in batches the way the setup tour does it:
 * 1. All apps ring by default.
 * 2. Focus apps get their own rules (Teams has work hours, Instagram is hidden overnight).
 * 3. Annoying apps are demoted (the spam shop is blocked).
 */
val referenceSettings =
    UndertoneSettings(
        defaultRule = AppRule(AppRule.ALL_APPS, inHours = Level.RING),
        appRules =
            listOf(
                AppRule(Apps.DIALER, inHours = Level.RING),
                AppRule(Apps.MESSENGER, inHours = Level.RING),
                AppRule(Apps.GMAIL, inHours = Level.RING),
                AppRule(
                    Apps.TEAMS,
                    inHours = Level.RING,
                    outsideHours = Level.HIDDEN,
                    hours = listOf(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0))),
                ),
                AppRule(
                    Apps.INSTAGRAM,
                    inHours = Level.HIDDEN,
                    outsideHours = Level.RING,
                    hours = listOf(WeeklyWindow(WeeklyWindow.EVERY_DAY, LocalTime.of(23, 0), LocalTime.of(7, 0))),
                ),
                AppRule(Apps.SPAM_SHOP, inHours = Level.BLOCKED),
            ).associateBy { it.packageName },
        calendarRules =
            listOf(
                CalendarRules.STANDUP,
                CalendarRules.FOCUS,
                CalendarRules.HOLIDAYS,
                CalendarRules.OPEN_HOUSE,
            ),
        defaultCallTier = CallTier.CONTACTS,
    )

val myRules = RuleState(profile = Profile.MY_RULES, settings = referenceSettings)

fun RuleState.withTier(tier: CallTier) = copy(settings = settings.copy(defaultCallTier = tier))

/** Callers used in call scenarios. */
object Callers {
    const val FAMILY = "+30 690 000 0001"
    const val COLLEAGUE = "+30 690 000 0002"
    const val UNKNOWN = "+30 210 000 0003"

    fun family(at: LocalDateTime) = IncomingCall(FAMILY, isContact = true, isOnAllowList = true, at = at)

    fun colleague(at: LocalDateTime) = IncomingCall(COLLEAGUE, isContact = true, isOnAllowList = false, at = at)

    fun unknown(at: LocalDateTime) = IncomingCall(UNKNOWN, isContact = false, isOnAllowList = false, at = at)
}
