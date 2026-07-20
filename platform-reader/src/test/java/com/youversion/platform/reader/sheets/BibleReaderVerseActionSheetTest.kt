package com.youversion.platform.reader.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderVerseActionSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        colorsToRemove: List<HighlightColor> = emptyList(),
        colorsToAdd: List<HighlightColor> = emptyList(),
        onAddHighlight: (String) -> Unit = {},
        onRemoveHighlight: (String) -> Unit = {},
        onCopy: () -> Unit = {},
        onShare: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderVerseActionSheet(
                    colorsToRemove = colorsToRemove,
                    colorsToAdd = colorsToAdd,
                    onAddHighlight = onAddHighlight,
                    onRemoveHighlight = onRemoveHighlight,
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

        assertTrue(isCopied)
    }

    @Test
    fun `clicking Share button calls onShare`() {
        var isShared = false

        renderSheet(onShare = { isShared = true })

        composeTestRule.onNodeWithText("Share").performClick()

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

    // ----- Highlight Color Picker

    @Test
    fun `displays an add affordance for a color to add`() {
        renderSheet(colorsToAdd = listOf(HighlightColor.Yellow))

        composeTestRule.onNodeWithContentDescription("Add yellow highlight").assertIsDisplayed()
    }

    @Test
    fun `displays a remove affordance for a color to remove`() {
        renderSheet(colorsToRemove = listOf(HighlightColor.Yellow))

        composeTestRule.onNodeWithContentDescription("Remove yellow highlight").assertIsDisplayed()
    }

    @Test
    fun `clicking a color to add calls onAddHighlight with its hex`() {
        var addedHex: String? = null

        renderSheet(
            colorsToAdd = listOf(HighlightColor.Green),
            onAddHighlight = { addedHex = it },
        )

        composeTestRule.onNodeWithContentDescription("Add green highlight").performClick()

        assertEquals(HighlightColor.Green.hexColor, addedHex)
    }

    @Test
    fun `clicking a color to remove calls onRemoveHighlight with its hex`() {
        var removedHex: String? = null

        renderSheet(
            colorsToRemove = listOf(HighlightColor.Cyan),
            onRemoveHighlight = { removedHex = it },
        )

        composeTestRule.onNodeWithContentDescription("Remove cyan highlight").performClick()

        assertEquals(HighlightColor.Cyan.hexColor, removedHex)
    }

    @Test
    fun `a color present on some but not all verses renders both add and remove affordances`() {
        renderSheet(
            colorsToRemove = listOf(HighlightColor.Yellow),
            colorsToAdd = listOf(HighlightColor.Yellow),
        )

        composeTestRule.onNodeWithContentDescription("Remove yellow highlight").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add yellow highlight").assertIsDisplayed()
    }
}
