package com.youversion.platform.ui.views.card

import app.cash.turbine.test
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BibleCardViewModelTests {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bibleVersionRepository: BibleVersionRepository

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private val testBibleVersion = BibleVersion(id = 1, abbreviation = "KJV")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bibleVersionRepository = mockk(relaxed = true)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(bibleVersion: BibleVersion? = null): BibleCardViewModel =
        BibleCardViewModel(
            reference = defaultReference,
            bibleVersion = bibleVersion,
            bibleVersionRepository = bibleVersionRepository,
        )

    // region Initial State

    @Test
    fun `initial state has provided version and copyright hidden`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = testBibleVersion)

            assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
            assertFalse(viewModel.state.value.showCopyright)
        }

    @Test
    fun `initial state has null version when none provided`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = null)

            assertNull(viewModel.state.value.bibleVersion)
            assertFalse(viewModel.state.value.showCopyright)
        }

    // endregion

    // region Version Loading

    @Test
    fun `loads version from repository when initialized with null version`() =
        runTest {
            coEvery { bibleVersionRepository.version(id = 1) } returns testBibleVersion

            val viewModel = createViewModel(bibleVersion = null)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
        }

    @Test
    fun `does not load version from repository when initialized with version`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = testBibleVersion)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { bibleVersionRepository.version(id = any()) }
            assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
        }

    @Test
    fun `emits error event when version loading fails`() =
        runTest {
            coEvery { bibleVersionRepository.version(id = any()) } throws RuntimeException("Network error")

            val viewModel = createViewModel(bibleVersion = null)

            viewModel.events.test {
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(BibleCardViewModel.Event.OnErrorLoadingBibleVersion, awaitItem())
            }
            assertNull(viewModel.state.value.bibleVersion)
        }

    // endregion

    // region Actions

    @Test
    fun `OnViewCopyright sets showCopyright to true`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = testBibleVersion)

            viewModel.onAction(BibleCardViewModel.Action.OnViewCopyright)

            assertTrue(viewModel.state.value.showCopyright)
        }

    @Test
    fun `copyright toggle sequence shows copyright then returns to hidden`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = testBibleVersion)

            viewModel.onAction(BibleCardViewModel.Action.OnViewCopyright)
            assertTrue(viewModel.state.value.showCopyright)

            viewModel.onAction(BibleCardViewModel.Action.OnCloseCopyright)
            assertFalse(viewModel.state.value.showCopyright)
        }

    // endregion

    // region Edge Cases

    @Test
    fun `OnCloseCopyright is idempotent when already hidden`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = testBibleVersion)

            viewModel.onAction(BibleCardViewModel.Action.OnCloseCopyright)

            assertFalse(viewModel.state.value.showCopyright)
        }

    @Test
    fun `OnViewCopyright is idempotent when already showing`() =
        runTest {
            val viewModel = createViewModel(bibleVersion = testBibleVersion)

            viewModel.onAction(BibleCardViewModel.Action.OnViewCopyright)
            viewModel.onAction(BibleCardViewModel.Action.OnViewCopyright)

            assertTrue(viewModel.state.value.showCopyright)
        }

    @Test
    fun `copyright actions work independently of version loading`() =
        runTest {
            coEvery { bibleVersionRepository.version(id = 1) } returns testBibleVersion

            val viewModel = createViewModel(bibleVersion = null)

            viewModel.onAction(BibleCardViewModel.Action.OnViewCopyright)
            assertTrue(viewModel.state.value.showCopyright)

            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(viewModel.state.value.showCopyright)
            assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
        }

    // endregion

    // region Version Switching

    @Test
    fun `switchToVersion rebuilds reference and updates version on success`() =
        runTest {
            val newVersionId = 42
            val newVersion = BibleVersion(id = newVersionId, abbreviation = "NIV")
            coEvery { bibleVersionRepository.version(id = newVersionId) } returns newVersion

            val viewModel = createViewModel(bibleVersion = testBibleVersion)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.switchToVersion(newVersionId)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(newVersionId, viewModel.state.value.reference.versionId)
            assertEquals("GEN", viewModel.state.value.reference.bookUSFM)
            assertEquals(1, viewModel.state.value.reference.chapter)
            assertEquals(newVersion, viewModel.state.value.bibleVersion)
        }

    @Test
    fun `switchToVersion emits error event and keeps prior reference and version when fetch fails`() =
        runTest {
            val newVersionId = 42
            coEvery { bibleVersionRepository.version(id = newVersionId) } throws RuntimeException("Network error")

            val viewModel = createViewModel(bibleVersion = testBibleVersion)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.events.test {
                viewModel.switchToVersion(newVersionId)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(BibleCardViewModel.Event.OnErrorLoadingBibleVersion, awaitItem())
            }

            assertEquals(defaultReference.versionId, viewModel.state.value.reference.versionId)
            assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
        }

    // endregion
}
