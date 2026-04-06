package com.youversion.platform.reader.screens.versions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VersionsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stateFlow = MutableStateFlow(VersionsViewModel.State())

    private val mockViewModel =
        mockk<VersionsViewModel>(relaxed = true) {
            every { state } returns stateFlow
        }

    private fun renderScreen(
        onBackClick: () -> Unit = {},
        onLanguagesClick: () -> Unit = {},
        onVersionSelect: (BibleVersion) -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                VersionsScreen(
                    viewModel = mockViewModel,
                    onBackClick = onBackClick,
                    onLanguagesClick = onLanguagesClick,
                    onVersionSelect = onVersionSelect,
                )
            }
        }
    }

    // ----- Top App Bar

    @Test
    fun `displays Versions title in top app bar`() {
        renderScreen()

        composeTestRule.onNodeWithText("Versions").assertIsDisplayed()
    }

    @Test
    fun `shows version count and language count subtitle when versionsCount is greater than zero`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions =
                    listOf(
                        BibleVersion(id = 1, languageTag = "en"),
                        BibleVersion(id = 2, languageTag = "es"),
                        BibleVersion(id = 3, languageTag = "en"),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("3 Versions in 2 Languages").assertIsDisplayed()
    }

    @Test
    fun `does not show subtitle when versionsCount is zero`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = emptyList(),
            )

        renderScreen()

        composeTestRule.onNodeWithText("Versions in", substring = true).assertDoesNotExist()
    }

    // ----- LanguageSelector

    @Test
    fun `displays Language label and active language name in language selector`() {
        stateFlow.value =
            VersionsViewModel.State(
                activeLanguageName = "Español",
            )

        renderScreen()

        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("Español").assertIsDisplayed()
    }

    @Test
    fun `language selector is disabled during initialization`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = true,
            )
        var languagesClicked = false

        renderScreen(onLanguagesClick = { languagesClicked = true })

        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule.waitForIdle()
        assertFalse(languagesClicked)
    }

    @Test
    fun `language selector calls onLanguagesClick when enabled`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
            )
        var languagesClicked = false

        renderScreen(onLanguagesClick = { languagesClicked = true })

        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule.waitForIdle()
        assertTrue(languagesClicked)
    }

    // ----- Section Header

    @Test
    fun `shows section header with active language name and version count`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                activeLanguageName = "English",
                activeLanguageTag = "en",
                permittedMinimalVersions =
                    listOf(
                        BibleVersion(id = 1, languageTag = "en"),
                        BibleVersion(id = 2, languageTag = "en"),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("English Versions (2)").assertIsDisplayed()
    }

    // ----- Loading State

    @Test
    fun `does not show empty state when initializing`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = true,
            )

        renderScreen()

        composeTestRule.onNodeWithText("No versions found for this language").assertDoesNotExist()
    }

    // ----- Empty State

    @Test
    fun `shows empty state message when showEmptyState is true`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = emptyList(),
            )

        renderScreen()

        composeTestRule.onNodeWithText("No versions found for this language").assertIsDisplayed()
    }

    // ----- Version List

    @Test
    fun `renders version rows when versions are available`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 1,
                            localizedAbbreviation = "KJV",
                            localizedTitle = "King James Version",
                            languageTag = "en",
                        ),
                        BibleVersion(
                            id = 2,
                            localizedAbbreviation = "NIV",
                            localizedTitle = "New International Version",
                            languageTag = "en",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("KJV").assertIsDisplayed()
        composeTestRule.onNodeWithText("King James Version").assertIsDisplayed()
        composeTestRule.onNodeWithText("NIV").assertIsDisplayed()
        composeTestRule.onNodeWithText("New International Version").assertIsDisplayed()
    }

    @Test
    fun `clicking version row calls onVersionSelect with correct version`() {
        val kjv =
            BibleVersion(
                id = 1,
                localizedAbbreviation = "KJV",
                localizedTitle = "King James Version",
                languageTag = "en",
            )
        val niv =
            BibleVersion(
                id = 2,
                localizedAbbreviation = "NIV",
                localizedTitle = "New International Version",
                languageTag = "en",
            )
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions =
                    listOf(
                        BibleVersion(id = 1, languageTag = "en"),
                        BibleVersion(id = 2, languageTag = "en"),
                    ),
                activeLanguageVersions = listOf(kjv, niv),
            )
        var selectedVersion: BibleVersion? = null

        renderScreen(onVersionSelect = { selectedVersion = it })

        composeTestRule.onNodeWithText("King James Version").performClick()
        composeTestRule.waitForIdle()
        assertEquals(kjv, selectedVersion)

        composeTestRule.onNodeWithText("New International Version").performClick()
        composeTestRule.waitForIdle()
        assertEquals(niv, selectedVersion)
    }

    @Test
    fun `clicking info icon dispatches VersionInfoTapped action`() {
        val version =
            BibleVersion(
                id = 1,
                localizedAbbreviation = "KJV",
                localizedTitle = "King James Version",
                languageTag = "en",
            )
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                activeLanguageVersions = listOf(version),
            )

        renderScreen()

        composeTestRule.onNodeWithContentDescription("Bible Version Details").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockViewModel.onAction(VersionsViewModel.Action.VersionInfoTapped(version))
        }
    }

    // ----- BibleVersionRow Display

    @Test
    fun `displays localizedAbbreviation when available`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 1,
                            localizedAbbreviation = "RVR",
                            abbreviation = "RV1960",
                            languageTag = "es",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("RVR").assertIsDisplayed()
    }

    @Test
    fun `displays abbreviation when localizedAbbreviation is null`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 1,
                            localizedAbbreviation = null,
                            abbreviation = "RV1960",
                            languageTag = "es",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("RV1960").assertIsDisplayed()
    }

    @Test
    fun `displays id as abbreviation when both localizedAbbreviation and abbreviation are null`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 42, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 42,
                            localizedAbbreviation = null,
                            abbreviation = null,
                            languageTag = "en",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("42").assertIsDisplayed()
    }

    @Test
    fun `displays localizedTitle when available`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 1,
                            localizedTitle = "Reina Valera",
                            title = "Reina-Valera 1960",
                            languageTag = "es",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("Reina Valera").assertIsDisplayed()
    }

    @Test
    fun `displays title when localizedTitle is null`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 1,
                            localizedTitle = null,
                            title = "Reina-Valera 1960",
                            languageTag = "es",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("Reina-Valera 1960").assertIsDisplayed()
    }

    @Test
    fun `displays id as title when both localizedTitle and title are null`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 77, languageTag = "en")),
                activeLanguageVersions =
                    listOf(
                        BibleVersion(
                            id = 77,
                            localizedTitle = null,
                            title = null,
                            localizedAbbreviation = "TEST",
                            languageTag = "en",
                        ),
                    ),
            )

        renderScreen()

        composeTestRule.onNodeWithText("77").assertIsDisplayed()
    }

    // ----- Bottom Sheet

    @Test
    fun `does not show VersionInfoBottomSheet when selectedBibleVersion is null`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                selectedBibleVersion = null,
            )

        renderScreen()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Agree and Download").assertDoesNotExist()
    }

    @Test
    fun `shows VersionInfoBottomSheet when selectedBibleVersion is non-null`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                selectedBibleVersion = BibleVersion.preview,
            )

        renderScreen()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Agree and Download").assertIsDisplayed()
    }

    @Test
    fun `dismissing bottom sheet via Maybe Later dispatches VersionDismissed action`() {
        stateFlow.value =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = listOf(BibleVersion(id = 1, languageTag = "en")),
                selectedBibleVersion = BibleVersion.preview,
            )

        renderScreen()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Maybe Later").performClick()
        composeTestRule.waitForIdle()

        verify { mockViewModel.onAction(VersionsViewModel.Action.VersionDismissed) }
    }
}
