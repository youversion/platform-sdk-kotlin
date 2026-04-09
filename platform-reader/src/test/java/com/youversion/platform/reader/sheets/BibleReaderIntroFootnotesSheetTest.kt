package com.youversion.platform.reader.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BibleReaderIntroFootnotesSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        onDismissRequest: () -> Unit = {},
        footnotes: List<AnnotatedString> = emptyList(),
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderIntroFootnotesSheet(
                    onDismissRequest = onDismissRequest,
                    footnotes = footnotes,
                )
            }
        }
    }

    // ----- Header

    @Test
    fun `displays Footnote header label`() {
        renderSheet()

        composeTestRule.onNodeWithText("Footnote").assertIsDisplayed()
    }

    // ----- Footnotes

    @Test
    fun `renders all footnote texts without enumeration`() {
        renderSheet(
            footnotes =
                listOf(
                    AnnotatedString("First intro footnote"),
                    AnnotatedString("Second intro footnote"),
                ),
        )

        composeTestRule.onNodeWithText("First intro footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second intro footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("a.").assertDoesNotExist()
        composeTestRule.onNodeWithText("b.").assertDoesNotExist()
    }

    @Test
    fun `renders empty state when no footnotes provided`() {
        renderSheet(footnotes = emptyList())

        composeTestRule.onNodeWithText("Footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("First intro footnote").assertDoesNotExist()
    }
}
