package io.github.yiannosparanomos.undertone.core.system

import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RuleState
import io.github.yiannosparanomos.undertone.core.domain.model.UndertoneSettings
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import io.github.yiannosparanomos.undertone.core.system.schedule.BoundaryAlarms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class BoundaryAlarmsTest {
    private val alarms = BoundaryAlarms.from(InstrumentationRegistry.getInstrumentation().targetContext)

    private val monday = LocalDateTime.of(2026, 10, 12, 0, 0)

    private fun stateWith(vararg hours: WeeklyWindow) =
        RuleState(
            profile = Profile.MY_RULES,
            settings =
                UndertoneSettings(
                    appRules =
                        mapOf(
                            "com.example.chat" to
                                AppRule("com.example.chat", Level.RING, Level.HIDDEN, hours.toList()),
                        ),
                ),
        )

    @Test
    fun schedulingReturnsTheNextBoundary() {
        val state = stateWith(WeeklyWindow(WeeklyWindow.WEEKDAYS, LocalTime.of(9, 0), LocalTime.of(18, 0)))
        assertEquals(monday.withHour(18), alarms.scheduleNext(state, monday.withHour(12)))
        alarms.cancel()
    }

    @Test
    fun withNothingToWaitForNoAlarmIsSet() {
        assertNull(alarms.scheduleNext(stateWith(), monday.withHour(12)))
    }

    @Test
    fun reschedulingReplacesThePendingAlarmRatherThanAdding() {
        val state = stateWith(WeeklyWindow(WeeklyWindow.EVERY_DAY, LocalTime.of(8, 0), LocalTime.of(23, 0)))
        // The PendingIntent is built with FLAG_UPDATE_CURRENT and a fixed request code, so the
        // second call replaces the first: only ever one boundary alarm outstanding.
        assertEquals(monday.withHour(23), alarms.scheduleNext(state, monday.withHour(9)))
        assertEquals(monday.withHour(8), alarms.scheduleNext(state, monday.withHour(7)))
        alarms.cancel()
    }
}
