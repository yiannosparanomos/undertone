package io.github.yiannosparanomos.undertone.core.data.history

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.domain.engine.CallDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.CallOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.engine.IncomingCall
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationDecision
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

/**
 * What gets written after each kind of decision, and what retention removes.
 *
 * In memory rather than on the encrypted file: these are about the rules of recording, and
 * EncryptionTest covers the file itself.
 */
class HistoryStoreTest {
    private lateinit var database: UndertoneDatabase
    private lateinit var store: HistoryStore

    private val noon: LocalDateTime = LocalDateTime.of(2026, 10, 12, 12, 0)

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    UndertoneDatabase::class.java,
                ).build()
        store = HistoryStore.from(database)
    }

    @After
    fun close() = database.close()

    private suspend fun record(
        outcome: NotificationOutcome,
        key: String = "k-$outcome",
        at: LocalDateTime = noon,
        packageName: String = "com.example.chat",
        releaseAt: LocalDateTime? = null,
    ) = store.record(
        packageName = packageName,
        notificationKey = key,
        title = "title",
        text = "text",
        postedAt = at,
        decision = NotificationDecision(outcome, DecisionReason.APP_SCHEDULE, releaseAt),
    )

    @Test
    fun ringAndPassThroughAreNotRecordedAtAll() =
        runTest {
            record(NotificationOutcome.RING)
            record(NotificationOutcome.PASS_THROUGH)
            assertEquals(0, database.logDao().count())
            assertEquals(0, store.heldCount.first())
            assertEquals(0, store.blockedTotalOn(noon.toLocalDate()).first())
        }

    @Test
    fun silentIsLoggedButNotHeld() =
        runTest {
            record(NotificationOutcome.SILENT)
            assertEquals(1, store.log.first().size)
            assertEquals(0, store.heldCount.first())
        }

    @Test
    fun hiddenIsBothLoggedAndHeld() =
        runTest {
            record(NotificationOutcome.HIDDEN, releaseAt = noon.plusHours(6))
            assertEquals(1, store.log.first().size)
            val held = store.held.first().single()
            assertEquals("com.example.chat", held.packageName)
            assertEquals(noon.plusHours(6), held.releaseAt)
        }

    @Test
    fun blockedIsCountedAndNothingElseIsKept() =
        runTest {
            repeat(3) { record(NotificationOutcome.BLOCKED, key = "k$it") }
            // Spec section 2: blocked gets a daily count only, no log rows and no held rows.
            assertEquals(0, database.logDao().count())
            assertEquals(0, store.heldCount.first())
            assertEquals(3, store.blockedTotalOn(noon.toLocalDate()).first())
            assertEquals(
                3,
                store
                    .blockedCountsOn(noon.toLocalDate())
                    .first()
                    .single()
                    .count,
            )
        }

    @Test
    fun blockedCountsAreSeparatePerDay() =
        runTest {
            record(NotificationOutcome.BLOCKED, key = "a", at = noon)
            record(NotificationOutcome.BLOCKED, key = "b", at = noon.plusDays(1))
            assertEquals(1, store.blockedTotalOn(noon.toLocalDate()).first())
            assertEquals(1, store.blockedTotalOn(noon.plusDays(1).toLocalDate()).first())
            assertEquals(2, store.blockedTotalForApp("com.example.chat", noon.toLocalDate()))
        }

    @Test
    fun arepostedNotificationReplacesItsHeldRow() =
        runTest {
            record(NotificationOutcome.HIDDEN, key = "same", releaseAt = noon.plusHours(1))
            record(NotificationOutcome.HIDDEN, key = "same", releaseAt = noon.plusHours(2))
            val held = store.held.first()
            assertEquals("one row per notification key", 1, held.size)
            assertEquals(noon.plusHours(2), held.single().releaseAt)
        }

    @Test
    fun heldNotificationsComeDueAtTheirReleaseTime() =
        runTest {
            record(NotificationOutcome.HIDDEN, key = "soon", releaseAt = noon.plusHours(1))
            record(NotificationOutcome.HIDDEN, key = "later", releaseAt = noon.plusHours(5))
            record(NotificationOutcome.HIDDEN, key = "never", releaseAt = null)

            assertEquals(emptyList<String>(), store.heldDueAt(noon).map { it.notificationKey })
            assertEquals(listOf("soon"), store.heldDueAt(noon.plusHours(1)).map { it.notificationKey })
            assertEquals(
                setOf("soon", "later"),
                store.heldDueAt(noon.plusHours(6)).map { it.notificationKey }.toSet(),
            )
        }

    @Test
    fun releasingRemovesFromTheInboxButLeavesTheLog() =
        runTest {
            record(NotificationOutcome.HIDDEN, key = "k", releaseAt = noon.plusHours(1))
            assertTrue(store.releaseHeld("k"))
            assertEquals(0, store.heldCount.first())
            assertEquals("the log keeps its history", 1, store.log.first().size)
            assertEquals("releasing twice is harmless", false, store.releaseHeld("k"))
        }

    @Test
    fun releaseAllEmptiesTheInbox() =
        runTest {
            repeat(4) { record(NotificationOutcome.HIDDEN, key = "k$it", releaseAt = noon.plusHours(1)) }
            assertEquals(4, store.heldCount.first())
            store.releaseAllHeld()
            assertEquals(0, store.heldCount.first())
        }

    @Test
    fun recentCallsAreFoundInsideTheRepeatWindow() =
        runTest {
            val call = IncomingCall("+35799000000", isContact = false, isOnAllowList = false, at = noon)
            store.record(call, CallDecision(CallOutcome.SILENT, DecisionReason.TIER_SILENCED, CallTier.ALLOW_LIST))
            store.record(
                call.copy(at = noon.plusMinutes(1)),
                CallDecision(CallOutcome.SILENT, DecisionReason.TIER_SILENCED, CallTier.ALLOW_LIST),
            )
            store.record(
                call.copy(at = noon.minusMinutes(30)),
                CallDecision(CallOutcome.SILENT, DecisionReason.TIER_SILENCED, CallTier.ALLOW_LIST),
            )

            val within =
                store.recentCallsFrom("+35799000000", noon.plusMinutes(2), Duration.ofMinutes(2))
            assertEquals("only the calls inside the window count", 2, within.size)

            val other = store.recentCallsFrom("+35799111111", noon.plusMinutes(2), Duration.ofMinutes(2))
            assertEquals("a different caller is a different repeat count", 0, other.size)
        }

    @Test
    fun retentionRemovesEverythingOlderThanTheCutoff() =
        runTest {
            val old = noon.minusDays(8)
            record(NotificationOutcome.SILENT, key = "old", at = old)
            record(NotificationOutcome.HIDDEN, key = "oldHeld", at = old, releaseAt = old.plusHours(1))
            record(NotificationOutcome.BLOCKED, key = "oldBlocked", at = old)
            store.record(
                IncomingCall("+35799000000", isContact = false, isOnAllowList = false, at = old),
                CallDecision(CallOutcome.SILENT, DecisionReason.TIER_SILENCED, CallTier.ALLOW_LIST),
            )
            record(NotificationOutcome.SILENT, key = "fresh", at = noon.minusDays(1))

            val removed = store.deleteExpired(noon, retentionDays = 7)

            // Two log rows aged out, not one: the Hidden notification is logged as well as held.
            assertEquals(2, removed.logEntries)
            assertEquals(1, removed.heldNotifications)
            assertEquals(1, removed.recentCalls)
            assertEquals(1, removed.blockedCountDays)
            assertEquals(5, removed.total)

            assertEquals(listOf("fresh"), store.log.first().map { it.notificationKey })
            assertEquals(0, store.heldCount.first())
            assertEquals(0, store.blockedTotalOn(old.toLocalDate()).first())
        }

    @Test
    fun retentionKeepsEntriesInsideThePeriod() =
        runTest {
            record(NotificationOutcome.SILENT, key = "edge", at = noon.minusDays(7).plusMinutes(1))
            assertEquals(0, store.deleteExpired(noon, retentionDays = 7).total)
            assertEquals(1, store.log.first().size)
        }

    @Test
    fun retentionPeriodIsAdjustable() =
        runTest {
            record(NotificationOutcome.SILENT, key = "twoDaysOld", at = noon.minusDays(2))
            assertEquals(0, store.deleteExpired(noon, retentionDays = 7).total)
            assertEquals(1, store.deleteExpired(noon, retentionDays = 1).logEntries)
        }

    @Test
    fun theLogCanBeFilteredToOneApp() =
        runTest {
            record(NotificationOutcome.SILENT, key = "a", packageName = "com.example.chat")
            record(NotificationOutcome.SILENT, key = "b", packageName = "com.example.news")
            assertEquals(1, store.logForApp("com.example.news").first().size)
            assertEquals(2, store.log.first().size)
        }

    @Test
    fun deletingEverythingLeavesNothingBehind() =
        runTest {
            record(NotificationOutcome.SILENT, key = "a")
            record(NotificationOutcome.HIDDEN, key = "b", releaseAt = noon.plusHours(1))
            record(NotificationOutcome.BLOCKED, key = "c")
            store.record(
                IncomingCall("+1", isContact = true, isOnAllowList = true, at = noon),
                CallDecision(CallOutcome.RING, DecisionReason.RING_TIER, CallTier.CONTACTS),
            )

            store.deleteEverything()

            assertEquals(0, database.logDao().count())
            assertEquals(0, store.heldCount.first())
            assertEquals(0, store.blockedTotalOn(noon.toLocalDate()).first())
            assertEquals(0, store.recentCallsFrom("+1", noon.plusMinutes(1), Duration.ofDays(1)).size)
            assertNull(database.heldDao().byKey("b"))
        }
}
