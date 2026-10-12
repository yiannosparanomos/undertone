package io.github.yiannosparanomos.undertone.core.system

import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.data.db.UndertoneDatabase
import io.github.yiannosparanomos.undertone.core.data.history.HistoryStore
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.domain.engine.CallOutcome
import io.github.yiannosparanomos.undertone.core.domain.engine.DecisionReason
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatCallerSettings
import io.github.yiannosparanomos.undertone.core.domain.model.RepeatGroup
import io.github.yiannosparanomos.undertone.core.system.calls.CallPipeline
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDateTime

/** The call path end to end: real engine, real database, real settings. */
class CallPipelineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: UndertoneDatabase
    private lateinit var history: HistoryStore
    private lateinit var settings: UndertoneSettingsStore
    private lateinit var settingsFile: File
    private lateinit var pipeline: CallPipeline

    private val noon = LocalDateTime.of(2026, 10, 12, 12, 0)
    private val unknown = "+35799123456"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, UndertoneDatabase::class.java).build()
        history = HistoryStore.from(database)
        settingsFile = File.createTempFile("calls-settings", ".json", context.cacheDir).apply { delete() }
        settings =
            UndertoneSettingsStore(
                DataStoreFactory.create(
                    serializer = StoredStateSerializerTestAccess.serializer,
                    produceFile = { settingsFile },
                ),
            )
        pipeline = CallPipeline(settings, history)
    }

    @After
    fun tearDown() {
        database.close()
        settingsFile.delete()
    }

    @Test
    fun theEveryoneTierLetsAnUnknownNumberRing() =
        runTest {
            settings.updateSettings { it.copy(defaultCallTier = CallTier.EVERYONE) }
            val decision = pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon)
            assertEquals(CallOutcome.RING, decision.outcome)
            assertEquals(DecisionReason.RING_TIER, decision.reason)
        }

    @Test
    fun theContactsTierSilencesAnUnknownNumberButNotAContact() =
        runTest {
            settings.updateSettings { it.copy(defaultCallTier = CallTier.CONTACTS) }

            assertEquals(
                CallOutcome.SILENT,
                pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon).outcome,
            )
            assertEquals(
                CallOutcome.RING,
                pipeline.onIncomingCall("+35799000001", isContact = true, isOnAllowList = false, at = noon).outcome,
            )
        }

    @Test
    fun theNobodyTierSilencesEvenAStarredContact() =
        runTest {
            settings.updateSettings {
                it.copy(
                    defaultCallTier = CallTier.NOBODY,
                    repeatCallers = RepeatCallerSettings(enabled = false),
                )
            }
            val decision = pipeline.onIncomingCall(unknown, isContact = true, isOnAllowList = true, at = noon)
            assertEquals(CallOutcome.SILENT, decision.outcome)
            assertEquals(CallTier.NOBODY, decision.tier)
        }

    @Test
    fun aSilencedCallIsStillRecordedSoTheCallerCanBreakThroughByTryingAgain() =
        runTest {
            settings.updateSettings {
                it.copy(
                    defaultCallTier = CallTier.NOBODY,
                    repeatCallers =
                        RepeatCallerSettings(
                            enabled = true,
                            group = RepeatGroup.EVERYONE,
                            callsNeeded = 2,
                            window = Duration.ofMinutes(2),
                        ),
                )
            }

            val first = pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon)
            assertEquals("the first call is silenced", CallOutcome.SILENT, first.outcome)

            val second =
                pipeline.onIncomingCall(
                    unknown,
                    isContact = false,
                    isOnAllowList = false,
                    at = noon.plusMinutes(1),
                )
            assertEquals(CallOutcome.RING, second.outcome)
            assertEquals(DecisionReason.REPEAT_CALLER, second.reason)
        }

    @Test
    fun aSecondCallOutsideTheWindowDoesNotBreakThrough() =
        runTest {
            settings.updateSettings {
                it.copy(
                    defaultCallTier = CallTier.NOBODY,
                    repeatCallers =
                        RepeatCallerSettings(
                            enabled = true,
                            group = RepeatGroup.EVERYONE,
                            callsNeeded = 2,
                            window = Duration.ofMinutes(2),
                        ),
                )
            }
            pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon)
            val later =
                pipeline.onIncomingCall(
                    unknown,
                    isContact = false,
                    isOnAllowList = false,
                    at = noon.plusMinutes(3),
                )
            assertEquals(CallOutcome.SILENT, later.outcome)
        }

    @Test
    fun aDifferentCallerDoesNotCountTowardsARepeat() =
        runTest {
            settings.updateSettings {
                it.copy(
                    defaultCallTier = CallTier.NOBODY,
                    repeatCallers =
                        RepeatCallerSettings(enabled = true, group = RepeatGroup.EVERYONE, callsNeeded = 2),
                )
            }
            pipeline.onIncomingCall("+35799111111", isContact = false, isOnAllowList = false, at = noon)
            val other =
                pipeline.onIncomingCall(
                    "+35799222222",
                    isContact = false,
                    isOnAllowList = false,
                    at = noon.plusSeconds(30),
                )
            assertEquals(CallOutcome.SILENT, other.outcome)
        }

    @Test
    fun theSameCallerIsRecognisedThroughDifferentNumberFormatting() =
        runTest {
            settings.updateSettings {
                it.copy(
                    defaultCallTier = CallTier.NOBODY,
                    repeatCallers =
                        RepeatCallerSettings(enabled = true, group = RepeatGroup.EVERYONE, callsNeeded = 2),
                )
            }
            pipeline.onIncomingCall("+30 210 123 4567", isContact = false, isOnAllowList = false, at = noon)
            // The engine compares digits only, so the spaced and unspaced forms are one caller.
            val again =
                pipeline.onIncomingCall(
                    "+302101234567",
                    isContact = false,
                    isOnAllowList = false,
                    at = noon.plusSeconds(20),
                )
            assertEquals(CallOutcome.RING, again.outcome)
            assertEquals(DecisionReason.REPEAT_CALLER, again.reason)
        }

    @Test
    fun anOverridesTierWinsWhileItIsRunning() =
        runTest {
            settings.updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }
            settings.setOverride(Override.LetThrough(until = noon.plusMinutes(30), callTier = CallTier.EVERYONE))

            assertEquals(
                CallOutcome.RING,
                pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon).outcome,
            )
            // Once it has run out the default tier applies again.
            assertEquals(
                CallOutcome.SILENT,
                pipeline
                    .onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon.plusHours(1))
                    .outcome,
            )
        }

    @Test
    fun allowAllAndSystemDndStepAside() =
        runTest {
            settings.updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }

            settings.setProfile(Profile.ALLOW_ALL)
            assertEquals(
                CallOutcome.PASS_THROUGH,
                pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon).outcome,
            )

            settings.setProfile(Profile.SYSTEM_DND)
            assertEquals(
                CallOutcome.PASS_THROUGH,
                pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon).outcome,
            )
        }

    @Test
    fun noCallIsEverRejected() =
        runTest {
            // Invariant 5 lives in the service's CallResponse, but the engine must never ask for
            // anything but RING, SILENT or PASS_THROUGH either.
            settings.updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }
            val outcomes =
                listOf(
                    pipeline.onIncomingCall(unknown, isContact = false, isOnAllowList = false, at = noon),
                    pipeline.onIncomingCall(unknown, isContact = true, isOnAllowList = true, at = noon),
                )
            outcomes.forEach {
                org.junit.Assert.assertTrue(
                    "unexpected outcome ${it.outcome}",
                    it.outcome in setOf(CallOutcome.RING, CallOutcome.SILENT, CallOutcome.PASS_THROUGH),
                )
            }
        }
}
