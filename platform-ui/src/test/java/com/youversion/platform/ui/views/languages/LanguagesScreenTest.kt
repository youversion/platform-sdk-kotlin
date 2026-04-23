package com.youversion.platform.ui.views.languages

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.views.components.LanguageRowItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LanguagesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stateFlow = MutableStateFlow(LanguagesViewModel.State())

    private val mockViewModel =
        mockk<LanguagesViewModel>(relaxed = true) {
            every { state } returns stateFlow
        }

    private fun renderScreen(
        onBackClick: () -> Unit = {},
        onLanguageTagSelected: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                LanguagesScreen(
                    viewModel = mockViewModel,
                    onBackClick = onBackClick,
                    onLanguageTagSelected = onLanguageTagSelected,
                )
            }
        }
    }

    // ----- Top App Bar

    @Test
    fun `displays Select a Languages title in top app bar`() {
        renderScreen()

        composeTestRule.onNodeWithText("Select a Languages").assertIsDisplayed()
    }

    // ----- Tabs

    @Test
    fun `displays Suggested and All tabs`() {
        renderScreen()

        composeTestRule.onNodeWithText("Suggested").assertIsDisplayed()
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
    }

    // ----- Loading State

    @Test
    fun `shows CircularProgressIndicator when initializing`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = true,
            )

        renderScreen()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun `does not show CircularProgressIndicator when not initializing`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
            )

        renderScreen()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    // ----- Suggested Languages Tab

    @Test
    fun `displays suggested languages on first tab`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
                suggestedLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "en",
                            displayName = "English",
                            localeDisplayName = null,
                        ),
                        LanguageRowItem(
                            languageTag = "es",
                            displayName = "Español",
                            localeDisplayName = "Spanish",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Español").assertIsDisplayed()
    }

    @Test
    fun `displays localeDisplayName when non-null`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
                suggestedLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "es",
                            displayName = "Español",
                            localeDisplayName = "Spanish",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("Español").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spanish").assertIsDisplayed()
    }

    @Test
    fun `does not display localeDisplayName when null`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
                suggestedLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "en",
                            displayName = "English",
                            localeDisplayName = null,
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun `clicking a language row calls onLanguageTagSelected with correct tag`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
                suggestedLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "en",
                            displayName = "English",
                            localeDisplayName = null,
                        ),
                        LanguageRowItem(
                            languageTag = "es",
                            displayName = "Español",
                            localeDisplayName = null,
                        ),
                    ),
            )
        val selectedTags = mutableListOf<String>()

        renderScreen(onLanguageTagSelected = { selectedTags.add(it) })

        composeTestRule.onNodeWithText("English").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("en"), selectedTags)

        composeTestRule.onNodeWithText("Español").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("en", "es"), selectedTags)
    }

    @Test
    fun `shows CircularProgressIndicator on All tab when initializing`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = true,
            )

        renderScreen()

        composeTestRule.onNodeWithText("All").performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    // ----- All Languages Tab

    @Test
    fun `displays all languages after switching to All tab`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
                suggestedLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "en",
                            displayName = "English",
                            localeDisplayName = null,
                        ),
                    ),
                allLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "fr",
                            displayName = "Français",
                            localeDisplayName = "French",
                        ),
                        LanguageRowItem(
                            languageTag = "de",
                            displayName = "Deutsch",
                            localeDisplayName = "German",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("All").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Français").assertIsDisplayed()
        composeTestRule.onNodeWithText("French").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deutsch").assertIsDisplayed()
        composeTestRule.onNodeWithText("German").assertIsDisplayed()
    }

    @Test
    fun `clicking a language on All tab calls onLanguageTagSelected with correct tag`() {
        stateFlow.value =
            LanguagesViewModel.State(
                initializing = false,
                allLanguages =
                    listOf(
                        LanguageRowItem(
                            languageTag = "fr",
                            displayName = "Français",
                            localeDisplayName = null,
                        ),
                    ),
            )
        var selectedTag: String? = null

        renderScreen(onLanguageTagSelected = { selectedTag = it })

        composeTestRule.onNodeWithText("All").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Français").performClick()
        composeTestRule.waitForIdle()

        assertEquals("fr", selectedTag)
    }
}
