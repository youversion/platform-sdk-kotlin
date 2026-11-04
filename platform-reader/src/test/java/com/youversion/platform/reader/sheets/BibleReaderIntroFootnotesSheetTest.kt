package com.youversion.platform.reader.sheets

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BibleReaderIntroFootnotesSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        onDismissRequest: () -> Unit = {},
        footnotes: List<AnnotatedString> = emptyList(),
        isVersionRightToLeft: Boolean = false,
        deviceDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides deviceDirection) {
                    BibleReaderIntroFootnotesSheet(
                        onDismissRequest = onDismissRequest,
                        footnotes = footnotes,
                        isVersionRightToLeft = isVersionRightToLeft,
                    )
                }
            }
        }
    }

    private fun textDirection(text: String): ResolvedTextDirection {
        val layoutResults = mutableListOf<TextLayoutResult>()
        composeTestRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)
        return layoutResults.first().getParagraphDirection(0)
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

    // ----- Text Direction

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [34])
    fun `a right-to-left footnote reads right to left on a left-to-right device`() {
        renderSheet(footnotes = listOf(AnnotatedString("Selah שלום")), isVersionRightToLeft = true)

        assertEquals(ResolvedTextDirection.Rtl, textDirection("Selah שלום"))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [34])
    fun `a left-to-right footnote reads left to right on a right-to-left device`() {
        renderSheet(
            footnotes = listOf(AnnotatedString("שלום Selah")),
            isVersionRightToLeft = false,
            deviceDirection = LayoutDirection.Rtl,
        )

        assertEquals(ResolvedTextDirection.Ltr, textDirection("שלום Selah"))
    }
}
