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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class SettingsCodecTest {
    private val fullSettings =
        UndertoneSettings(
            defaultRule = AppRule(AppRule.ALL_APPS, inHours = Level.SILENT),
            appRules =
                mapOf(
                    "com.example.chat" to
                        AppRule(
                            packageName = "com.example.chat",
                            inHours = Level.RING,
                            outsideHours = Level.HIDDEN,
                            hours =
                                listOf(
                                    WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0)),
                                    // Overnight window: end is not after start.
                                    WeeklyWindow(WeeklyWindow.WEEKEND, LocalTime.of(22, 0), LocalTime.of(7, 0)),
                                ),
                        ),
                    "com.example.spam" to AppRule("com.example.spam", inHours = Level.BLOCKED),
                ),
            calendarRules =
                listOf(
                    CalendarRule(
                        id = "rule-1",
                        match = EventMatch.Series("series-9"),
                        ringApps = setOf("com.example.chat"),
                        othersLevel = Level.HIDDEN,
                        callTier = CallTier.ALLOW_LIST,
                        includeAllDayEvents = true,
                    ),
                    CalendarRule(
                        id = "rule-2",
                        match = EventMatch.TitleContains("standup"),
                        ringApps = emptySet(),
                        othersLevel = Level.SILENT,
                        callTier = CallTier.NOBODY,
                    ),
                ),
            defaultCallTier = CallTier.CONTACTS,
            repeatCallers =
                RepeatCallerSettings(
                    enabled = false,
                    group = RepeatGroup.EVERYONE,
                    callsNeeded = 3,
                    window = Duration.ofMinutes(5),
                ),
        )

    @Test
    fun `settings survive a round trip`() {
        val encoded = SettingsCodec.encode(StoredStateDto(settings = fullSettings.toDto()))
        assertEquals(fullSettings, SettingsCodec.decode(encoded).settings.toDomain())
    }

    @Test
    fun `a quiet override survives a round trip`() {
        val override =
            Override.Quiet(
                until = LocalDateTime.of(2026, 10, 12, 18, 0),
                level = Level.HIDDEN,
                ringApps = setOf("com.example.chat"),
                callTier = CallTier.NOBODY,
            )
        val encoded = SettingsCodec.encode(StoredStateDto(override = override.toDto()))
        assertEquals(override, SettingsCodec.decode(encoded).override?.toDomain())
    }

    @Test
    fun `a let-through override survives a round trip`() {
        val override = Override.LetThrough(until = LocalDateTime.of(2026, 10, 12, 9, 30))
        val encoded = SettingsCodec.encode(StoredStateDto(override = override.toDto()))
        assertEquals(override, SettingsCodec.decode(encoded).override?.toDomain())
    }

    @Test
    fun `every event match type survives a round trip`() {
        val matches =
            listOf(
                EventMatch.Series("s"),
                EventMatch.SingleEvent("e"),
                EventMatch.WholeCalendar("c"),
                EventMatch.TitleContains("t"),
            )
        assertEquals(matches, matches.map { it.toDto().toDomain() })
    }

    @Test
    fun `defaults round trip to the domain defaults`() {
        val decoded = SettingsCodec.decode(SettingsCodec.encode(StoredStateDto()))
        assertEquals(UndertoneSettings(), decoded.settings.toDomain())
        assertEquals(Profile.MY_RULES, decoded.profileOrDefault())
        assertEquals(7, decoded.retentionDays)
        assertNull(decoded.override)
    }

    @Test
    fun `encoding is stable so exports stay diffable`() {
        val a = SettingsCodec.encode(StoredStateDto(settings = fullSettings.toDto()))
        val b = SettingsCodec.encode(StoredStateDto(settings = fullSettings.toDto()))
        assertEquals(a, b)
    }

    @Test
    fun `app rule order does not change the encoding`() {
        val reversed =
            fullSettings.copy(
                appRules =
                    LinkedHashMap<String, AppRule>().apply {
                        fullSettings.appRules.entries
                            .reversed()
                            .forEach { put(it.key, it.value) }
                    },
            )
        assertEquals(
            SettingsCodec.encode(StoredStateDto(settings = fullSettings.toDto())),
            SettingsCodec.encode(StoredStateDto(settings = reversed.toDto())),
        )
    }

    @Test
    fun `an export carries no notification content`() {
        // Invariant 2: an export is rules only. Nothing in the format can hold a title, a body,
        // a contact name or a number, so there is no field for one to leak through.
        val encoded = SettingsCodec.encode(StoredStateDto(settings = fullSettings.toDto()))
        listOf("title", "text", "body", "number", "contact", "phone").forEach { field ->
            assertTrue(
                "exported settings must have no \"$field\" field",
                !encoded.contains("\"$field\""),
            )
        }
    }

    @Test
    fun `a newer schema version is refused with a readable message`() {
        val text = SettingsCodec.encode(StoredStateDto(version = StoredStateDto.SCHEMA_VERSION + 1))
        val error = assertThrows(StoredStateException::class.java) { SettingsCodec.decode(text) }
        assertTrue(error.message!!.contains("newer version"))
    }

    @Test
    fun `junk is refused rather than parsed`() {
        assertThrows(StoredStateException::class.java) { SettingsCodec.decode("not json at all") }
        assertThrows(StoredStateException::class.java) { SettingsCodec.decode("") }
    }

    @Test
    fun `an unknown level is an import error naming the field`() {
        val text = """{"settings":{"defaultRule":{"packageName":"*","inHours":"LOUD"}}}"""
        val error = assertThrows(StoredStateException::class.java) { SettingsCodec.decode(text) }
        assertTrue(error.message!!.contains("inHours"))
        assertTrue(error.message!!.contains("LOUD"))
    }

    @Test
    fun `a malformed time is an import error naming the field`() {
        val text =
            """
            {"settings":{"defaultRule":{"packageName":"*","inHours":"RING",
            "hours":[{"days":["MONDAY"],"start":"nine o'clock","end":"18:00"}]}}}
            """.trimIndent()
        val error = assertThrows(StoredStateException::class.java) { SettingsCodec.decode(text) }
        assertTrue(error.message!!.contains("start"))
    }

    @Test
    fun `a zero retention is refused`() {
        val text = SettingsCodec.encode(StoredStateDto(retentionDays = 0))
        assertThrows(StoredStateException::class.java) { SettingsCodec.decode(text) }
    }

    @Test
    fun `unknown keys from a newer build are ignored`() {
        val text = """{"version":1,"profile":"MY_RULES","somethingNew":{"a":1}}"""
        assertEquals(Profile.MY_RULES, SettingsCodec.decode(text).profileOrDefault())
    }

    @Test
    fun `overnight windows keep their meaning across a round trip`() {
        val window = WeeklyWindow(setOf(DayOfWeek.FRIDAY), LocalTime.of(22, 0), LocalTime.of(7, 0))
        val restored = window.toDto().toDomain()
        assertEquals(window, restored)
        // Saturday 02:00 is inside a Friday-night window.
        assertTrue(restored.contains(LocalDateTime.of(2026, 10, 17, 2, 0)))
    }
}
