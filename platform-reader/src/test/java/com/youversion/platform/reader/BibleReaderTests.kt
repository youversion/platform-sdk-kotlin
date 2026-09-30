package com.youversion.platform.reader

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleIntroRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleBookIntro
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.languages.domain.LanguageRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BibleReaderTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val genesis =
        BibleBook(
            id = "GEN",
            title = "Genesis",
            fullTitle = null,
            abbreviation = null,
            canon = null,
            chapters =
                (1..3).map {
                    BibleChapter(
                        id = "GEN.$it",
                        passageId = "GEN.$it",
                        title = "$it",
                        verses = listOf(BibleVerse(id = "1", passageId = "GEN.$it.1", title = "1")),
                    )
                },
            intro = BibleBookIntro(id = "GEN_INTRO", passageId = "GEN.INTRO1", title = "Introduction to Genesis"),
        )

    private val version =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            languageTag = "en",
            bookCodes = listOf("GEN"),
            books = listOf(genesis),
        )

    private val niv =
        BibleVersion(
            id = 2,
            abbreviation = "NIV",
            title = "New International Version",
            languageTag = "en",
            bookCodes = listOf("GEN"),
            books = listOf(genesis),
        )

    private val rvr =
        BibleVersion(
            id = 3,
            abbreviation = "RVR1960",
            title = "Reina-Valera 1960",
            languageTag = "es",
            bookCodes = listOf("GEN"),
            books = listOf(genesis),
        )

    private val versionRepository =
        mockk<BibleVersionRepository>(relaxed = true) {
            coEvery { version(1) } returns version
            coEvery { version(2) } returns niv
            coEvery { permittedVersionsListing() } returns listOf(version, niv, rvr)
            coEvery { fullVersions("en") } returns listOf(version, niv)
            coEvery { fullVersions("es") } returns listOf(rvr)
        }

    private val languageRepository =
        mockk<LanguageRepository>(relaxed = true) {
            coEvery { suggestedLanguageTags() } returns listOf("en", "es")
            every { languageName("en") } returns "English"
            every { languageName("es") } returns "Spanish"
        }

    @Before
    fun setUp() {
        PlatformKoinGraph.start(
            listOf(
                module {
                    single<Context> { ApplicationProvider.getApplicationContext() }
                    single<Storage> { mockk(relaxed = true) }
                    single { versionRepository }
                    single {
                        mockk<BibleChapterRepository>(relaxed = true) {
                            coEvery { chapter(any()) } returns
                                """<div><div class="p"><span class="yv-v" v="1"></span>In the beginning</div></div>"""
                        }
                    }
                    single { mockk<BibleIntroRepository>(relaxed = true) }
                    single { languageRepository }
                    single { BibleHighlightsRepository(api = mockk(relaxed = true)) }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
    }

    private fun setReaderContent() {
        composeTestRule.setContent {
            BibleReader(bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
        }
    }

    @Test
    fun `opens on the reader showing the passage and version`() {
        setReaderContent()

        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("KJV").assertIsDisplayed()
    }

    @Test
    fun `tapping the passage opens references and back returns to the reader`() {
        setReaderContent()

        composeTestRule.onNodeWithText("Genesis 1").performClick()
        composeTestRule.onNodeWithText("Books").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
    }

    @Test
    fun `choosing a chapter in references returns to the reader on that chapter`() {
        setReaderContent()

        composeTestRule.onNodeWithText("Genesis 1").performClick()
        composeTestRule.onNodeWithText("2").performClick()

        composeTestRule.onNodeWithText("Genesis 2").assertIsDisplayed()
    }

    @Test
    fun `choosing an intro in references returns to the reader on the intro`() {
        setReaderContent()

        composeTestRule.onNodeWithText("Genesis 1").performClick()
        composeTestRule.onNodeWithContentDescription("Intro").performClick()

        composeTestRule.onNodeWithText("Genesis Intro").assertIsDisplayed()
    }

    @Test
    fun `choosing a version returns to the reader on that version`() {
        setReaderContent()

        composeTestRule.onNodeWithText("KJV").performClick()
        composeTestRule.onNodeWithText("Bible Versions").assertIsDisplayed()

        composeTestRule.onNodeWithText("New International Version").performClick()

        composeTestRule.onNodeWithText("Bible Versions").assertDoesNotExist()
        composeTestRule.onNodeWithText("NIV").assertIsDisplayed()
    }

    @Test
    fun `back from versions returns to the reader`() {
        setReaderContent()

        composeTestRule.onNodeWithText("KJV").performClick()
        composeTestRule.onNodeWithText("Bible Versions").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        composeTestRule.onNodeWithText("Bible Versions").assertDoesNotExist()
        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
    }

    @Test
    fun `back from languages returns to versions`() {
        setReaderContent()

        composeTestRule.onNodeWithText("KJV").performClick()
        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule.onNodeWithText("Select a Language").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        composeTestRule.onNodeWithText("Select a Language").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bible Versions").assertIsDisplayed()
    }

    @Test
    fun `choosing a language returns to versions for that language`() {
        setReaderContent()

        composeTestRule.onNodeWithText("KJV").performClick()
        composeTestRule.onNodeWithText("Language").performClick()
        composeTestRule.onNodeWithText("Select a Language").assertIsDisplayed()

        composeTestRule.onNodeWithText("Spanish").performClick()

        composeTestRule.onNodeWithText("Select a Language").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bible Versions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spanish").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reina-Valera 1960").assertIsDisplayed()
        composeTestRule.onNodeWithText("New International Version").assertDoesNotExist()
    }

    @Test
    fun `choosing font in font settings opens fonts and back returns to the reader`() {
        mockkObject(YouVersionApi)
        coEvery { YouVersionApi.hasValidToken() } returns true
        try {
            setReaderContent()

            composeTestRule.onNodeWithContentDescription("Font & Settings").performClick()
            composeTestRule.onNodeWithText("Font & Settings").performClick()
            composeTestRule.onNodeWithTag("font_display_button").performClick()

            composeTestRule.onNodeWithText("Genesis 1").assertDoesNotExist()
            composeTestRule.onNodeWithText("Font").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
        } finally {
            unmockkObject(YouVersionApi)
        }
    }

    @Test
    fun `references appears once the version finishes loading`() {
        val gate = CompletableDeferred<Unit>()
        coEvery { versionRepository.version(1) } coAnswers {
            gate.await()
            version
        }
        setReaderContent()

        composeTestRule.onNodeWithContentDescription("Previous chapter").onParent().performClick()
        composeTestRule.onNodeWithText("Books").assertDoesNotExist()

        gate.complete(Unit)

        composeTestRule.onNodeWithText("Books").assertIsDisplayed()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated overload passes its sign-in copy to the configuration`() {
        mockkObject(YouVersionPlatformConfiguration)
        every { YouVersionPlatformConfiguration.configureSignIn(any(), any()) } just Runs
        try {
            composeTestRule.setContent {
                BibleReader(
                    appName = "Sample App",
                    appSignInMessage = "Keep your highlights",
                    bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
                )
            }

            composeTestRule.waitForIdle()
            verify {
                YouVersionPlatformConfiguration.configureSignIn(
                    appName = "Sample App",
                    signInPromptMessage = "Keep your highlights",
                )
            }
        } finally {
            unmockkObject(YouVersionPlatformConfiguration)
        }
    }

    @Test
    fun `draws the host app's bottom bar`() {
        composeTestRule.setContent {
            BibleReader(
                bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
                bottomBar = { Text("Host bar") },
            )
        }

        composeTestRule.onNodeWithText("Host bar").assertIsDisplayed()
    }
}
