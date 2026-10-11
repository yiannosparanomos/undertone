package io.github.yiannosparanomos.undertone.core.system

import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.SoundsDto
import io.github.yiannosparanomos.undertone.core.data.settings.StoredStateDto
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.domain.engine.NotificationOutcome
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.WeeklyWindow
import io.github.yiannosparanomos.undertone.core.system.notification.NotificationAction
import io.github.yiannosparanomos.undertone.core.system.notification.NotificationPipeline
import io.github.yiannosparanomos.undertone.core.system.notification.Ringer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The pipeline end to end: real engine, real database, real settings store. Only the sound
 * output is a stand-in, since an emulator cannot be asked whether something was audible.
 */
class NotificationPipelineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: UndertoneDatabase
    private lateinit var history: HistoryStore
    private lateinit var settings: UndertoneSettingsStore
    private lateinit var settingsFile: File
    private lateinit var pipeline: NotificationPipeline

    private val rung = mutableListOf<String>()

    /** Monday 2026-10-12, 12:00. */
    private val noon = LocalDateTime.of(2026, 10, 12, 12, 0)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, UndertoneDatabase::class.java).build()
        history = HistoryStore.from(database)
        settingsFile = File.createTempFile("pipeline-settings", ".json", context.cacheDir).apply { delete() }
        settings =
            UndertoneSettingsStore(
                DataStoreFactory.create(
                    serializer = StoredStateSerializerTestAccess.serializer,
                    produceFile = { settingsFile },
                ),
            )
        pipeline =
            NotificationPipeline(
                settings = settings,
                history = history,
                ringer = Ringer { packageName, _ -> rung += packageName },
                now = { noon },
            )
    }

    @After
    fun tearDown() {
        database.close()
        settingsFile.delete()
    }

    private suspend fun post(
        packageName: String = "com.example.chat",
        key: String = "k1",
        at: LocalDateTime = noon,
        isOngoing: Boolean = false,
    ) = pipeline.onPosted(
        packageName = packageName,
        notificationKey = key,
        title = "Mum",
        text = "Dinner on Sunday?",
        postedAt = at,
        isOngoing = isOngoing,
    )

    @Test
    fun aRingAppIsLeftInTheShadeAndSounds() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.RING)) }

            val outcome = post()

            assertEquals(NotificationOutcome.RING, outcome.decision.outcome)
            assertEquals(NotificationAction.Leave, outcome.action)
            assertEquals(listOf("com.example.chat"), rung)
            // Ring is not logged: the shade already shows it (spec section 2).
            assertEquals(0, database.logDao().count())
        }

    @Test
    fun aSilentAppIsLeftInTheShadeWithoutSound() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.SILENT)) }

            val outcome = post()

            assertEquals(NotificationOutcome.SILENT, outcome.decision.outcome)
            assertEquals(NotificationAction.Leave, outcome.action)
            assertTrue("a Silent app must not make a sound", rung.isEmpty())
            assertEquals(1, history.log.first().size)
        }

    @Test
    fun aBlockedAppIsCancelledAndOnlyCounted() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.BLOCKED)) }

            val outcome = post()

            assertEquals(NotificationAction.Cancel, outcome.action)
            assertEquals(0, database.logDao().count())
            assertEquals(1, history.blockedTotalOn(noon.toLocalDate()).first())
        }

    @Test
    fun aHiddenAppIsSnoozedUntilItsHoursStart() =
        runTest {
            // Ring 09:00-18:00 on weekdays, Hidden outside: at 20:00 it waits for 09:00 tomorrow.
            settings.updateSettings {
                it.copy(
                    appRules =
                        mapOf(
                            "com.example.chat" to
                                AppRule(
                                    packageName = "com.example.chat",
                                    inHours = Level.RING,
                                    outsideHours = Level.HIDDEN,
                                    hours =
                                        listOf(
                                            WeeklyWindow(
                                                WeeklyWindow.WEEKDAYS,
                                                LocalTime.of(9, 0),
                                                LocalTime.of(18, 0),
                                            ),
                                        ),
                                ),
                        ),
                )
            }
            val evening = noon.withHour(20)

            val outcome = post(at = evening)

            assertEquals(NotificationOutcome.HIDDEN, outcome.decision.outcome)
            val expected = Duration.between(evening, noon.plusDays(1).withHour(9)).toMillis()
            assertEquals(NotificationAction.Snooze(expected), outcome.action)

            val held = history.held.first().single()
            assertEquals(noon.plusDays(1).withHour(9), held.releaseAt)
            assertEquals("Hidden is logged as well as held", 1, history.log.first().size)
        }

    @Test
    fun aHiddenAppWithNoReleaseInSightSnoozesForTheRetentionPeriod() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.HIDDEN)) }
            settings.setRetentionDays(7)

            val outcome = post()

            // Nothing ever stops being Hidden, so it is held for as long as it would be kept.
            assertEquals(NotificationAction.Snooze(Duration.ofDays(7).toMillis()), outcome.action)
        }

    @Test
    fun anOngoingNotificationIsAlwaysLeftAlone() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.BLOCKED)) }

            val outcome = post(isOngoing = true)

            // Holding or cancelling a music player's notification would break the app.
            assertEquals(NotificationAction.PassThrough, outcome.action)
            assertEquals(0, database.logDao().count())
            assertEquals(0, history.blockedTotalOn(noon.toLocalDate()).first())
        }

    @Test
    fun allowAllPassesEverythingThroughAndRecordsNothing() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.HIDDEN)) }
            settings.setProfile(Profile.ALLOW_ALL)

            val outcome = post()

            assertEquals(NotificationOutcome.PASS_THROUGH, outcome.decision.outcome)
            assertEquals(NotificationAction.PassThrough, outcome.action)
            assertEquals(0, database.logDao().count())
            assertEquals(0, history.heldCount.first())
        }

    @Test
    fun aBlockedAppStaysBlockedUnderALetThroughOverride() =
        runTest {
            settings.updateSettings {
                it.copy(appRules = mapOf("com.example.spam" to AppRule("com.example.spam", Level.BLOCKED)))
            }
            settings.setOverride(Override.LetThrough(until = noon.plusMinutes(30)))

            val outcome = post(packageName = "com.example.spam")

            // Invariant 5: blocked apps stay blocked under every override.
            assertEquals(NotificationAction.Cancel, outcome.action)
        }

    @Test
    fun aSwipeIsCountedAndAnOpenIsNot() =
        runTest {
            repeat(10) {
                pipeline.onRemoved(
                    packageName = "com.example.news",
                    notificationKey = "k$it",
                    reason = NotificationPipeline.RemovalReason.SWIPED_AWAY,
                    at = noon,
                )
            }
            assertEquals(
                listOf("com.example.news"),
                history.blockSuggestions(noon.toLocalDate()).map { it.packageName },
            )

            pipeline.onRemoved(
                packageName = "com.example.news",
                notificationKey = "opened",
                reason = NotificationPipeline.RemovalReason.OPENED,
                at = noon,
            )
            // An app the user does read should never be suggested for blocking.
            assertTrue(history.blockSuggestions(noon.toLocalDate()).isEmpty())
        }

    @Test
    fun aWithdrawnNotificationLeavesTheHeldInbox() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.HIDDEN)) }
            post(key = "held-1")
            assertEquals(1, history.heldCount.first())

            // The source app took it back while it was waiting (spec section 3).
            pipeline.onRemoved(
                packageName = "com.example.chat",
                notificationKey = "held-1",
                reason = NotificationPipeline.RemovalReason.OTHER,
                at = noon,
            )
            assertEquals(0, history.heldCount.first())
        }

    @Test
    fun theRuleStateReflectsStoredSettings() =
        runTest {
            settings.setProfile(Profile.MY_RULES)
            settings.setOverride(Override.Quiet(until = noon.plusHours(1)))

            val live = pipeline.ruleState(noon)
            assertEquals(Profile.MY_RULES, live.profile)
            assertTrue(live.override != null)

            // An override that has run out is not part of the state any more.
            assertFalse(pipeline.ruleState(noon.plusHours(2)).override != null)
        }

    @Test
    fun aPerAppSoundChoiceIsPassedToTheRinger() =
        runTest {
            settings.updateSettings { it.copy(defaultRule = AppRule(AppRule.ALL_APPS, Level.RING)) }
            settings.updateSounds { SoundsDto(defaultSoundUri = "content://sound/default", volumePercent = 50) }

            var seen: SoundsDto? = null
            val withCapture =
                NotificationPipeline(
                    settings = settings,
                    history = history,
                    ringer = Ringer { _, sounds -> seen = sounds },
                    now = { noon },
                )
            withCapture.onPosted("com.example.chat", "k", "t", "b", noon)

            assertEquals("content://sound/default", seen?.defaultSoundUri)
            assertEquals(50, seen?.volumePercent)
        }

    @Test
    fun aFreshStoreUsesTheSpecDefaults() =
        runTest {
            assertEquals(Profile.MY_RULES, settings.profile.first())
            assertEquals(7, settings.retentionDays.first())
            assertEquals(CallTier.ALLOW_LIST, settings.settings.first().defaultCallTier)
            assertEquals(SoundsDto(), settings.sounds.first())
        }
}
