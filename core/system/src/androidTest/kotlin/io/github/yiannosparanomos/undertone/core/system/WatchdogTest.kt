package io.github.yiannosparanomos.undertone.core.system

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.system.dnd.DndController
import io.github.yiannosparanomos.undertone.core.system.watchdog.ListenerHeartbeat
import io.github.yiannosparanomos.undertone.core.system.watchdog.Watchdog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The watchdog's job is the failure path, so that is what is tested: in this test process
 * UnderTone's listener is not an enabled notification listener, which is exactly the state a
 * force-stop leaves behind.
 */
class WatchdogTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val heartbeat = ListenerHeartbeat(context)

    @Before
    fun resetHeartbeat() {
        heartbeat.markStopped()
        heartbeat.warningShown = false
    }

    @Test
    fun ourListenerIsNotEnabledInThisTestProcess() {
        assertFalse(Watchdog.isListenerEnabled(context))
    }

    @Test
    fun anUnrelatedComponentNameIsNotMistakenForOurs() {
        assertFalse(Watchdog.isListenerEnabled(context, "com.example.SomeOtherListener"))
    }

    @Test
    fun anUnhealthyCheckReleasesDndAndWarnsExactlyOnce() {
        val watchdog =
            Watchdog(
                context = context,
                dnd = DndController.from(context),
                heartbeat = heartbeat,
                notificationManager = context.getSystemService(NotificationManager::class.java),
            )

        assertFalse("with no notification access the watchdog must report unhealthy", watchdog.check())
        assertTrue("the user should have been warned", heartbeat.warningShown)

        // A second check must not nag again.
        assertFalse(watchdog.check())
        assertTrue(heartbeat.warningShown)
        watchdog.clearWarning()
    }

    @Test
    fun theHeartbeatRemembersWhetherTheListenerWasRunning() {
        heartbeat.markAlive()
        assertTrue(heartbeat.wasAlive)
        assertTrue(heartbeat.lastSeenAtMillis > 0)

        heartbeat.markStopped()
        assertFalse(heartbeat.wasAlive)
    }
}
