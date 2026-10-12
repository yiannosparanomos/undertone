package io.github.yiannosparanomos.undertone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yiannosparanomos.undertone.core.data.settings.UndertoneSettingsStore
import io.github.yiannosparanomos.undertone.core.domain.model.Profile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator smoke test (layer 2): the app starts, the profile switch changes the status line, and
 * every screen opens.
 *
 * The profile is a real persisted setting now, so the test sets it before and puts it back
 * after; otherwise one run would decide the next one's starting state.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    private val store =
        UndertoneSettingsStore.get(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    fun startFromAKnownState() =
        runBlocking {
            store.setProfile(Profile.MY_RULES)
            // Overrides persist, so a previous run must not decide this one's starting point.
            store.setOverride(null)
        }

    @After
    fun restore() =
        runBlocking {
            store.setProfile(Profile.MY_RULES)
            store.setOverride(null)
        }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchingToAllowAllUpdatesTheStatus() {
        compose.onNodeWithText("Your rules decide what rings.").assertIsDisplayed()
        compose.onNodeWithText("Allow all").performScrollTo().performClick()
        awaitText("Everything comes through, like a normal phone.")
    }

    @Test
    fun everyScreenOpensFromHome() {
        listOf(
            "Apps" to "APPS",
            "Calls" to "CALLS",
            "Rules" to "RULES",
            "Held and log" to "HELD AND LOG",
            "Sounds" to "SOUNDS",
            "Health check" to "HEALTH CHECK",
        ).forEach { (row, title) ->
            // Home scrolls, so the lower rows have to be brought into view before they can be
            // tapped -- which is also what a person does.
            compose.onNodeWithText(row).performScrollTo().performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText("← Back").performScrollTo().performClick()
        }
    }

    @Test
    fun theQuietOverrideShowsOnHomeAndCanBeEnded() {
        // Setting an override is a DataStore write that comes back through a flow, so each step
        // waits for the text to change rather than assuming it already has.
        awaitText("No override. Your schedule applies.")
        compose.onNodeWithText("Quiet 1h").performScrollTo().performClick()
        awaitText("Quiet until")
        compose.onNodeWithText("End override").performScrollTo().performClick()
        awaitText("No override. Your schedule applies.")
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
