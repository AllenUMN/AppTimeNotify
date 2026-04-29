package com.example.apptimenotify

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.ExperimentalTestApi

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class AppFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "appops set $packageName GET_USAGE_STATS allow"
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFullFlow() {
        // 1. Wait for idle
        composeTestRule.waitForIdle()

        // 2. Wait for the loading to finish
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasTestTag("search_bar") and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // 3. Click on an app item
        composeTestRule.onAllNodes(hasTestTag("app_item"))
            .onFirst()
            .performClick()

        // 4. Verify we are on the Time Limit Screen
        composeTestRule.onNodeWithText("Set limit for", substring = true)
            .assertIsDisplayed()

        // 5. Enter hours and minutes
        composeTestRule.onNodeWithTag("hours_input").performTextInput("1")
        composeTestRule.onNodeWithTag("minutes_input").performTextInput("30")

        // 6. Click Confirm
        composeTestRule.onNodeWithTag("confirm_button").performClick()

        // 7. Verify we are back on the list screen and tracking is active
        composeTestRule.onNodeWithText("Tracking:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Limit: 1h 30m per day").assertIsDisplayed()

        // 8. Stop tracking
        composeTestRule.onNodeWithText("Stop Tracking").performClick()

        // 9. Verify tracking card is gone
        composeTestRule.onNodeWithText("Tracking:", substring = true).assertDoesNotExist()
    }
}
