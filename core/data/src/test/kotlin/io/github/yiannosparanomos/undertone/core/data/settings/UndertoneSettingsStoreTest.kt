package io.github.yiannosparanomos.undertone.core.data.settings

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.yiannosparanomos.undertone.core.domain.model.AppRule
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import io.github.yiannosparanomos.undertone.core.domain.model.Level
import io.github.yiannosparanomos.undertone.core.domain.model.Override
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

class UndertoneSettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun store(name: String = "settings.json"): UndertoneSettingsStore =
        UndertoneSettingsStore(
            DataStoreFactory.create(
                serializer = StoredStateSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { StoredStateDto() },
                produceFile = { folder.newFile(name) },
            ),
        )

    @Test
    fun `starts on My rules with the domain defaults`() =
        runTest {
            val store = store()
            assertEquals(Profile.MY_RULES, store.profile.first())
            assertEquals(7, store.retentionDays.first())
            assertNull(store.override(LocalDateTime.now()).first())
        }

    @Test
    fun `the profile can be switched and read back`() =
        runTest {
            val store = store()
            store.setProfile(Profile.ALLOW_ALL)
            assertEquals(Profile.ALLOW_ALL, store.profile.first())
        }

    @Test
    fun `settings updates are visible to readers`() =
        runTest {
            val store = store()
            store.updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }
            assertEquals(CallTier.NOBODY, store.settings.first().defaultCallTier)

            store.updateSettings { current ->
                current.copy(
                    appRules = current.appRules + ("com.example.x" to AppRule("com.example.x", Level.BLOCKED)),
                )
            }
            assertEquals(
                Level.BLOCKED,
                store.settings
                    .first()
                    .ruleFor("com.example.x")
                    .inHours,
            )
            // The earlier change is still there.
            assertEquals(CallTier.NOBODY, store.settings.first().defaultCallTier)
        }

    @Test
    fun `an override that has run out reads as no override`() =
        runTest {
            val store = store()
            val until = LocalDateTime.of(2026, 10, 12, 18, 0)
            store.setOverride(Override.Quiet(until = until))

            assertNotNull(store.override(until.minusMinutes(1)).first())
            assertNull("an expired override must not apply", store.override(until).first())
            assertNull(store.override(until.plusHours(2)).first())
        }

    @Test
    fun `the rule state snapshot carries profile settings and a live override`() =
        runTest {
            val store = store()
            val until = LocalDateTime.of(2026, 10, 12, 18, 0)
            store.setProfile(Profile.MY_RULES)
            store.updateSettings { it.copy(defaultCallTier = CallTier.CONTACTS) }
            store.setOverride(Override.LetThrough(until = until))

            val live = store.ruleState(until.minusMinutes(5)).first()
            assertEquals(Profile.MY_RULES, live.profile)
            assertEquals(CallTier.CONTACTS, live.settings.defaultCallTier)
            assertNotNull(live.override)

            assertNull(store.ruleState(until.plusMinutes(5)).first().override)
        }

    @Test
    fun `retention must be at least a day`() =
        runTest {
            val store = store()
            store.setRetentionDays(30)
            assertEquals(30, store.retentionDays.first())
            val refused = runCatching { store.setRetentionDays(0) }.exceptionOrNull()
            assertTrue("zero days must be refused", refused is IllegalArgumentException)
            assertEquals("the refused change must not be stored", 30, store.retentionDays.first())
        }

    @Test
    fun `export writes the rules and leaves the override out`() =
        runTest {
            val store = store()
            store.updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }
            store.setOverride(Override.Quiet(until = LocalDateTime.of(2030, 1, 1, 0, 0)))

            val out = ByteArrayOutputStream()
            store.export(out)
            val text = out.toString("UTF-8")

            val decoded = SettingsCodec.decode(text)
            assertEquals(CallTier.NOBODY, decoded.settings.toDomain().defaultCallTier)
            assertNull("an export must not carry the momentary override", decoded.override)
        }

    @Test
    fun `an exported file imports into another store unchanged`() =
        runTest {
            val source = store("a.json")
            source.updateSettings { current ->
                current.copy(
                    defaultRule = AppRule(AppRule.ALL_APPS, Level.HIDDEN),
                    appRules = mapOf("com.example.chat" to AppRule("com.example.chat", Level.RING)),
                    defaultCallTier = CallTier.CONTACTS,
                )
            }
            source.setRetentionDays(14)

            val out = ByteArrayOutputStream()
            source.export(out)

            val target = store("b.json")
            target.import(ByteArrayInputStream(out.toByteArray()))

            assertEquals(source.settings.first(), target.settings.first())
            assertEquals(14, target.retentionDays.first())
        }

    @Test
    fun `a bad import leaves the existing settings untouched`() =
        runTest {
            val store = store()
            store.updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }

            val error =
                runCatching {
                    store.import(
                        ByteArrayInputStream("{ not settings".toByteArray()),
                    )
                }.exceptionOrNull()
            assertTrue("a junk import must be refused", error is StoredStateException)
            assertTrue(error!!.message!!.isNotBlank())
            assertEquals(CallTier.NOBODY, store.settings.first().defaultCallTier)
        }

    @Test
    fun `an import keeps this phone's own override`() =
        runTest {
            val source = store("a.json")
            source.updateSettings { it.copy(defaultCallTier = CallTier.EVERYONE) }
            val out = ByteArrayOutputStream()
            source.export(out)

            val target = store("b.json")
            val until = LocalDateTime.of(2030, 1, 1, 0, 0)
            target.setOverride(Override.Quiet(until = until))
            target.import(ByteArrayInputStream(out.toByteArray()))

            assertEquals(CallTier.EVERYONE, target.settings.first().defaultCallTier)
            assertNotNull("the running override belongs to this phone", target.override(until.minusDays(1)).first())
        }

    @Test
    fun `a corrupt settings file falls back to defaults instead of crashing`() =
        runTest {
            val file = folder.newFile("corrupt.json")
            file.writeText("}}} definitely not json")
            val store =
                UndertoneSettingsStore(
                    DataStoreFactory.create(
                        serializer = StoredStateSerializer,
                        corruptionHandler = ReplaceFileCorruptionHandler { StoredStateDto() },
                        produceFile = { file },
                    ),
                )
            assertEquals(Profile.MY_RULES, store.profile.first())
        }

    @Test
    fun `settings survive being written and read by a new store on the same file`() =
        runTest {
            // DataStore allows only one live instance per file, so each store gets its own scope
            // and is shut down before the next one opens -- which is what an app restart does.
            val file = folder.newFile("persist.json")

            suspend fun <T> onFile(block: suspend (UndertoneSettingsStore) -> T): T {
                val scope = CoroutineScope(Job() + Dispatchers.IO)
                return try {
                    block(
                        UndertoneSettingsStore(
                            DataStoreFactory.create(
                                serializer = StoredStateSerializer,
                                corruptionHandler = ReplaceFileCorruptionHandler { StoredStateDto() },
                                scope = scope,
                                produceFile = { file },
                            ),
                        ),
                    )
                } finally {
                    scope.cancel()
                }
            }
            onFile { it.setProfile(Profile.SYSTEM_DND) }
            assertEquals(Profile.SYSTEM_DND, onFile { it.profile.first() })
        }
}
