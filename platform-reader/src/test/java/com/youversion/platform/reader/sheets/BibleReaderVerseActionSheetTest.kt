package com.youversion.platform.reader.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderVerseActionSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        selectedVerses: Set<BibleReference> = emptySet(),
        onCopy: () -> Unit = {},
        onShare: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderVerseActionSheet(
                    selectedVerses = selectedVerses,
                    onCopy = onCopy,
                    onShare = onShare,
                )
            }
        }
    }

    // ----- Action Buttons

    @Test
    fun `displays Copy button`() {
        renderSheet()

        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
    }

    @Test
    fun `displays Share button`() {
        renderSheet()

        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
    }

    @Test
    fun `clicking Copy button calls onCopy`() {
        var isCopied = false

        renderSheet(onCopy = { isCopied = true })

        composeTestRule.onNodeWithText("Copy").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isCopied)
    }

    @Test
    fun `clicking Share button calls onShare`() {
        var isShared = false

        renderSheet(onShare = { isShared = true })

        composeTestRule.onNodeWithText("Share").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isShared)
    }

    @Test
    fun `Copy button has correct content description`() {
        renderSheet()

        composeTestRule.onNodeWithContentDescription("Copy").assertIsDisplayed()
    }

    @Test
    fun `Share button has correct content description`() {
        renderSheet()

        composeTestRule.onNodeWithContentDescription("Share").assertIsDisplayed()
    }
}
