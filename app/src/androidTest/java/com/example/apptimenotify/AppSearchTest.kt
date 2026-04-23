package com.example.apptimenotify

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSearchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testSearchAndSelectApp() {
        // 1. Wait for the app to load and find the search bar
        composeTestRule.onNodeWithTag("search_bar")
            .assertIsDisplayed()

        // 2. Type "a" to filter the list
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("a")

        // 3. Find the first clickable item in the list and click it
        // We'll use a matcher that looks for things with a click action
        composeTestRule.onAllNodes(hasClickAction())
            .onFirst()
            .performClick()

        // 4. Verify that the "Selected:" text appears
        composeTestRule.onNodeWithText("Selected:", substring = true)
            .assertIsDisplayed()
        
        // 5. Clear selection
        composeTestRule.onNodeWithText("Clear Selection")
            .performClick()
            
        // 6. Verify selection text is gone
        composeTestRule.onNodeWithText("Selected:", substring = true)
            .assertDoesNotExist()
    }
}
