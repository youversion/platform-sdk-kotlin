package com.youversion.platform.ui.views.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderTopAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the BibleReaderTopAppBar with string title overload`() {
        composeTestRule.setContent {
            BibleReaderTopAppBar(title = "Title", onBackClick = {})
        }

        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `renders the BibleReaderTopAppBar with composable title overload`() {
        composeTestRule.setContent {
            BibleReaderTopAppBar(title = { Text("Title") }, onBackClick = {})
        }

        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `back button triggers the onBackClick callback`() {
        var backClicked = false
        composeTestRule.setContent {
            BibleReaderTopAppBar(title = { Text("Title") }, onBackClick = { backClicked = true })
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun `actions button renders and triggers the callback`() {
        var iconButtonClicked = false
        composeTestRule.setContent {
            BibleReaderTopAppBar(
                title = { Text("Title") },
                onBackClick = {},
                actions = {
                    IconButton(
                        onClick =
                            { iconButtonClicked = true },
                    ) { Icon(imageVector = Icons.Default.Share, contentDescription = "Share") }
                },
            )
        }

        composeTestRule.onNodeWithContentDescription("Share").performClick()
        assertTrue(iconButtonClicked)
    }
}
