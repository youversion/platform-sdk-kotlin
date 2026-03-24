package com.youversion.platform.reader.screens.languages

import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LanguagesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bibleReaderRepository: BibleReaderRepository
    private lateinit var viewModel: LanguagesViewModel

    private val testBibleVersion = BibleVersion(id = 1, abbreviation = "KJV", languageTag = "en")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bibleReaderRepository = mockk(relaxed = true)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(bibleVersion: BibleVersion?): LanguagesViewModel =
        LanguagesViewModel(
            bibleVersion = bibleVersion,
            bibleReaderRepository = bibleReaderRepository,
        )

    private fun stubSuccessfulLoad() {
        coEvery { bibleReaderRepository.loadLanguageNames(any()) } returns Unit
        every { bibleReaderRepository.allPermittedLanguageTags } returns listOf("en", "es")
        every { bibleReaderRepository.languageName("en") } returns "English"
        every { bibleReaderRepository.languageName("es") } returns "Spanish"
        coEvery { bibleReaderRepository.suggestedLanguageTags() } returns listOf("en")
    }

    // ----- State Initial Values

    @Test
    fun `state has initializing true and empty lists before load completes`() =
        runTest(testDispatcher) {
            val loadDeferred = CompletableDeferred<Unit>()
            coEvery { bibleReaderRepository.loadLanguageNames(any()) } coAnswers {
                loadDeferred.await()
            }
            viewModel = createViewModel(bibleVersion = null)

            assertTrue(viewModel.state.value.initializing)
            assertTrue(
                viewModel.state.value.suggestedLanguages
                    .isEmpty(),
            )
            assertTrue(
                viewModel.state.value.allLanguages
                    .isEmpty(),
            )
            loadDeferred.completeExceptionally(RuntimeException("test cancel"))
            advanceUntilIdle()
        }

    // ----- Constructor Variants

    @Test
    fun `init calls loadLanguageNames with null when bibleVersion is null`() =
        runTest(testDispatcher) {
            stubSuccessfulLoad()
            coEvery { bibleReaderRepository.loadLanguageNames(null) } returns Unit

            viewModel = createViewModel(bibleVersion = null)
            advanceUntilIdle()

            coVerify(exactly = 1) { bibleReaderRepository.loadLanguageNames(null) }
        }

    @Test
    fun `init calls loadLanguageNames with provided bibleVersion when non-null`() =
        runTest(testDispatcher) {
            stubSuccessfulLoad()
            coEvery { bibleReaderRepository.loadLanguageNames(testBibleVersion) } returns Unit

            viewModel = createViewModel(bibleVersion = testBibleVersion)
            advanceUntilIdle()

            coVerify(exactly = 1) { bibleReaderRepository.loadLanguageNames(testBibleVersion) }
        }

    // ----- Success Path

    @Test
    fun `loadLanguages maps allPermittedLanguageTags to LanguageRowItem and updates state`() =
        runTest(testDispatcher) {
            stubSuccessfulLoad()
            viewModel = createViewModel(bibleVersion = null)
            advanceUntilIdle()

            assertEquals(2, viewModel.state.value.allLanguages.size)
            assertEquals(
                LanguageRowItem("en", "English", null),
                viewModel.state.value.allLanguages[0],
            )
            assertEquals(
                LanguageRowItem("es", "Spanish", null),
                viewModel.state.value.allLanguages[1],
            )
            assertEquals(1, viewModel.state.value.suggestedLanguages.size)
            assertEquals(
                LanguageRowItem("en", "English", null),
                viewModel.state.value.suggestedLanguages[0],
            )
            assertFalse(viewModel.state.value.initializing)
        }

    // ----- Exception Path

    @Test
    fun `on loadLanguageNames exception does not update suggestedLanguages or allLanguages`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.loadLanguageNames(any()) } throws RuntimeException("test")

            viewModel = createViewModel(bibleVersion = null)
            advanceUntilIdle()

            assertTrue(
                viewModel.state.value.suggestedLanguages
                    .isEmpty(),
            )
            assertTrue(
                viewModel.state.value.allLanguages
                    .isEmpty(),
            )
        }

    @Test
    fun `on exception initializing is set to false in finally block`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.loadLanguageNames(any()) } throws RuntimeException("test")

            viewModel = createViewModel(bibleVersion = null)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `initializing is set to false in finally block after success`() =
        runTest(testDispatcher) {
            stubSuccessfulLoad()
            viewModel = createViewModel(bibleVersion = null)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
        }

    // ----- Call Order

    @Test
    fun `loadLanguageNames is called before suggestedLanguageTags`() =
        runTest(testDispatcher) {
            val callOrder = mutableListOf<String>()
            coEvery { bibleReaderRepository.loadLanguageNames(any()) } answers {
                callOrder.add("loadLanguageNames")
            }
            every { bibleReaderRepository.allPermittedLanguageTags } returns listOf("en")
            every { bibleReaderRepository.languageName("en") } returns "English"
            coEvery { bibleReaderRepository.suggestedLanguageTags() } answers {
                callOrder.add("suggestedLanguageTags")
                listOf("en")
            }

            viewModel = createViewModel(bibleVersion = null)
            advanceUntilIdle()

            assertEquals(listOf("loadLanguageNames", "suggestedLanguageTags"), callOrder)
        }
}
