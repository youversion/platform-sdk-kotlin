package com.youversion.platform.ui.views.versions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BibleVersionPickingButtonTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVersionRepository = mockk<BibleVersionRepository>(relaxed = true)
    private val mockLanguageRepository = mockk<LanguageRepository>(relaxed = true)

    private val testBibleVersion =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            localizedAbbreviation = "KJV",
        )

    @Before
    fun setUp() {
        coEvery { mockVersionRepository.version(any()) } returns testBibleVersion

        PlatformKoinGraph.start(
            listOf(
                module {
                    single { mockVersionRepository }
                    single { mockLanguageRepository }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
    }

    @Test
    fun `displays abbreviation after async version load`() {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleVersionPickingButton(initialVersionId = 1)
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "Abbreviation appears after async version load",
        ) {
            composeTestRule.onAllNodesWithText("KJV").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("KJV").assertIsDisplayed()
    }

    @Test
    fun `invokes onVersionChange callback when version loads`() {
        var receivedVersion: BibleVersion? = null

        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleVersionPickingButton(
                    initialVersionId = 1,
                    onVersionChange = { receivedVersion = it },
                )
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "onVersionChange fires after async load",
        ) {
            receivedVersion != null
        }

        assertEquals(testBibleVersion, receivedVersion)
    }

    @Test
    fun `tapping button opens the version picker sheet`() {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleVersionPickingButton(initialVersionId = 1)
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "Button renders the abbreviation",
        ) {
            composeTestRule.onAllNodesWithText("KJV").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("KJV").performClick()

        composeTestRule.onNodeWithText("Versions").assertIsDisplayed()
    }

    @Test
    fun `selecting a version in the sheet fires onVersionChange with the selected version`() {
        val newVersion =
            BibleVersion(
                id = 2,
                abbreviation = "NIV",
                localizedAbbreviation = "NIV",
                title = "New International Version",
                languageTag = "en",
            )
        coEvery { mockVersionRepository.fullVersions("en") } returns listOf(newVersion)
        coEvery { mockVersionRepository.permittedVersionsListing() } returns listOf(newVersion)

        val receivedVersions = mutableListOf<BibleVersion>()

        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleVersionPickingButton(
                    initialVersionId = 1,
                    onVersionChange = { receivedVersions.add(it) },
                )
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "Initial version load fires onVersionChange",
        ) {
            receivedVersions.isNotEmpty()
        }

        composeTestRule.onNodeWithText("KJV").performClick()

        composeTestRule.waitUntil(
            conditionDescription = "Version row visible inside sheet",
        ) {
            composeTestRule.onAllNodesWithText("New International Version").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("New International Version").performClick()

        composeTestRule.waitUntil(
            conditionDescription = "Selected version delivered to onVersionChange",
        ) {
            receivedVersions.lastOrNull() == newVersion
        }
    }

    @Test
    fun `tapping Languages in the sheet navigates to the languages screen`() {
        val testVersion =
            BibleVersion(
                id = 2,
                abbreviation = "NIV",
                localizedAbbreviation = "NIV",
                title = "New International Version",
                languageTag = "en",
            )
        coEvery { mockVersionRepository.fullVersions("en") } returns listOf(testVersion)
        coEvery { mockVersionRepository.permittedVersionsListing() } returns listOf(testVersion)

        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleVersionPickingButton(initialVersionId = 1)
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "Initial version loaded",
        ) {
            composeTestRule.onAllNodesWithText("KJV").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("KJV").performClick()

        composeTestRule.waitUntil(
            conditionDescription = "Versions screen finished loading",
        ) {
            composeTestRule.onAllNodesWithText("New International Version").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Language").performClick()

        composeTestRule.onNodeWithText("Suggested").assertIsDisplayed()
    }

    @Test
    fun `selecting a language returns to versions screen with versions for that language`() {
        val englishVersion =
            BibleVersion(
                id = 2,
                abbreviation = "NIV",
                localizedAbbreviation = "NIV",
                title = "New International Version",
                languageTag = "en",
            )
        val spanishVersion =
            BibleVersion(
                id = 3,
                abbreviation = "RV",
                localizedAbbreviation = "RV",
                title = "Reina-Valera",
                languageTag = "es",
            )

        coEvery { mockVersionRepository.fullVersions("en") } returns listOf(englishVersion)
        coEvery { mockVersionRepository.fullVersions("es") } returns listOf(spanishVersion)
        coEvery { mockVersionRepository.permittedVersionsListing() } returns listOf(englishVersion, spanishVersion)

        every { mockLanguageRepository.allPermittedLanguageTags } returns listOf("en", "es")
        coEvery { mockLanguageRepository.suggestedLanguageTags() } returns listOf("es")
        every { mockLanguageRepository.languageName("es") } returns "Spanish"
        every { mockLanguageRepository.languageName("en") } returns "English"

        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleVersionPickingButton(initialVersionId = 1)
            }
        }

        composeTestRule.waitUntil(conditionDescription = "Initial version loaded") {
            composeTestRule.onAllNodesWithText("KJV").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("KJV").performClick()

        composeTestRule.waitUntil(conditionDescription = "Versions screen loaded") {
            composeTestRule.onAllNodesWithText("New International Version").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Language").performClick()

        composeTestRule.waitUntil(conditionDescription = "Languages screen rendered with rows") {
            composeTestRule.onAllNodesWithText("Spanish").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Spanish").performClick()

        composeTestRule.waitUntil(conditionDescription = "Returned to versions screen with Spanish versions") {
            composeTestRule.onAllNodesWithText("Reina-Valera").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
