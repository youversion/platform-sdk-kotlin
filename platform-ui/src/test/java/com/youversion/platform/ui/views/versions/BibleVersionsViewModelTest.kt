package com.youversion.platform.ui.views.versions

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.organizations.api.OrganizationsApi
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.ui.views.components.LanguageRowItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
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
class BibleVersionsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bibleVersionRepository: BibleVersionRepository
    private lateinit var languageRepository: LanguageRepository
    private lateinit var mockOrganizationsApi: OrganizationsApi

    private val permittedEn = BibleVersion(id = 10, abbreviation = "KJV", languageTag = "en")
    private val activeEn = BibleVersion(id = 20, abbreviation = "NIV", languageTag = "en")
    private val spanishVersion = BibleVersion(id = 30, abbreviation = "RV", languageTag = "es")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bibleVersionRepository = mockk(relaxed = true)
        languageRepository = mockk(relaxed = true)
        mockOrganizationsApi = mockk()
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

    private fun createViewModel(
        initialVersionId: Int? = null,
        onVersionChange: (BibleVersion) -> Unit = {},
    ): BibleVersionsViewModel =
        BibleVersionsViewModel(
            initialVersionId = initialVersionId,
            onVersionChange = onVersionChange,
            languageRepository = languageRepository,
            bibleVersionRepository = bibleVersionRepository,
        )

    @Test
    fun `loadVersion fires onVersionChange with loaded version when initialVersionId is provided`() =
        runTest(testDispatcher) {
            val loaded = BibleVersion(id = 42, abbreviation = "NIV", languageTag = "en")
            coEvery { bibleVersionRepository.version(id = 42) } returns loaded
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            var received: BibleVersion? = null
            createViewModel(initialVersionId = 42, onVersionChange = { received = it })
            advanceUntilIdle()

            assertEquals(loaded, received)
            coVerify(exactly = 1) { bibleVersionRepository.version(id = 42) }
        }

    @Test
    fun `loadVersion does not fire onVersionChange when initialVersionId is null`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            val onVersionChange = mockk<(BibleVersion) -> Unit>(relaxed = true)
            createViewModel(initialVersionId = null, onVersionChange = onVersionChange)
            advanceUntilIdle()

            verify(exactly = 0) { onVersionChange(any()) }
            coVerify(exactly = 0) { bibleVersionRepository.version(any()) }
        }

    @Test
    fun `loadVersion does not fire onVersionChange when repository throws`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.version(id = 42) } coAnswers { throw RuntimeException("boom") }
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            val onVersionChange = mockk<(BibleVersion) -> Unit>(relaxed = true)
            createViewModel(initialVersionId = 42, onVersionChange = onVersionChange)
            advanceUntilIdle()

            verify(exactly = 0) { onVersionChange(any()) }
        }

    @Test
    fun `selectFallbackVersion picks first downloaded version when downloads are not empty`() =
        runTest(testDispatcher) {
            val downloaded = BibleVersion(id = 77, abbreviation = "D", languageTag = "en")
            every { bibleVersionRepository.downloadedVersions } returns listOf(77)
            coEvery { bibleVersionRepository.version(id = 77) } returns downloaded

            var received: BibleVersion? = null
            createViewModel(initialVersionId = null, onVersionChange = { received = it })
            advanceUntilIdle()

            assertEquals(downloaded, received)
        }

    @Test
    fun `selectFallbackVersion picks first English permitted version when downloads are empty`() =
        runTest(testDispatcher) {
            val english = BibleVersion(id = 11, abbreviation = "NIV", languageTag = "en")
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(spanishVersion, english)
            coEvery { bibleVersionRepository.version(id = 11) } returns english

            var received: BibleVersion? = null
            createViewModel(initialVersionId = null, onVersionChange = { received = it })
            advanceUntilIdle()

            assertEquals(english, received)
        }

    @Test
    fun `selectFallbackVersion picks first permitted version when no English is available`() =
        runTest(testDispatcher) {
            val french = BibleVersion(id = 55, abbreviation = "LSG", languageTag = "fr")
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(french, spanishVersion)
            coEvery { bibleVersionRepository.version(id = 55) } returns french

            var received: BibleVersion? = null
            createViewModel(initialVersionId = null, onVersionChange = { received = it })
            advanceUntilIdle()

            assertEquals(french, received)
        }

    @Test
    fun `selectFallbackVersion does not fire onVersionChange when permittedVersionsListing throws`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } coAnswers
                { throw RuntimeException("offline") }

            val onVersionChange = mockk<(BibleVersion) -> Unit>(relaxed = true)
            createViewModel(initialVersionId = null, onVersionChange = onVersionChange)
            advanceUntilIdle()

            verify(exactly = 0) { onVersionChange(any()) }
        }

    @Test
    fun `loadVersion falls back when initialVersionId load throws`() =
        runTest(testDispatcher) {
            val english = BibleVersion(id = 11, abbreviation = "NIV", languageTag = "en")
            coEvery { bibleVersionRepository.version(id = 42) } coAnswers { throw RuntimeException("not permitted") }
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(english)
            coEvery { bibleVersionRepository.version(id = 11) } returns english

            var received: BibleVersion? = null
            createViewModel(initialVersionId = 42, onVersionChange = { received = it })
            advanceUntilIdle()

            assertEquals(english, received)
        }

    @Test
    fun `loadVersions on success loads permitted and active language versions concurrently`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleVersionRepository.fullVersions("en") } returns listOf(activeEn)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(listOf(permittedEn), viewModel.state.value.permittedMinimalVersions)
            assertEquals(listOf(activeEn), viewModel.state.value.activeLanguageVersions)
            assertFalse(viewModel.state.value.hasLoadFailed)
            coVerify(exactly = 2) { bibleVersionRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleVersionRepository.fullVersions("en") }
        }

    @Test
    fun `loadVersions on exception leaves initializing false and does not crash`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleVersionRepository.fullVersions("en") } coAnswers {
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
            assertTrue(viewModel.state.value.hasLoadFailed)
            assertFalse(viewModel.state.value.showEmptyState)
            coVerify(exactly = 2) { bibleVersionRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleVersionRepository.fullVersions("en") }
        }

    @Test
    fun `loadVersions when permitted listing throws leaves initializing false`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } coAnswers {
                throw RuntimeException("listing failed")
            }
            coEvery { bibleVersionRepository.fullVersions("en") } returns listOf(activeEn)

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
            assertTrue(viewModel.state.value.hasLoadFailed)
            assertFalse(viewModel.state.value.showEmptyState)
            coVerify(exactly = 2) { bibleVersionRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleVersionRepository.fullVersions("en") }
        }

    @Test
    fun `loadVersions when both concurrent calls fail leaves initializing false and calls both repositories`() =
        runTest(testDispatcher) {
            val unauthorized = RuntimeException("401 Unauthorized")
            val unavailable = RuntimeException("503 Service Unavailable")
            coEvery { bibleVersionRepository.permittedVersionsListing() } coAnswers { throw unauthorized }
            coEvery { bibleVersionRepository.fullVersions("en") } coAnswers { throw unavailable }

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
            assertTrue(viewModel.state.value.hasLoadFailed)
            assertFalse(viewModel.state.value.showEmptyState)
            coVerify(exactly = 2) { bibleVersionRepository.permittedVersionsListing() }
            coVerify(exactly = 1) { bibleVersionRepository.fullVersions("en") }
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
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `loadVersionsForLanguage on success updates active language versions and name`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("es") } returns listOf(spanishVersion)
            every { languageRepository.languageName("es") } returns "Español"

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
    fun `loadVersionsForLanguage sets initializing and activeLanguageTag before fetching`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()
            lateinit var viewModel: BibleVersionsViewModel
            coEvery { bibleVersionRepository.fullVersions("es") } coAnswers {
                assertTrue(viewModel.state.value.initializing)
                assertEquals("es", viewModel.state.value.activeLanguageTag)
                listOf(spanishVersion)
            }
            every { languageRepository.languageName("es") } returns "Español"

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadVersionsForLanguage("es")
            advanceUntilIdle()

            assertEquals("es", viewModel.state.value.activeLanguageTag)
            assertEquals(listOf(spanishVersion), viewModel.state.value.activeLanguageVersions)
            assertEquals("Español", viewModel.state.value.activeLanguageName)
            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `loadVersionsForLanguage on exception leaves prior versions and name and clears initializing`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleVersionRepository.fullVersions("en") } returns listOf(activeEn)
            coEvery { bibleVersionRepository.fullVersions("es") } coAnswers {
                throw RuntimeException("network")
            }

            val viewModel = createViewModel()
            advanceUntilIdle()

            val versionsBefore = viewModel.state.value.activeLanguageVersions
            val nameBefore = viewModel.state.value.activeLanguageName

            viewModel.loadVersionsForLanguage("es")
            advanceUntilIdle()

            assertEquals("es", viewModel.state.value.activeLanguageTag)
            assertEquals(versionsBefore, viewModel.state.value.activeLanguageVersions)
            assertEquals(nameBefore, viewModel.state.value.activeLanguageName)
            assertFalse(viewModel.state.value.initializing)
        }

    @Test
    fun `loadVersionsForLanguage when languageName throws after fetch keeps prior versions and name`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns listOf(permittedEn)
            coEvery { bibleVersionRepository.fullVersions("en") } returns listOf(activeEn)
            coEvery { bibleVersionRepository.fullVersions("es") } returns listOf(spanishVersion)
            every { languageRepository.languageName("es") } throws RuntimeException("name lookup failed")

            val viewModel = createViewModel()
            advanceUntilIdle()

            val versionsBefore = viewModel.state.value.activeLanguageVersions
            val nameBefore = viewModel.state.value.activeLanguageName

            viewModel.loadVersionsForLanguage("es")
            advanceUntilIdle()

            assertEquals("es", viewModel.state.value.activeLanguageTag)
            assertEquals(versionsBefore, viewModel.state.value.activeLanguageVersions)
            assertEquals(nameBefore, viewModel.state.value.activeLanguageName)
            assertFalse(viewModel.state.value.initializing)
            coVerify(exactly = 1) { bibleVersionRepository.fullVersions("es") }
            coVerify(exactly = 1) { languageRepository.languageName("es") }
        }

    private fun stubSuccessfulLanguageLoad() {
        coEvery { languageRepository.loadLanguageNames(null) } returns Unit
        coEvery { languageRepository.allPermittedLanguageTags() } returns listOf("en", "es")
        every { languageRepository.languageName("en") } returns "English"
        every { languageRepository.languageName("es") } returns "Spanish"
        coEvery { languageRepository.suggestedLanguageTags() } returns listOf("en")
    }

    @Test
    fun `loadLanguages populates suggested and all language lists`() =
        runTest(testDispatcher) {
            stubSuccessfulLanguageLoad()
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadLanguages()
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
            assertFalse(viewModel.state.value.languagesInitializing)
        }

    @Test
    fun `loadLanguages on loadLanguageNames exception leaves lists empty and clears flag`() =
        runTest(testDispatcher) {
            coEvery { languageRepository.loadLanguageNames(null) } throws RuntimeException("test")
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadLanguages()
            advanceUntilIdle()

            assertTrue(
                viewModel.state.value.allLanguages
                    .isEmpty(),
            )
            assertTrue(
                viewModel.state.value.suggestedLanguages
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.languagesInitializing)
        }

    @Test
    fun `loadLanguages is idempotent after first successful load`() =
        runTest(testDispatcher) {
            stubSuccessfulLanguageLoad()
            coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
            coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.loadLanguages()
            advanceUntilIdle()
            viewModel.loadLanguages()
            advanceUntilIdle()

            coVerify(exactly = 1) { languageRepository.loadLanguageNames(null) }
            coVerify(exactly = 1) { languageRepository.suggestedLanguageTags() }
        }

    @Test
    fun `VersionInfoTapped sets selected bible version and loads organization`() =
        runTest(testDispatcher) {
            mockkObject(YouVersionApi)
            try {
                every { YouVersionApi.organizations } returns mockOrganizationsApi
                coEvery { mockOrganizationsApi.organization("org-1") } returns Organization.preview

                coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

                val version =
                    BibleVersion(
                        id = 99,
                        abbreviation = "T",
                        languageTag = "en",
                        organizationId = "org-1",
                    )
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(BibleVersionsViewModel.Action.VersionInfoTapped(version))
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

                coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

                val version = BibleVersion(id = 100, abbreviation = "X", languageTag = "en", organizationId = null)
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(BibleVersionsViewModel.Action.VersionInfoTapped(version))
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

                coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

                val version =
                    BibleVersion(
                        id = 101,
                        abbreviation = "Y",
                        languageTag = "en",
                        organizationId = "org-2",
                    )
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(BibleVersionsViewModel.Action.VersionInfoTapped(version))
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

                coEvery { bibleVersionRepository.permittedVersionsListing() } returns emptyList()
                coEvery { bibleVersionRepository.fullVersions("en") } returns emptyList()

                val version =
                    BibleVersion(
                        id = 102,
                        abbreviation = "Z",
                        languageTag = "en",
                        organizationId = "org-1",
                    )
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onAction(BibleVersionsViewModel.Action.VersionInfoTapped(version))
                advanceUntilIdle()
                assertEquals(Organization.preview, viewModel.state.value.selectedOrganization)

                viewModel.onAction(BibleVersionsViewModel.Action.VersionDismissed)

                assertNull(viewModel.state.value.selectedBibleVersion)
                assertNull(viewModel.state.value.selectedOrganization)
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `State versionsCount returns permitted minimal versions count`() {
        val state =
            BibleVersionsViewModel.State(
                permittedMinimalVersions = listOf(permittedEn, activeEn),
            )
        assertEquals(2, state.versionsCount)
    }

    @Test
    fun `State languagesCount returns distinct language tags count`() {
        val en2 = BibleVersion(id = 3, abbreviation = "A", languageTag = "en")
        val state =
            BibleVersionsViewModel.State(
                permittedMinimalVersions = listOf(permittedEn, spanishVersion, en2),
            )
        assertEquals(2, state.languagesCount)
    }

    @Test
    fun `State showEmptyState is true when not initializing and no permitted versions`() {
        val empty =
            BibleVersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = emptyList(),
            )
        assertTrue(empty.showEmptyState)
    }

    @Test
    fun `State showEmptyState is false when initializing`() {
        val loading =
            BibleVersionsViewModel.State(
                initializing = true,
                permittedMinimalVersions = emptyList(),
            )
        assertFalse(loading.showEmptyState)
    }

    @Test
    fun `State showEmptyState is false when hasLoadFailed`() {
        val failed =
            BibleVersionsViewModel.State(
                initializing = false,
                permittedMinimalVersions = emptyList(),
                hasLoadFailed = true,
            )
        assertFalse(failed.showEmptyState)
    }

    @Test
    fun `State activeLanguageVersionsCount counts versions matching active language tag`() {
        val state =
            BibleVersionsViewModel.State(
                permittedMinimalVersions = listOf(permittedEn, spanishVersion),
                activeLanguageTag = "en",
            )
        assertEquals(1, state.activeLanguageVersionsCount)
    }
}
