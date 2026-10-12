package io.github.yiannosparanomos.undertone.core.data.settings

import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.domain.model.CallTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Regression test for a bug the emulator caught during M4.
 *
 * DataStore allows one active instance per file. UnderTone runs the notification listener, the
 * call screening service, the boot receiver and the UI in one process, and each was building its
 * own store — so whichever came second threw `IllegalStateException`. Call screening caught it
 * and let the call ring, which is the safe direction but ignored the user's ring tier entirely.
 */
class SettingsStoreSingletonTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun severalComponentsAskingForTheStoreGetTheSameOne() {
        val fromListener = UndertoneSettingsStore.get(context)
        val fromCallScreening = UndertoneSettingsStore.get(context)
        val fromReceiver = UndertoneSettingsStore.get(context)

        assertSame(fromListener, fromCallScreening)
        assertSame(fromListener, fromReceiver)
    }

    @Test
    fun theSharedStoreIsUsableByEveryCaller() =
        runTest {
            // Before the fix, this second read threw rather than returning settings.
            UndertoneSettingsStore.get(context).updateSettings { it.copy(defaultCallTier = CallTier.NOBODY) }
            assertEquals(
                CallTier.NOBODY,
                UndertoneSettingsStore
                    .get(context)
                    .settings
                    .first()
                    .defaultCallTier,
            )

            // Put it back so the shared file does not leak into other tests on this device.
            UndertoneSettingsStore.get(context).updateSettings { it.copy(defaultCallTier = CallTier.ALLOW_LIST) }
        }
}
