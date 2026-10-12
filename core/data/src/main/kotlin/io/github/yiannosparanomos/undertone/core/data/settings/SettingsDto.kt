package io.github.yiannosparanomos.undertone.core.data.settings

import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.CalendarRule
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.EventMatch
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatCallerSettings
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatGroup
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The on-disk shape of everything the user configured.
 *
 * These are deliberately separate from the domain models: `core:domain` stays pure Kotlin with
 * no dependencies (invariant 3), and the stored format can stay stable while the domain is
 * refactored. Times are written as readable strings because this same format is what settings
 * export produces, and a user should be able to open the file and understand it.
 *
 * Nothing here is notification content: these are rules, package names and times only. An
 * export therefore carries no titles, bodies, contact names or phone numbers (invariant 2).
 */
@Serializable
data class StoredStateDto(
    val version: Int = SCHEMA_VERSION,
    val profile: String = Profile.MY_RULES.name,
    val settings: SettingsDto = SettingsDto(),
    val override: OverrideDto? = null,
    /** How long log entries and held notifications are kept. Spec default: 7 days. */
    val retentionDays: Int = DEFAULT_RETENTION_DAYS,
    val sounds: SoundsDto = SoundsDto(),
    val allowList: AllowListDto = AllowListDto(),
) {
    companion object {
        /** Bumped whenever the stored shape changes in a way that needs migrating. */
        const val SCHEMA_VERSION = 1
        const val DEFAULT_RETENTION_DAYS = 7
    }
}

@Serializable
data class SettingsDto(
    val defaultRule: AppRuleDto = AppRuleDto(AppRule.ALL_APPS, Level.RING.name),
    val appRules: List<AppRuleDto> = emptyList(),
    val calendarRules: List<CalendarRuleDto> = emptyList(),
    val defaultCallTier: String = CallTier.ALLOW_LIST.name,
    val repeatCallers: RepeatCallersDto = RepeatCallersDto(),
)

/**
 * Who is on the allow list (spec §5).
 *
 * Like the sounds, this is not in `core:domain`: `CallEngine` is handed a resolved
 * `isOnAllowList` flag, because deciding whether a number belongs to a starred contact needs
 * the contacts provider and the engine must stay pure (invariant 3).
 *
 * [Mode.STARRED] is the same list Android's own Do Not Disturb uses. [Mode.EXPANDED] is that
 * list **plus** whatever the user added inside UnderTone — it never takes anyone away.
 */
@Serializable
data class AllowListDto(
    val mode: Mode = Mode.STARRED,
    /** Numbers typed into UnderTone. Only consulted in [Mode.EXPANDED]. */
    val numbers: List<String> = emptyList(),
    /** Contacts picked inside UnderTone, by `ContactsContract` lookup key. */
    val contactLookupKeys: List<String> = emptyList(),
) {
    @Serializable
    enum class Mode {
        @SerialName("starred")
        STARRED,

        @SerialName("expanded")
        EXPANDED,
    }

    /** Digits-only comparison, so "+30 210 123 4567" matches "+302101234567". */
    fun containsNumber(number: String): Boolean {
        if (mode == Mode.STARRED) return false
        val digits = number.filter(Char::isDigit)
        if (digits.isEmpty()) return false
        return numbers.any { it.filter(Char::isDigit) == digits }
    }
}

/**
 * What a Ring app sounds like.
 *
 * Sounds are not part of the rule engine: the engine decides *whether* a notification rings,
 * never what it sounds like, so none of this appears in `core:domain` (invariant 3). UnderTone
 * plays these itself because DND mutes the system's own sounds (ADR 0006).
 */
@Serializable
data class SoundsDto(
    /** Content URI of the default Ring sound; null means the system notification sound. */
    val defaultSoundUri: String? = null,
    val vibrate: Boolean = true,
    /** 0-100, applied on top of the notification stream's own volume. */
    val volumePercent: Int = 100,
    val perApp: List<AppSoundDto> = emptyList(),
) {
    fun forApp(packageName: String): AppSoundDto? = perApp.firstOrNull { it.packageName == packageName }
}

@Serializable
data class AppSoundDto(
    val packageName: String,
    val soundUri: String? = null,
    val vibrate: Boolean? = null,
)

@Serializable
data class AppRuleDto(
    val packageName: String,
    val inHours: String,
    val outsideHours: String = inHours,
    val hours: List<WeeklyWindowDto> = emptyList(),
)

@Serializable
data class WeeklyWindowDto(
    /** Day names as `java.time.DayOfWeek` spells them, e.g. "MONDAY". */
    val days: List<String>,
    /** "HH:mm". */
    val start: String,
    val end: String,
)

@Serializable
data class CalendarRuleDto(
    val id: String,
    val match: EventMatchDto,
    val ringApps: List<String> = emptyList(),
    val othersLevel: String,
    val callTier: String,
    val includeAllDayEvents: Boolean = false,
)

@Serializable
data class EventMatchDto(
    val type: Type,
    val value: String,
) {
    @Serializable
    enum class Type {
        @SerialName("series")
        SERIES,

        @SerialName("event")
        SINGLE_EVENT,

        @SerialName("calendar")
        WHOLE_CALENDAR,

        @SerialName("titleContains")
        TITLE_CONTAINS,
    }
}

@Serializable
data class RepeatCallersDto(
    val enabled: Boolean = true,
    val group: String = RepeatGroup.ALLOW_LIST.name,
    val callsNeeded: Int = 2,
    val windowSeconds: Long = 120,
)

@Serializable
data class OverrideDto(
    val type: Type,
    /** ISO-8601 local date-time, e.g. "2026-10-12T18:00". */
    val until: String,
    val level: String? = null,
    val ringApps: List<String> = emptyList(),
    val callTier: String? = null,
) {
    @Serializable
    enum class Type {
        @SerialName("quiet")
        QUIET,

        @SerialName("letThrough")
        LET_THROUGH,
    }
}

// ---------------------------------------------------------------------------------------------
// Domain -> DTO. Collections are sorted so the same settings always serialise byte-identically:
// that keeps exports diffable and stops DataStore rewriting the file for no reason.
// ---------------------------------------------------------------------------------------------

fun UndertoneSettings.toDto(): SettingsDto =
    SettingsDto(
        defaultRule = defaultRule.toDto(),
        appRules = appRules.values.map { it.toDto() }.sortedBy { it.packageName },
        calendarRules = calendarRules.map { it.toDto() },
        defaultCallTier = defaultCallTier.name,
        repeatCallers = repeatCallers.toDto(),
    )

fun AppRule.toDto(): AppRuleDto =
    AppRuleDto(
        packageName = packageName,
        inHours = inHours.name,
        outsideHours = outsideHours.name,
        hours = hours.map { it.toDto() },
    )

fun WeeklyWindow.toDto(): WeeklyWindowDto =
    WeeklyWindowDto(
        days = days.sortedBy { it.value }.map { it.name },
        start = start.format(TIME_FORMAT),
        end = end.format(TIME_FORMAT),
    )

fun CalendarRule.toDto(): CalendarRuleDto =
    CalendarRuleDto(
        id = id,
        match = match.toDto(),
        ringApps = ringApps.sorted(),
        othersLevel = othersLevel.name,
        callTier = callTier.name,
        includeAllDayEvents = includeAllDayEvents,
    )

fun EventMatch.toDto(): EventMatchDto =
    when (this) {
        is EventMatch.Series -> EventMatchDto(EventMatchDto.Type.SERIES, seriesId)
        is EventMatch.SingleEvent -> EventMatchDto(EventMatchDto.Type.SINGLE_EVENT, eventId)
        is EventMatch.WholeCalendar -> EventMatchDto(EventMatchDto.Type.WHOLE_CALENDAR, calendarId)
        is EventMatch.TitleContains -> EventMatchDto(EventMatchDto.Type.TITLE_CONTAINS, text)
    }

fun RepeatCallerSettings.toDto(): RepeatCallersDto =
    RepeatCallersDto(
        enabled = enabled,
        group = group.name,
        callsNeeded = callsNeeded,
        windowSeconds = window.seconds,
    )

fun Override.toDto(): OverrideDto =
    when (this) {
        is Override.Quiet ->
            OverrideDto(
                type = OverrideDto.Type.QUIET,
                until = until.toString(),
                level = level.name,
                ringApps = ringApps.sorted(),
                callTier = callTier.name,
            )
        is Override.LetThrough ->
            OverrideDto(
                type = OverrideDto.Type.LET_THROUGH,
                until = until.toString(),
                callTier = callTier.name,
            )
    }

// ---------------------------------------------------------------------------------------------
// DTO -> domain. A stored file can be hand-edited or come from an older version, so every
// conversion that could fail says what was wrong: see SettingsCodec, which turns these into a
// readable import error rather than a crash.
// ---------------------------------------------------------------------------------------------

fun SettingsDto.toDomain(): UndertoneSettings =
    UndertoneSettings(
        defaultRule = defaultRule.toDomain(),
        appRules = appRules.associate { it.packageName to it.toDomain() },
        calendarRules = calendarRules.map { it.toDomain() },
        defaultCallTier = enumOf<CallTier>(defaultCallTier, "defaultCallTier"),
        repeatCallers = repeatCallers.toDomain(),
    )

fun AppRuleDto.toDomain(): AppRule =
    AppRule(
        packageName = packageName,
        inHours = enumOf<Level>(inHours, "inHours"),
        outsideHours = enumOf<Level>(outsideHours, "outsideHours"),
        hours = hours.map { it.toDomain() },
    )

fun WeeklyWindowDto.toDomain(): WeeklyWindow =
    WeeklyWindow(
        days = days.map { enumOf<DayOfWeek>(it, "days") }.toSet(),
        start = parseTime(start, "start"),
        end = parseTime(end, "end"),
    )

fun CalendarRuleDto.toDomain(): CalendarRule =
    CalendarRule(
        id = id,
        match = match.toDomain(),
        ringApps = ringApps.toSet(),
        othersLevel = enumOf<Level>(othersLevel, "othersLevel"),
        callTier = enumOf<CallTier>(callTier, "callTier"),
        includeAllDayEvents = includeAllDayEvents,
    )

fun EventMatchDto.toDomain(): EventMatch =
    when (type) {
        EventMatchDto.Type.SERIES -> EventMatch.Series(value)
        EventMatchDto.Type.SINGLE_EVENT -> EventMatch.SingleEvent(value)
        EventMatchDto.Type.WHOLE_CALENDAR -> EventMatch.WholeCalendar(value)
        EventMatchDto.Type.TITLE_CONTAINS -> EventMatch.TitleContains(value)
    }

fun RepeatCallersDto.toDomain(): RepeatCallerSettings =
    RepeatCallerSettings(
        enabled = enabled,
        group = enumOf<RepeatGroup>(group, "group"),
        callsNeeded = callsNeeded,
        window = Duration.ofSeconds(windowSeconds),
    )

fun OverrideDto.toDomain(): Override {
    val at = parseDateTime(until, "until")
    return when (type) {
        OverrideDto.Type.QUIET ->
            Override.Quiet(
                until = at,
                level = level?.let { enumOf<Level>(it, "level") } ?: Level.SILENT,
                ringApps = ringApps.toSet(),
                callTier = callTier?.let { enumOf<CallTier>(it, "callTier") } ?: CallTier.ALLOW_LIST,
            )
        OverrideDto.Type.LET_THROUGH ->
            Override.LetThrough(
                until = at,
                callTier = callTier?.let { enumOf<CallTier>(it, "callTier") } ?: CallTier.EVERYONE,
            )
    }
}

fun StoredStateDto.profileOrDefault(): Profile = enumOf<Profile>(profile, "profile")

private val TIME_FORMAT =
    java.time.format.DateTimeFormatter
        .ofPattern("HH:mm")

private inline fun <reified T : Enum<T>> enumOf(
    value: String,
    field: String,
): T =
    enumValues<T>().firstOrNull { it.name == value }
        ?: throw StoredStateException("$field: \"$value\" is not one of ${enumValues<T>().joinToString { it.name }}")

private fun parseTime(
    value: String,
    field: String,
): LocalTime =
    try {
        LocalTime.parse(value)
    } catch (e: java.time.format.DateTimeParseException) {
        throw StoredStateException("$field: \"$value\" is not a time of day like \"09:00\"", e)
    }

private fun parseDateTime(
    value: String,
    field: String,
): LocalDateTime =
    try {
        LocalDateTime.parse(value)
    } catch (e: java.time.format.DateTimeParseException) {
        throw StoredStateException("$field: \"$value\" is not a date and time like \"2026-10-12T18:00\"", e)
    }

/** A stored or imported file could not be read. Carries a message fit to show the user. */
class StoredStateException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
