package com.youversion.platform.ui.views.votd

import app.cash.turbine.test
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.votd.api.VotdApi
import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VerseOfTheDayViewModelTests {
    private val testDispatcher = StandardTestDispatcher()
    private val mockVotdApi = mockk<VotdApi>()
    private val mockBibleVersionRepository = mockk<BibleVersionRepository>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(YouVersionApi)
        every { YouVersionApi.votd } returns mockVotdApi
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        unmockkObject(YouVersionApi)
    }

    private fun createViewModel(versionId: Int = 1) =
        VerseOfTheDayViewModel(
            bibleVersionId = versionId,
            bibleVersionRepository = mockBibleVersionRepository,
        )

    private fun setupSuccessfulLoad(passageUsfm: String = YouVersionVerseOfTheDay.preview.passageUsfm) {
        coEvery { mockVotdApi.verseOfTheDay(any()) } returns YouVersionVerseOfTheDay(day = 1, passageUsfm = passageUsfm)
        coEvery { mockBibleVersionRepository.version(any()) } returns bibleVersionWithBooks(isaBook)
    }

    // ----- State default values

    @Test
    fun `State defaults to isLoading true with null reference, null version, and showIcon true`() {
        val state = VerseOfTheDayViewModel.State()
        assertTrue(state.isLoading)
        assertNull(state.bibleReference)
        assertNull(state.bibleVersion)
        assertTrue(state.showIcon)
    }

    // ----- Initial state

    @Test
    fun `initial state has isLoading true before load completes`() =
        runTest(testDispatcher) {
            setupSuccessfulLoad()
            val viewModel = createViewModel()
            assertTrue(viewModel.state.value.isLoading)
        }

    // ----- Successful load

    @Test
    fun `successful load updates state with bible reference and version`() =
        runTest(testDispatcher) {
            setupSuccessfulLoad()
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertNotNull(viewModel.state.value.bibleReference)
            assertNotNull(viewModel.state.value.bibleVersion)
        }

    @Test
    fun `successful load sets isLoading to false`() =
        runTest(testDispatcher) {
            setupSuccessfulLoad()
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `successful load emits no error event`() =
        runTest(testDispatcher) {
            setupSuccessfulLoad()
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                expectNoEvents()
            }
        }

    // ----- VOTD API failure

    @Test
    fun `VOTD API failure emits OnErrorLoadingVerseOfTheDay`() =
        runTest(testDispatcher) {
            coEvery { mockVotdApi.verseOfTheDay(any()) } throws RuntimeException("network error")
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                assertEquals(VerseOfTheDayViewModel.Event.OnErrorLoadingVerseOfTheDay, awaitItem())
            }
        }

    @Test
    fun `VOTD API failure sets isLoading to false`() =
        runTest(testDispatcher) {
            coEvery { mockVotdApi.verseOfTheDay(any()) } throws RuntimeException("network error")
            val viewModel = createViewModel()
            // Collect events so the rendezvous channel send in the error path can complete
            viewModel.events.test {
                advanceUntilIdle()
                awaitItem()
            }
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun `VOTD API failure does not populate reference or version`() =
        runTest(testDispatcher) {
            coEvery { mockVotdApi.verseOfTheDay(any()) } throws RuntimeException("network error")
            val viewModel = createViewModel()
            // Collect events so the rendezvous channel send in the error path can complete
            viewModel.events.test {
                advanceUntilIdle()
                awaitItem()
            }
            assertNull(viewModel.state.value.bibleReference)
            assertNull(viewModel.state.value.bibleVersion)
        }

    // ----- Version repository failure

    @Test
    fun `version repository failure emits OnErrorLoadingVerseOfTheDay and sets isLoading to false`() =
        runTest(testDispatcher) {
            coEvery { mockVotdApi.verseOfTheDay(any()) } returns YouVersionVerseOfTheDay.preview
            coEvery { mockBibleVersionRepository.version(any()) } throws RuntimeException("db error")
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                assertEquals(VerseOfTheDayViewModel.Event.OnErrorLoadingVerseOfTheDay, awaitItem())
            }
            assertFalse(viewModel.state.value.isLoading)
        }

    // ----- Null reference (USFM parsing failures)

    @Test
    fun `book absent from version books list results in error event and null reference`() =
        runTest(testDispatcher) {
            // Version only has ISA, but VOTD returns a JHN reference
            coEvery { mockVotdApi.verseOfTheDay(any()) } returns
                YouVersionVerseOfTheDay(day = 1, passageUsfm = "JHN.3.16")
            coEvery { mockBibleVersionRepository.version(any()) } returns bibleVersionWithBooks(isaBook)
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                assertEquals(VerseOfTheDayViewModel.Event.OnErrorLoadingVerseOfTheDay, awaitItem())
            }
            assertNull(viewModel.state.value.bibleReference)
        }

    // ----- Reference parsing edge cases

    @Test
    fun `passageUsfm shorter than 3 chars produces null reference and emits error event`() =
        runTest(testDispatcher) {
            coEvery { mockVotdApi.verseOfTheDay(any()) } returns YouVersionVerseOfTheDay(day = 1, passageUsfm = "AB")
            coEvery { mockBibleVersionRepository.version(any()) } returns bibleVersionWithBooks(isaBook)
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                assertEquals(VerseOfTheDayViewModel.Event.OnErrorLoadingVerseOfTheDay, awaitItem())
            }
        }

    @Test
    fun `passageUsfm with surrounding whitespace is trimmed and parsed successfully`() =
        runTest(testDispatcher) {
            setupSuccessfulLoad(passageUsfm = "  ISA.43.19  ")
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                expectNoEvents()
            }
            assertNotNull(viewModel.state.value.bibleReference)
        }

    @Test
    fun `BibleVersion with null books always produces null reference and emits error event`() =
        runTest(testDispatcher) {
            coEvery { mockVotdApi.verseOfTheDay(any()) } returns YouVersionVerseOfTheDay.preview
            // Default BibleVersion has books = null, so reference() will always return null
            coEvery { mockBibleVersionRepository.version(any()) } returns BibleVersion(id = 1)
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                assertEquals(VerseOfTheDayViewModel.Event.OnErrorLoadingVerseOfTheDay, awaitItem())
            }
        }

    @Test
    fun `compound passageUsfm with plus delimiter parses first valid reference successfully`() =
        runTest(testDispatcher) {
            setupSuccessfulLoad(passageUsfm = "ISA.43.19+ISA.43.20")
            val viewModel = createViewModel()
            viewModel.events.test {
                advanceUntilIdle()
                expectNoEvents()
            }
            assertNotNull(viewModel.state.value.bibleReference)
        }
}

private val isaBook =
    BibleBook(
        id = "ISA",
        title = "Isaiah",
        fullTitle = "Isaiah",
        abbreviation = "Isa",
        canon = "old_testament",
        chapters = null,
    )

private fun bibleVersionWithBooks(vararg books: BibleBook) = BibleVersion(id = 1, books = books.toList())
