package com.example.apptimenotify

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.ExperimentalTestApi

@RunWith(AndroidJUnit4::class)
class AppSearchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testSearchAndSelectApp() {
        // 1. Wait for the loading to finish by waiting for the search bar to be ENABLED
        // This is more reliable than waiting for a tag that might already be gone
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasTestTag("search_bar") and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithTag("search_bar")
            .assertIsDisplayed()
            .assertIsEnabled()

        // 2. Type "a" to filter the list
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("a")

        // 3. Wait for the list to be populated (searching might take a split second)
        // Note: We use hasTestTag("app_item") because we added it to each item in MainActivity
        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("app_item"), timeoutMillis = 5000)

        // 4. Click the first app item
        composeTestRule.onAllNodes(hasTestTag("app_item"))
            .onFirst()
            .performClick()

        // 5. Verify that the "Selected:" text appears
        composeTestRule.onNodeWithText("Selected:", substring = true)
            .assertIsDisplayed()
        
        // 6. Clear selection
        composeTestRule.onNodeWithText("Clear Selection")
            .performClick()
            
        // 7. Verify selection text is gone
        composeTestRule.onNodeWithText("Selected:", substring = true)
            .assertDoesNotExist()
    }
}
