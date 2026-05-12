package com.youversion.platform.ui.views.versions

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.views.components.LanguageRowItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class LanguagesScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val stateFlow = MutableStateFlow(BibleVersionsViewModel.State())

    private val mockViewModel =
        mockk<BibleVersionsViewModel>(relaxed = true) {
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

        composeTestRule.onNodeWithText("Select a Language").assertIsDisplayed()
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
            BibleVersionsViewModel.State(
                languagesInitializing = true,
            )

        renderScreen()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun `does not show CircularProgressIndicator when not initializing`() {
        stateFlow.value = BibleVersionsViewModel.State()

        renderScreen()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    // ----- Suggested Languages Tab

    @Test
    fun `displays suggested languages on first tab`() {
        stateFlow.value =
            BibleVersionsViewModel.State(
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
            BibleVersionsViewModel.State(
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
            BibleVersionsViewModel.State(
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
            BibleVersionsViewModel.State(
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
            BibleVersionsViewModel.State(
                languagesInitializing = true,
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
            BibleVersionsViewModel.State(
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
            BibleVersionsViewModel.State(
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

    // ----- Search

    @Test
    fun `displays search icon in top app bar`() {
        renderScreen()

        composeTestRule.onNodeWithContentDescription("Search for a language").assertIsDisplayed()
    }

    @Test
    fun `clicking search icon shows search bar and swaps icon to close`() {
        renderScreen()

        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close the search input").assertIsDisplayed()
    }

    @Test
    fun `clicking close icon hides search bar and clears non-empty search query`() {
        stateFlow.value = BibleVersionsViewModel.State(languageSearchQuery = "eng")

        renderScreen()
        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("eng").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Close the search input").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search for a language").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close the search input").assertDoesNotExist()
        verify { mockViewModel.onLanguageSearchQueryChange("") }
    }

    @Test
    fun `typing in search bar calls onLanguageSearchQueryChange`() {
        renderScreen()

        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("e")
        composeTestRule.waitForIdle()

        verify { mockViewModel.onLanguageSearchQueryChange("e") }
    }

    @Test
    fun `search bar reflects current languageSearchQuery from state`() {
        stateFlow.value = BibleVersionsViewModel.State(languageSearchQuery = "eng")

        renderScreen()
        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("eng").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
    }

    @Test
    fun `hides Suggested and All tabs when search is visible`() {
        renderScreen()
        composeTestRule.onNodeWithText("Suggested").assertIsDisplayed()
        composeTestRule.onNodeWithText("All").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Suggested").assertDoesNotExist()
        composeTestRule.onNodeWithText("All").assertDoesNotExist()
    }

    @Test
    fun `displays filtered all-languages list when search is visible`() {
        stateFlow.value =
            BibleVersionsViewModel.State(
                allLanguages =
                    listOf(
                        LanguageRowItem("en", "English", null),
                        LanguageRowItem("es", "Spanish", null),
                    ),
                languageSearchQuery = "eng",
            )

        renderScreen()
        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spanish").assertDoesNotExist()
    }

    @Test
    fun `clicking back arrow clears language search query and invokes onBackClick`() {
        stateFlow.value = BibleVersionsViewModel.State(languageSearchQuery = "eng")
        var backInvoked = false

        renderScreen(onBackClick = { backInvoked = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        verify { mockViewModel.onLanguageSearchQueryChange("") }
        assertEquals(true, backInvoked)
    }

    @Test
    fun `system back exits search mode and clears query when search is visible`() {
        renderScreen()
        composeTestRule.onNodeWithContentDescription("Search for a language").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close the search input").assertIsDisplayed()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Search for a language").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close the search input").assertDoesNotExist()
        verify { mockViewModel.onLanguageSearchQueryChange("") }
    }
}
