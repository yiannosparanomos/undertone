package io.github.yiannosparanomos.undertone.core.system

import io.github.yiannosparanomos.undertone.core.system.notification.SnoozeHost
import io.github.yiannosparanomos.undertone.core.system.notification.SnoozeStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeStrategyTest {
    private class FakeHost(
        private var snoozed: MutableList<String> = mutableListOf(),
    ) : SnoozeHost {
        val calls = mutableListOf<Pair<String, Long>>()

        override fun snoozeNotification(
            key: String,
            durationMs: Long,
        ) {
            calls += key to durationMs
            if (durationMs <= 1L) snoozed.remove(key) else snoozed += key
        }

        override fun snoozedKeys(): List<String> = snoozed.toList()
    }

    @Test
    fun `the API level decides the strategy, with the fallback below 33`() {
        assertEquals(SnoozeStrategy.RepostCopy, SnoozeStrategy.forThisDevice(29))
        assertEquals(SnoozeStrategy.RepostCopy, SnoozeStrategy.forThisDevice(30))
        // 30-32 was never measured, so it takes the fallback that works everywhere (ADR 0006).
        assertEquals(SnoozeStrategy.RepostCopy, SnoozeStrategy.forThisDevice(32))
        assertEquals(SnoozeStrategy.ReSnoozeToRelease, SnoozeStrategy.forThisDevice(33))
        assertEquals(SnoozeStrategy.ReSnoozeToRelease, SnoozeStrategy.forThisDevice(35))
    }

    @Test
    fun `re-snoozing releases the original and reports success`() {
        val host = FakeHost()
        SnoozeStrategy.ReSnoozeToRelease.snooze(host, "k", 600_000)
        assertEquals(listOf("k"), host.snoozedKeys())

        assertTrue(SnoozeStrategy.ReSnoozeToRelease.releaseEarly(host, "k"))
        assertEquals("k" to 1L, host.calls.last())
        assertEquals(emptyList<String>(), host.snoozedKeys())
    }

    @Test
    fun `releasing something that is not snoozed does nothing`() {
        val host = FakeHost()
        assertFalse(SnoozeStrategy.ReSnoozeToRelease.releaseEarly(host, "missing"))
        assertEquals(emptyList<Pair<String, Long>>(), host.calls)
    }

    @Test
    fun `the fallback snoozes but cannot release, so the caller posts a copy`() {
        val host = FakeHost()
        SnoozeStrategy.RepostCopy.snooze(host, "k", 600_000)
        assertEquals(listOf("k"), host.snoozedKeys())

        assertFalse("API 29 cannot release early", SnoozeStrategy.RepostCopy.releaseEarly(host, "k"))
        // It must not try and silently leave the notification snoozed for ten minutes.
        assertEquals(1, host.calls.size)
        assertEquals(listOf("k"), host.snoozedKeys())
    }
}
