package io.github.yiannosparanomos.undertone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke test (layer 2): the app starts and the profile switch changes the status line. */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchingToAllowAllUpdatesTheStatus() {
        compose.onNodeWithText("Your rules decide what rings.").assertIsDisplayed()
        compose.onNodeWithText("Allow all").performClick()
        compose.onNodeWithText("Everything comes through, like a normal phone.").assertIsDisplayed()
    }
}
