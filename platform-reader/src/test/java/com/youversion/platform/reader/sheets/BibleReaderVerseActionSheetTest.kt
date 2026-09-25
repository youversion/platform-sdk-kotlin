package com.youversion.platform.reader.sheets

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Charcoal
import com.youversion.platform.ui.theme.PureWhite
import com.youversion.platform.ui.theme.TrueBlack
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
        showsHighlightColors: Boolean = true,
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
                    showsHighlightColors = showsHighlightColors,
                    onAddHighlight = onAddHighlight,
                    onRemoveHighlight = onRemoveHighlight,
                    onCopy = onCopy,
                    onShare = onShare,
                )
            }
        }
    }

    @Test
    fun `hides the highlight colors when sign-in is unavailable`() {
        renderSheet(
            colorsToRemove = listOf(HighlightColor.Yellow),
            colorsToAdd = listOf(HighlightColor.Green),
            showsHighlightColors = false,
        )

        composeTestRule.onNodeWithContentDescription("Remove yellow highlight").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Add green highlight").assertDoesNotExist()
    }

    @Test
    fun `keeps copy and share available when the highlight colors are hidden`() {
        renderSheet(
            colorsToAdd = listOf(HighlightColor.Green),
            showsHighlightColors = false,
        )

        composeTestRule.onNodeWithContentDescription("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share").assertIsDisplayed()
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
            colorsToRemove = listOf(HighlightColor.Blue),
            onRemoveHighlight = { removedHex = it },
        )

        composeTestRule.onNodeWithContentDescription("Remove blue highlight").performClick()

        assertEquals(HighlightColor.Blue.hexColor, removedHex)
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

    // ----- Swatch Color

    @Test
    fun `swatchColor keeps the palette color on a light theme`() {
        assertEquals(HighlightColor.Yellow.color, HighlightColor.Yellow.swatchColor(PureWhite))
    }

    @Test
    fun `swatchColor mixes the palette color into the reader background on a dark theme`() {
        val swatchColor = HighlightColor.Yellow.swatchColor(Charcoal)

        assertEquals(1f, swatchColor.alpha)
        assertEquals((255f * 0.2f + 43f * 0.8f) / 255f, swatchColor.red, absoluteTolerance = 0.005f)
        assertEquals((236f * 0.2f + 48f * 0.8f) / 255f, swatchColor.green, absoluteTolerance = 0.005f)
        assertEquals((91f * 0.2f + 49f * 0.8f) / 255f, swatchColor.blue, absoluteTolerance = 0.005f)
    }

    @Test
    fun `swatchColor lets more of the palette color through on the black theme`() {
        val swatchColor = HighlightColor.Yellow.swatchColor(TrueBlack)

        assertEquals((255f * 0.25f + 18f * 0.75f) / 255f, swatchColor.red, absoluteTolerance = 0.005f)
        assertEquals((236f * 0.25f + 18f * 0.75f) / 255f, swatchColor.green, absoluteTolerance = 0.005f)
        assertEquals((91f * 0.25f + 18f * 0.75f) / 255f, swatchColor.blue, absoluteTolerance = 0.005f)
    }

    // ----- Palette

    @Test
    fun `the palette offers the six default highlight colors`() {
        assertEquals(
            listOf("ffec5b", "b4ffc1", "bbf4ff", "ffdca7", "ffcff8", "dfdcff"),
            HighlightColor.entries.map { it.hexColor },
        )
    }

    @Test
    fun `each palette entry renders the color it stores`() {
        HighlightColor.entries.forEach { highlightColor ->
            assertEquals(
                Color(0xFF000000L or highlightColor.hexColor.toLong(radix = 16)),
                highlightColor.color,
            )
        }
    }
}
