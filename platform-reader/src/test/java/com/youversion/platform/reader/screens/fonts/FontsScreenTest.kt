package com.youversion.platform.reader.screens.fonts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontFamily
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.FontDefinition
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FontsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
    private val bibleReaderStateFlow =
        MutableStateFlow(
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                defaultFontDefinitions =
                    listOf(
                        FontDefinition("Serif", FontFamily.Serif),
                        FontDefinition("Monospace", FontFamily.Monospace),
                    ),
                selectedFontDefinition = FontDefinition("Serif", FontFamily.Serif),
            ),
        )
    private val mockBibleReaderViewModel =
        mockk<BibleReaderViewModel>(relaxed = true) {
            every { state } returns bibleReaderStateFlow
        }

    @Test
    fun `displays the Font title`() {
        composeTestRule.setContent { FontsScreen(viewModel = mockBibleReaderViewModel, onBackClick = {}) }

        composeTestRule.onNodeWithText("Font").assertIsDisplayed()
    }

    @Test
    fun `renders all font definitions`() {
        composeTestRule.setContent { FontsScreen(viewModel = mockBibleReaderViewModel, onBackClick = {}) }

        composeTestRule.onNodeWithText("Serif").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monospace").assertIsDisplayed()
    }

    @Test
    fun `renders the check icon when the font is selected`() {
        composeTestRule.setContent { FontsScreen(viewModel = mockBibleReaderViewModel, onBackClick = {}) }

        composeTestRule
            .onNode(
                hasContentDescription("Font selected check") and hasAnySibling(hasText("Serif")),
                useUnmergedTree = true,
            ).assertExists()
        composeTestRule
            .onNode(
                hasContentDescription("Font selected check") and hasAnySibling(hasText("Monospace")),
                useUnmergedTree = true,
            ).assertDoesNotExist()
    }

    @Test
    fun `Row click triggers the onClick and onBackClick`() {
        var onBackClicked = false
        composeTestRule.setContent {
            FontsScreen(
                viewModel = mockBibleReaderViewModel,
                onBackClick = { onBackClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Monospace").performClick()
        composeTestRule.waitForIdle()
        verify {
            mockBibleReaderViewModel.onAction(
                BibleReaderViewModel.Action.SetFontDefinition(
                    FontDefinition
                        ("Monospace", FontFamily.Monospace),
                ),
            )
        }
        assertTrue(onBackClicked)
    }

    @Test
    fun `Back click triggers the onBackClick`() {
        var onBackClicked = false
        composeTestRule.setContent {
            FontsScreen(
                viewModel = mockBibleReaderViewModel,
                onBackClick = { onBackClicked = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(onBackClicked)
    }
}
