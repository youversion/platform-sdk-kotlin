package com.youversion.platform.reader.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderHalfPillPickerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `BibleReaderHalfPillPicker renders in normal mode`() {
        composeTestRule.setContent {
            BibleReaderHalfPillPicker(
                "Genesis 1",
                versionAbbreviation = "NIV",
                handleVersionTap = {},
                handleChapterTap = {},
                foregroundColor = Color.Black,
                buttonColor = Color.White,
                compactMode = false,
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIV").assertIsDisplayed()
    }

    @Test
    fun `BibleReaderHalfPillPicker renders in compact mode`() {
        composeTestRule.setContent {
            BibleReaderHalfPillPicker(
                "Genesis 1",
                versionAbbreviation = "NIV",
                handleVersionTap = {},
                handleChapterTap = {},
                foregroundColor = Color.Black,
                buttonColor = Color.White,
                compactMode = true,
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIV").assertIsDisplayed()
    }

    @Test
    fun `clicking the buttons triggers the callbacks in normal mode`() {
        var versionTapped = false
        var chapterTapped = false
        composeTestRule.setContent {
            BibleReaderHalfPillPicker(
                "Genesis 1",
                versionAbbreviation = "NIV",
                handleVersionTap = { versionTapped = true },
                handleChapterTap = { chapterTapped = true },
                foregroundColor = Color.Black,
                buttonColor = Color.White,
                compactMode = false,
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").performClick()
        assertTrue(chapterTapped)
        composeTestRule.onNodeWithText("NIV").performClick()
        assertTrue(versionTapped)
    }

    @Test
    fun `clicking the buttons triggers the callbacks in compact mode`() {
        var versionTapped = false
        var chapterTapped = false
        composeTestRule.setContent {
            BibleReaderHalfPillPicker(
                "Genesis 1",
                versionAbbreviation = "NIV",
                handleVersionTap = { versionTapped = true },
                handleChapterTap = { chapterTapped = true },
                foregroundColor = Color.Black,
                buttonColor = Color.White,
                compactMode = true,
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").performClick()
        assertTrue(chapterTapped)
        composeTestRule.onNodeWithText("NIV").performClick()
        assertTrue(versionTapped)
    }
}
