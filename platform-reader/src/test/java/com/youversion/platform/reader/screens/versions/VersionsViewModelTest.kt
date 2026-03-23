package com.youversion.platform.reader.screens.versions

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.api.OrganizationsApi
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.reader.domain.BibleReaderRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VersionsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bibleReaderRepository: BibleReaderRepository
    private val mockOrganizationsApi = mockk<OrganizationsApi>()

    private val permittedEn = BibleVersion(id = 10, abbreviation = "KJV", languageTag = "en")
    private val activeEn = BibleVersion(id = 20, abbreviation = "NIV", languageTag = "en")
    private val spanishVersion = BibleVersion(id = 30, abbreviation = "RV", languageTag = "es")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bibleReaderRepository = mockk(relaxed = true)
    }

    /**
     * ViewModel work runs on [Dispatchers.Main] and is not a child of [runTest]'s scope. Draining the shared
     * [StandardTestDispatcher] avoids cancellation or failure propagation finishing after the test body and
     * tripping [kotlinx.coroutines.test.UncaughtExceptionsBeforeTest] on the following test.
     */
    @AfterTest
    fun teardown() {
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): VersionsViewModel = VersionsViewModel(bibleReaderRepository)

    @Test
    fun `loadVersions on success loads permitted and active language versions concurrently`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns listOf(activeEn)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(listOf(permittedEn), viewModel.state.value.permittedMinimalVersions)
            assertEquals(listOf(activeEn), viewModel.state.value.activeLanguageVersions)
            coVerify(exactly = 1) { bibleReaderRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleReaderRepository.fetchVersionsInLanguage("en") }
        }

    @Test
    fun `loadVersions on exception leaves initializing false and does not crash`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } coAnswers {
                throw RuntimeException("boom")
            }

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
            assertTrue(
                viewModel.state.value.permittedMinimalVersions
                    .isEmpty(),
            )
            assertTrue(
                viewModel.state.value.activeLanguageVersions
                    .isEmpty(),
            )
            coVerify(exactly = 1) { bibleReaderRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleReaderRepository.fetchVersionsInLanguage("en") }
        }

    @Test
    fun `loadVersions when permitted listing throws leaves initializing false`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } coAnswers {
                throw RuntimeException("listing failed")
            }
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns listOf(activeEn)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
            assertTrue(
                viewModel.state.value.permittedMinimalVersions
                    .isEmpty(),
            )
            assertTrue(
                viewModel.state.value.activeLanguageVersions
                    .isEmpty(),
            )
            // Both `async` children are scheduled before either `await` runs. With StandardTestDispatcher’s
            // FIFO order, the second child runs to completion before the first `await` rethrows, and
            // `supervisorScope` does not cancel siblings on child failure—so both repository calls occur.
            coVerify(exactly = 1) { bibleReaderRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleReaderRepository.fetchVersionsInLanguage("en") }
        }

    @Test
    fun `loadVersions when both concurrent calls fail leaves initializing false and calls both repositories`() =
        runTest(testDispatcher) {
            val unauthorized = RuntimeException("401 Unauthorized")
            val unavailable = RuntimeException("503 Service Unavailable")
            coEvery { bibleReaderRepository.permittedVersionsListing() } coAnswers { throw unauthorized }
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } coAnswers { throw unavailable }

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
            assertTrue(
                viewModel.state.value.permittedMinimalVersions
                    .isEmpty(),
            )
            assertTrue(
                viewModel.state.value.activeLanguageVersions
                    .isEmpty(),
            )
            coVerify(exactly = 1) { bibleReaderRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleReaderRepository.fetchVersionsInLanguage("en") }
        }

    @Test
    fun `combineConcurrentLoadFailures adds active exception as suppressed when both fail`() {
        val permitted = RuntimeException("401 Unauthorized")
        val active = RuntimeException("503 Service Unavailable")
        val thrown =
            assertFailsWith<RuntimeException> {
                combineConcurrentLoadFailures(
                    Result.failure(permitted),
                    Result.failure(active),
                )
            }
        assertEquals(permitted, thrown)
        assertEquals(1, thrown.suppressedExceptions.size)
        assertEquals(active, thrown.suppressedExceptions.single())
    }

    @Test
    fun `combineConcurrentLoadFailures throws permitted failure when only permitted fails`() {
        val permitted = RuntimeException("401 Unauthorized")
        val thrown =
            assertFailsWith<RuntimeException> {
                combineConcurrentLoadFailures(
                    Result.failure(permitted),
                    Result.success(emptyList()),
                )
            }
        assertEquals(permitted, thrown)
        assertTrue(thrown.suppressedExceptions.isEmpty())
    }

    @Test
    fun `combineConcurrentLoadFailures throws active failure when only active fails`() {
        val active = RuntimeException("503 Service Unavailable")
        val thrown =
            assertFailsWith<RuntimeException> {
                combineConcurrentLoadFailures(
                    Result.success(emptyList()),
                    Result.failure(active),
                )
            }
        assertEquals(active, thrown)
        assertTrue(thrown.suppressedExceptions.isEmpty())
    }

    @Test
    fun `combineConcurrentLoadFailures does not addSuppressed when both results share the same exception instance`() {
        val shared = RuntimeException("shared")
        val thrown =
            assertFailsWith<RuntimeException> {
                combineConcurrentLoadFailures(
                    Result.failure(shared),
                    Result.failure(shared),
                )
            }
        assertEquals(shared, thrown)
        assertTrue(thrown.suppressedExceptions.isEmpty())
    }

    @Test
    fun `loadVersions sets initializing to false on success`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `loadVersionsForLanguage on success updates active language versions and name`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns emptyList()
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("es") } returns listOf(spanishVersion)
            every { bibleReaderRepository.languageName("es") } returns "Español"

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadVersionsForLanguage("es")
            advanceUntilIdle()

            assertEquals("es", viewModel.state.value.activeLanguageTag)
            assertEquals(listOf(spanishVersion), viewModel.state.value.activeLanguageVersions)
            assertEquals("Español", viewModel.state.value.activeLanguageName)
            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `loadVersionsForLanguage on exception does not update language state and clears initializing`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns listOf(activeEn)
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("es") } coAnswers {
                throw RuntimeException("network")
            }

            val viewModel = createViewModel()
            advanceUntilIdle()

            val versionsBefore = viewModel.state.value.activeLanguageVersions
            val nameBefore = viewModel.state.value.activeLanguageName
            val tagBefore = viewModel.state.value.activeLanguageTag

            viewModel.loadVersionsForLanguage("es")
            advanceUntilIdle()

            assertEquals(tagBefore, viewModel.state.value.activeLanguageTag)
            assertEquals(versionsBefore, viewModel.state.value.activeLanguageVersions)
            assertEquals(nameBefore, viewModel.state.value.activeLanguageName)
            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `loadVersionsForLanguage when languageName throws after fetch keeps prior language state`() =
        runTest(testDispatcher) {
            coEvery { bibleReaderRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns listOf(activeEn)
            coEvery { bibleReaderRepository.fetchVersionsInLanguage("es") } returns listOf(spanishVersion)
            every { bibleReaderRepository.languageName("es") } throws RuntimeException("name lookup failed")

            val viewModel = createViewModel()
            advanceUntilIdle()

            val versionsBefore = viewModel.state.value.activeLanguageVersions
            val nameBefore = viewModel.state.value.activeLanguageName
            val tagBefore = viewModel.state.value.activeLanguageTag

            viewModel.loadVersionsForLanguage("es")
            advanceUntilIdle()

            assertEquals(tagBefore, viewModel.state.value.activeLanguageTag)
            assertEquals(versionsBefore, viewModel.state.value.activeLanguageVersions)
            assertEquals(nameBefore, viewModel.state.value.activeLanguageName)
            assertFalse(viewModel.state.value.initializing)
            coVerify(exactly = 1) { bibleReaderRepository.fetchVersionsInLanguage("es") }
            coVerify(exactly = 1) { bibleReaderRepository.languageName("es") }
        }

    @Test
    fun `VersionInfoTapped sets selected bible version and loads organization`() =
        runTest(testDispatcher) {
            mockkObject(YouVersionApi)
            try {
                every { YouVersionApi.organizations } returns mockOrganizationsApi
                coEvery { mockOrganizationsApi.organization("org-1") } returns Organization.preview

                coEvery { bibleReaderRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns emptyList()

                val version =
                    BibleVersion(
                        id = 99,
                        abbreviation = "T",
                        languageTag = "en",
                        organizationId = "org-1",
                    )
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(VersionsViewModel.Action.VersionInfoTapped(version))
                advanceUntilIdle()

                assertEquals(version, viewModel.state.value.selectedBibleVersion)
                assertEquals(Organization.preview, viewModel.state.value.selectedOrganization)
                coVerify(exactly = 1) { mockOrganizationsApi.organization("org-1") }
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `VersionInfoTapped with null organizationId does not fetch organization`() =
        runTest(testDispatcher) {
            mockkObject(YouVersionApi)
            try {
                every { YouVersionApi.organizations } returns mockOrganizationsApi

                coEvery { bibleReaderRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns emptyList()

                val version = BibleVersion(id = 100, abbreviation = "X", languageTag = "en", organizationId = null)
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(VersionsViewModel.Action.VersionInfoTapped(version))
                advanceUntilIdle()

                assertEquals(version, viewModel.state.value.selectedBibleVersion)
                assertNull(viewModel.state.value.selectedOrganization)
                coVerify(exactly = 0) { mockOrganizationsApi.organization(any()) }
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `VersionInfoTapped keeps selected version when organization fetch throws`() =
        runTest(testDispatcher) {
            mockkObject(YouVersionApi)
            try {
                every { YouVersionApi.organizations } returns mockOrganizationsApi
                coEvery { mockOrganizationsApi.organization(any()) } coAnswers {
                    throw RuntimeException("api error")
                }

                coEvery { bibleReaderRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns emptyList()

                val version =
                    BibleVersion(
                        id = 101,
                        abbreviation = "Y",
                        languageTag = "en",
                        organizationId = "org-2",
                    )
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(VersionsViewModel.Action.VersionInfoTapped(version))
                advanceUntilIdle()

                assertEquals(version, viewModel.state.value.selectedBibleVersion)
                assertNull(viewModel.state.value.selectedOrganization)
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `VersionDismissed clears selected bible version and organization`() =
        runTest(testDispatcher) {
            mockkObject(YouVersionApi)
            try {
                every { YouVersionApi.organizations } returns mockOrganizationsApi
                coEvery { mockOrganizationsApi.organization("org-1") } returns Organization.preview

                coEvery { bibleReaderRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleReaderRepository.fetchVersionsInLanguage("en") } returns emptyList()

                val version =
                    BibleVersion(
                        id = 102,
                        abbreviation = "Z",
                        languageTag = "en",
                        organizationId = "org-1",
                    )
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(VersionsViewModel.Action.VersionInfoTapped(version))
                advanceUntilIdle()
                assertEquals(Organization.preview, viewModel.state.value.selectedOrganization)

                viewModel.onAction(VersionsViewModel.Action.VersionDismissed)

                assertNull(viewModel.state.value.selectedBibleVersion)
                assertNull(viewModel.state.value.selectedOrganization)
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `State versionsCount returns permitted minimal versions count`() {
        val state =
            VersionsViewModel.State(
                permittedMinimalVersions = listOf(permittedEn, activeEn),
            )
        assertEquals(2, state.versionsCount)
    }

    @Test
    fun `State languagesCount returns distinct language tags count`() {
        val en2 = BibleVersion(id = 3, abbreviation = "A", languageTag = "en")
        val state =
            VersionsViewModel.State(
                permittedMinimalVersions = listOf(permittedEn, spanishVersion, en2),
            )
        assertEquals(2, state.languagesCount)
    }

    @Test
    fun `State showEmptyState is true when not initializing and no permitted versions`() {
        val empty =
            VersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = emptyList(),
            )
        assertTrue(empty.showEmptyState)
    }

    @Test
    fun `State showEmptyState is false when initializing`() {
        val loading =
            VersionsViewModel.State(
                initializing = true,
                permittedMinimalVersions = emptyList(),
            )
        assertFalse(loading.showEmptyState)
    }

    @Test
    fun `State activeLanguageVersionsCount counts versions matching active language tag`() {
        val state =
            VersionsViewModel.State(
                permittedMinimalVersions = listOf(permittedEn, spanishVersion),
                activeLanguageTag = "en",
            )
        assertEquals(1, state.activeLanguageVersionsCount)
    }
}
