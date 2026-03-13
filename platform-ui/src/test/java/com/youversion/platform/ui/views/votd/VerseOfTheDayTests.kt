package com.youversion.platform.ui.views.votd

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.votd.api.VotdApi
import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VerseOfTheDayTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVotdApi = mockk<VotdApi>()
    private val testBibleVersion =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            localizedAbbreviation = "KJV",
            books =
                listOf(
                    BibleBook(
                        id = "GEN",
                        title = "Genesis",
                        fullTitle = null,
                        abbreviation = "Gen",
                        canon = "old_testament",
                        chapters = null,
                    ),
                ),
        )

    @Before
    fun setUp() {
        mockkObject(YouVersionApi)
        every { YouVersionApi.votd } returns mockVotdApi
        coEvery { mockVotdApi.verseOfTheDay(any()) } returns YouVersionVerseOfTheDay(day = 1, passageUsfm = "GEN.3.16")

        PlatformKoinGraph.start(
            listOf(
                module {
                    single {
                        mockk<BibleVersionRepository> {
                            coEvery { version(any()) } returns testBibleVersion
                        }
                    }
                    single {
                        mockk<BibleChapterRepository> {
                            coEvery { chapter(any()) } returns "<div data-usfm=\"GEN.3\"></div>"
                            coEvery { removeVersionChapters(any()) } returns Unit
                        }
                    }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
        unmockkObject(YouVersionApi)
    }

    // ----- VerseOfTheDay (public)

    @Test
    fun `VerseOfTheDay shows no content when VOTD fails to load`() {
        coEvery { mockVotdApi.verseOfTheDay(any()) } throws Exception("VOTD failed")
        composeTestRule.setContent { VerseOfTheDay(bibleVersionId = 1) }
        composeTestRule.onNodeWithText("VERSE OF THE DAY", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `VerseOfTheDay shows icon when showIcon is true`() {
        composeTestRule.setContent { VerseOfTheDay(bibleVersionId = 1, showIcon = true) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Verse of the Day").assertIsDisplayed()
    }

    @Test
    fun `VerseOfTheDay hides icon when showIcon is false`() {
        composeTestRule.setContent { VerseOfTheDay(bibleVersionId = 1, showIcon = false) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Verse of the Day").assertDoesNotExist()
    }

    @Test
    fun `VerseOfTheDay invokes onShareClick when share button is clicked`() {
        var shareClicked = false
        composeTestRule.setContent {
            VerseOfTheDay(
                bibleVersionId = 1,
                onShareClick = { shareClicked = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Share").performClick()
        assertTrue(shareClicked)
    }

    @Test
    fun `VerseOfTheDay invokes onFullChapterClick when full chapter button is clicked`() {
        var fullChapterClicked = false
        composeTestRule.setContent {
            VerseOfTheDay(
                bibleVersionId = 1,
                onFullChapterClick = { fullChapterClicked = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Full Chapter").performClick()
        assertTrue(fullChapterClicked)
    }

    @Test
    fun `VerseOfTheDay renders in dark mode`() {
        composeTestRule.setContent { VerseOfTheDay(bibleVersionId = 1, dark = true) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("VERSE OF THE DAY", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `VerseOfTheDay renders in light mode`() {
        composeTestRule.setContent { VerseOfTheDay(bibleVersionId = 1, dark = false) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("VERSE OF THE DAY", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `VerseOfTheDay displays bible version title`() {
        composeTestRule.setContent { VerseOfTheDay(bibleVersionId = 1) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Genesis 3:16 KJV").assertIsDisplayed()
    }

    // ----- CompactVerseOfTheDay (public)

    @Test
    fun `CompactVerseOfTheDay shows no content when VOTD fails to load`() {
        coEvery { mockVotdApi.verseOfTheDay(any()) } throws Exception("VOTD failed")
        composeTestRule.setContent { CompactVerseOfTheDay(bibleVersionId = 1) }
        composeTestRule.onNodeWithText("VERSE OF THE DAY", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `CompactVerseOfTheDay shows icon when showIcon is true`() {
        composeTestRule.setContent { CompactVerseOfTheDay(bibleVersionId = 1, showIcon = true) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Verse of the Day").assertIsDisplayed()
    }

    @Test
    fun `CompactVerseOfTheDay hides icon when showIcon is false`() {
        composeTestRule.setContent { CompactVerseOfTheDay(bibleVersionId = 1, showIcon = false) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Verse of the Day").assertDoesNotExist()
    }

    @Test
    fun `CompactVerseOfTheDay renders in dark mode`() {
        composeTestRule.setContent { CompactVerseOfTheDay(bibleVersionId = 1, dark = true) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("VERSE OF THE DAY", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `CompactVerseOfTheDay renders in light mode`() {
        composeTestRule.setContent { CompactVerseOfTheDay(bibleVersionId = 1, dark = false) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("VERSE OF THE DAY", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `CompactVerseOfTheDay displays bible version title`() {
        composeTestRule.setContent { CompactVerseOfTheDay(bibleVersionId = 1) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Genesis 3:16 KJV").assertIsDisplayed()
    }
}
