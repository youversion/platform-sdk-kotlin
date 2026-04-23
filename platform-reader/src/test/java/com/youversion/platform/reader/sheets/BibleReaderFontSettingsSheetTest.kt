package com.youversion.platform.reader.sheets

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.font.FontFamily
import com.youversion.platform.reader.FontDefinition
import com.youversion.platform.reader.ReaderFontSettings
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Cream
import com.youversion.platform.ui.theme.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderFontSettingsSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        onDismissRequest: () -> Unit = {},
        onSmallerFontClick: () -> Unit = {},
        onBiggerFontClick: () -> Unit = {},
        onFontClick: () -> Unit = {},
        onThemeSelect: (ReaderTheme) -> Unit = {},
        fontDefinition: FontDefinition = ReaderFontSettings.DEFAULT_FONT_DEFINITION,
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme(readerColorScheme = Cream) {
                BibleReaderFontSettingsSheet(
                    onDismissRequest = onDismissRequest,
                    onSmallerFontClick = onSmallerFontClick,
                    onBiggerFontClick = onBiggerFontClick,
                    onFontClick = onFontClick,
                    onThemeSelect = onThemeSelect,
                    fontDefinition = fontDefinition,
                )
            }
        }
    }

    // ----- FontSizeButtons

    @Test
    fun `displays two font size buttons with A labels`() {
        renderSheet()

        composeTestRule.onAllNodesWithText("A").assertCountEquals(2)
    }

    @Test
    fun `clicking smaller font button calls onSmallerFontClick`() {
        var isClicked = false

        renderSheet(onSmallerFontClick = { isClicked = true })

        composeTestRule.onNodeWithTag("smaller_font_button").performClick()

        assertTrue(isClicked)
    }

    @Test
    fun `clicking larger font button calls onBiggerFontClick`() {
        var isClicked = false

        renderSheet(onBiggerFontClick = { isClicked = true })

        composeTestRule.onNodeWithTag("larger_font_button").performClick()

        assertTrue(isClicked)
    }

    // ----- FontDisplayButton

    @Test
    fun `displays font name from fontDefinition`() {
        renderSheet(
            fontDefinition = FontDefinition("Custom Font", FontFamily.Default),
        )

        composeTestRule.onNodeWithText("Custom Font").assertIsDisplayed()
    }

    @Test
    fun `displays Font label`() {
        renderSheet()

        composeTestRule.onNodeWithText("Font").assertIsDisplayed()
    }

    @Test
    fun `clicking font display button calls onFontClick`() {
        var isClicked = false

        renderSheet(onFontClick = { isClicked = true })

        composeTestRule.onNodeWithTag("font_display_button").performClick()

        assertTrue(isClicked)
    }

    // ----- ThemePicker

    @Test
    fun `renders all theme items`() {
        renderSheet()

        ReaderTheme.allThemes.forEach { theme ->
            val tag = "theme_${theme.id}"
            composeTestRule
                .onNode(hasScrollToNodeAction())
                .performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun `selected theme shows checkmark indicator`() {
        renderSheet()

        composeTestRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("theme_4"))
        composeTestRule
            .onNode(hasTestTag("theme_4").and(hasContentDescription("Selected")))
            .assertIsDisplayed()
    }

    @Test
    fun `unselected theme shows circle outline indicator`() {
        renderSheet()

        composeTestRule
            .onNode(hasTestTag("theme_1").and(hasContentDescription("Selected")))
            .assertDoesNotExist()
    }

    @Test
    fun `clicking a theme item calls onThemeSelect with the correct theme`() {
        var selectedTheme: ReaderTheme? = null

        renderSheet(onThemeSelect = { selectedTheme = it })

        composeTestRule.onNodeWithTag("theme_3").performClick()

        assertEquals(ReaderTheme.allThemes.first { it.id == 3 }, selectedTheme)
    }
}
