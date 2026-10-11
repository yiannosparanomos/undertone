package io.github.yiannosparanomos.undertone.core.domain.model

import java.time.Duration
import java.time.LocalDateTime

/**
 * How one app is treated. [inHours] applies inside any of [hours], [outsideHours] otherwise.
 * An empty [hours] list means "always in hours".
 */
data class AppRule(
    val packageName: String,
    val inHours: Level,
    val outsideHours: Level = inHours,
    val hours: List<WeeklyWindow> = emptyList(),
) {
    fun levelAt(at: LocalDateTime): Level =
        if (hours.isEmpty() || hours.any { it.contains(at) }) inHours else outsideHours

    companion object {
        /** Package name used for the "All apps" default rule. */
        const val ALL_APPS = "*"
    }
}

/** An event read from a calendar synced to the phone. */
data class CalendarEvent(
    val id: String,
    val seriesId: String?,
    val calendarId: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false,
) {
    fun isActiveAt(at: LocalDateTime): Boolean = !at.isBefore(start) && at.isBefore(end)
}

/** Which calendar events a [CalendarRule] applies to. */
sealed interface EventMatch {
    fun matches(event: CalendarEvent): Boolean

    /** Every occurrence of a recurring event, e.g. every "Weekly standup". */
    data class Series(
        val seriesId: String,
    ) : EventMatch {
        override fun matches(event: CalendarEvent) = event.seriesId == seriesId
    }

    /** One single event. */
    data class SingleEvent(
        val eventId: String,
    ) : EventMatch {
        override fun matches(event: CalendarEvent) = event.id == eventId
    }

    /** Every event in one calendar, e.g. "Work". */
    data class WholeCalendar(
        val calendarId: String,
    ) : EventMatch {
        override fun matches(event: CalendarEvent) = event.calendarId == calendarId
    }

    /** Any event whose title contains [text], case-insensitive. */
    data class TitleContains(
        val text: String,
    ) : EventMatch {
        override fun matches(event: CalendarEvent) = event.title.contains(text, ignoreCase = true)
    }
}

/**
 * What is allowed while a matching event is running: apps in [ringApps] ring,
 * every other app gets [othersLevel], and calls follow [callTier].
 */
data class CalendarRule(
    val id: String,
    val match: EventMatch,
    val ringApps: Set<String>,
    val othersLevel: Level,
    val callTier: CallTier,
    val includeAllDayEvents: Boolean = false,
) {
    fun appliesTo(event: CalendarEvent): Boolean = (includeAllDayEvents || !event.allDay) && match.matches(event)

    fun levelFor(packageName: String): Level = if (packageName in ringApps) Level.RING else othersLevel
}

/** A temporary manual override from the widget or home screen. Ends by itself at [until]. */
sealed interface Override {
    val until: LocalDateTime

    fun isActiveAt(at: LocalDateTime): Boolean = at.isBefore(until)

    /** "Quiet for 1 hour": everything not in [ringApps] gets [level]. */
    data class Quiet(
        override val until: LocalDateTime,
        val level: Level = Level.SILENT,
        val ringApps: Set<String> = emptySet(),
        val callTier: CallTier = CallTier.ALLOW_LIST,
    ) : Override

    /** "Let everything through for 30 min". Blocked apps stay blocked. */
    data class LetThrough(
        override val until: LocalDateTime,
        val callTier: CallTier = CallTier.EVERYONE,
    ) : Override
}

/** The custom repeat-caller rule. Defaults: allow list, 2 calls within 2 minutes. */
data class RepeatCallerSettings(
    val enabled: Boolean = true,
    val group: RepeatGroup = RepeatGroup.ALLOW_LIST,
    val callsNeeded: Int = 2,
    val window: Duration = Duration.ofMinutes(2),
) {
    init {
        require(callsNeeded >= 2) { "A repeat call needs at least 2 calls" }
        require(!window.isNegative && !window.isZero) { "The repeat window must be positive" }
    }
}

/** Everything the user configured inside the My rules profile. */
data class UndertoneSettings(
    val defaultRule: AppRule = AppRule(AppRule.ALL_APPS, inHours = Level.RING),
    val appRules: Map<String, AppRule> = emptyMap(),
    val calendarRules: List<CalendarRule> = emptyList(),
    val defaultCallTier: CallTier = CallTier.ALLOW_LIST,
    val repeatCallers: RepeatCallerSettings = RepeatCallerSettings(),
) {
    fun ruleFor(packageName: String): AppRule = appRules[packageName] ?: defaultRule
}

/** A snapshot of everything the engines need to make a decision. */
data class RuleState(
    val profile: Profile,
    val settings: UndertoneSettings,
    val events: List<CalendarEvent> = emptyList(),
    val override: Override? = null,
) {
    fun activeOverride(at: LocalDateTime): Override? = override?.takeIf { it.isActiveAt(at) }

    fun activeCalendarRules(at: LocalDateTime): List<CalendarRule> {
        val running = events.filter { it.isActiveAt(at) }
        return settings.calendarRules.filter { rule -> running.any { rule.appliesTo(it) } }
    }
}
