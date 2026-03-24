package com.youversion.platform.reader.domain

import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.languages.models.Language
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibleReaderRepositoryTest {
    private lateinit var previousDefaultLocale: Locale

    @BeforeTest
    fun setupLocale() {
        previousDefaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterTest
    fun restoreLocaleAndCleanup() {
        try {
            unmockkObject(YouVersionApi)
        } catch (_: Throwable) {
        }
        Locale.setDefault(previousDefaultLocale)
    }

    @Test
    fun `lastBibleReference getter returns deserialized BibleReference when storage key exists`() {
        val storage = mockk<Storage>()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val json = Json.encodeToString(reference)
        every { storage.getStringOrNull(STORAGE_KEY_BIBLE_READER_REFERENCE) } returns json
        val repository = createRepository(storage = storage)

        assertEquals(reference, repository.lastBibleReference)
    }

    @Test
    fun `lastBibleReference getter returns null when storage key is missing`() {
        val storage = mockk<Storage>()
        every { storage.getStringOrNull(STORAGE_KEY_BIBLE_READER_REFERENCE) } returns null
        val repository = createRepository(storage = storage)

        assertNull(repository.lastBibleReference)
    }

    @Test
    fun `lastBibleReference setter serializes and stores BibleReference to storage`() {
        val storage = mockk<Storage>(relaxUnitFun = true)
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        every { storage.putString(any(), any()) } just Runs
        val repository = createRepository(storage = storage)

        repository.lastBibleReference = reference

        verify { storage.putString(STORAGE_KEY_BIBLE_READER_REFERENCE, Json.encodeToString(reference)) }
    }

    @Test
    fun `lastBibleReference setter stores null when value is null`() {
        val storage = mockk<Storage>(relaxUnitFun = true)
        every { storage.putString(any(), any()) } just Runs
        val repository = createRepository(storage = storage)

        repository.lastBibleReference = null

        verify { storage.putString(STORAGE_KEY_BIBLE_READER_REFERENCE, null) }
    }

    @Test
    fun `produceBibleReference returns provided reference when non-null`() {
        val repository = createRepository()
        val reference = BibleReference(versionId = 7, bookUSFM = "JHN", chapter = 3)

        assertEquals(reference, repository.produceBibleReference(reference))
    }

    @Test
    fun `produceBibleReference returns lastBibleReference when provided is null and last reference exists`() {
        val storage = mockk<Storage>()
        val saved = BibleReference(versionId = 2, bookUSFM = "PSA", chapter = 23)
        every { storage.getStringOrNull(STORAGE_KEY_BIBLE_READER_REFERENCE) } returns Json.encodeToString(saved)
        every { storage.putString(any(), any()) } just Runs
        val repository = createRepository(storage = storage)

        assertEquals(saved, repository.produceBibleReference(null))
    }

    @Test
    fun `produceBibleReference uses first downloaded version when argument and last saved are null`() {
        val bibleVersionRepository = mockk<BibleVersionRepository>()
        every { bibleVersionRepository.downloadedVersions } returns listOf(99, 100)
        val storage = mockk<Storage>()
        every { storage.getStringOrNull(STORAGE_KEY_BIBLE_READER_REFERENCE) } returns null
        val repository = createRepository(storage = storage, bibleVersionRepository = bibleVersionRepository)

        val result = repository.produceBibleReference(null)

        assertEquals(
            BibleReference(versionId = 99, bookUSFM = "JHN", chapter = 1),
            result,
        )
    }

    @Test
    fun `produceBibleReference falls back to BibleDefaults version and JHN 1 when no downloaded versions`() {
        val bibleVersionRepository = mockk<BibleVersionRepository>()
        every { bibleVersionRepository.downloadedVersions } returns emptyList()
        val storage = mockk<Storage>()
        every { storage.getStringOrNull(STORAGE_KEY_BIBLE_READER_REFERENCE) } returns null
        val repository = createRepository(storage = storage, bibleVersionRepository = bibleVersionRepository)

        val result = repository.produceBibleReference(null)

        assertEquals(
            BibleReference(versionId = BibleDefaults.VERSION_ID, bookUSFM = "JHN", chapter = 1),
            result,
        )
    }

    @Test
    fun `previousChapter returns previous chapter in same book when chapter greater than one`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 3))
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2)

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
            repository.previousChapter(version, reference),
        )
    }

    @Test
    fun `previousChapter returns last chapter of previous book when at chapter one and previous book exists`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 3), book("EXO", chapterCount = 2))
        val reference = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1)

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3),
            repository.previousChapter(version, reference),
        )
    }

    @Test
    fun `previousChapter returns null when at first chapter of first book`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 3))
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertNull(repository.previousChapter(version, reference))
    }

    @Test
    fun `previousChapter with null version decrements chapter when chapter greater than one`() {
        val repository = createRepository()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2)

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
            repository.previousChapter(null, reference),
        )
    }

    @Test
    fun `previousChapter with null version returns null at chapter one`() {
        val repository = createRepository()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertNull(repository.previousChapter(null, reference))
    }

    @Test
    fun `previousChapter returns null when previous book has no chapter metadata`() {
        val repository = createRepository()
        val genWithoutChapters =
            BibleBook(
                id = "GEN",
                title = null,
                fullTitle = null,
                abbreviation = null,
                canon = null,
                chapters = null,
            )
        val exo = book("EXO", chapterCount = 1)
        val version = versionWithBooks(genWithoutChapters, exo)
        val reference = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1)

        assertNull(repository.previousChapter(version, reference))
    }

    @Test
    fun `nextChapter returns next chapter in same book when chapter less than last chapter`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 3))
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2),
            repository.nextChapter(version, reference),
        )
    }

    @Test
    fun `nextChapter returns first chapter of next book when at last chapter of book`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 3), book("EXO", chapterCount = 2))
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3)

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1),
            repository.nextChapter(version, reference),
        )
    }

    @Test
    fun `nextChapter returns null when at last chapter of last book`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 1), book("EXO", chapterCount = 2))
        val reference = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 2)

        assertNull(repository.nextChapter(version, reference))
    }

    @Test
    fun `nextChapter with null version returns null when books list is empty`() {
        val repository = createRepository()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertNull(repository.nextChapter(null, reference))
    }

    @Test
    fun `nextChapter when book not found uses first book at chapter one`() {
        val repository = createRepository()
        val version = versionWithBooks(book("GEN", chapterCount = 2), book("EXO", chapterCount = 1))
        val reference = BibleReference(versionId = 1, bookUSFM = "ZZZ", chapter = 1)

        assertEquals(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
            repository.nextChapter(version, reference),
        )
    }

    @Test
    fun `permittedVersionsListing fetches once and returns cached result on second call`() =
        runTest {
            val bibleVersionRepository = mockk<BibleVersionRepository>()
            val versions =
                listOf(
                    BibleVersion(id = 1, languageTag = "en"),
                    BibleVersion(id = 2, languageTag = "es"),
                )
            coEvery { bibleVersionRepository.permittedVersions(null) } returns versions
            val repository = createRepository(bibleVersionRepository = bibleVersionRepository)

            assertEquals(versions, repository.permittedVersionsListing())
            assertEquals(versions, repository.permittedVersionsListing())

            coVerify(exactly = 1) { bibleVersionRepository.permittedVersions(null) }
        }

    @Test
    fun `fetchVersionsInLanguage deduplicates by id sorts with collator and caches`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                val mockBiblesApi = mockk<BiblesApi>()
                every { YouVersionApi.bible } returns mockBiblesApi
                val versions =
                    listOf(
                        BibleVersion(id = 1, title = "Bible B"),
                        BibleVersion(id = 1, title = "Duplicate id"),
                        BibleVersion(id = 2, title = "Bible A"),
                    )
                coEvery {
                    mockBiblesApi.versions(
                        languageCode = "eng",
                        fields = null,
                        pageSize = 99,
                        pageToken = null,
                    )
                } returns PaginatedResponse(data = versions, nextPageToken = null, totalSize = null)
                val repository = createRepository()

                val first = repository.fetchVersionsInLanguage("eng")
                val second = repository.fetchVersionsInLanguage("eng")

                assertEquals(first, second)
                assertEquals(
                    listOf(2, 1),
                    first.map { it.id },
                )
                coVerify(exactly = 1) {
                    mockBiblesApi.versions(
                        languageCode = "eng",
                        fields = null,
                        pageSize = 99,
                        pageToken = null,
                    )
                }
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `fetchVersionsInLanguage caches empty API response`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                val mockBiblesApi = mockk<BiblesApi>()
                every { YouVersionApi.bible } returns mockBiblesApi
                coEvery {
                    mockBiblesApi.versions(
                        languageCode = "eng",
                        fields = null,
                        pageSize = 99,
                        pageToken = null,
                    )
                } returns PaginatedResponse(data = emptyList(), nextPageToken = null, totalSize = null)
                val repository = createRepository()

                val first = repository.fetchVersionsInLanguage("eng")
                val second = repository.fetchVersionsInLanguage("eng")

                assertContentEquals(emptyList(), first)
                assertContentEquals(emptyList(), second)
                coVerify(exactly = 1) {
                    mockBiblesApi.versions(
                        languageCode = "eng",
                        fields = null,
                        pageSize = 99,
                        pageToken = null,
                    )
                }
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `fetchVersionsInLanguage comparable string uses title when localized title absent`() =
        runTest {
            mockkObject(YouVersionApi)
            try {
                val mockBiblesApi = mockk<BiblesApi>()
                every { YouVersionApi.bible } returns mockBiblesApi
                val versions =
                    listOf(
                        BibleVersion(
                            id = 10,
                            localizedTitle = null,
                            title = "Zebra",
                            localizedAbbreviation = null,
                            abbreviation = null,
                        ),
                        BibleVersion(
                            id = 11,
                            localizedTitle = null,
                            title = "Alpha",
                            localizedAbbreviation = null,
                            abbreviation = null,
                        ),
                    )
                coEvery {
                    mockBiblesApi.versions(
                        languageCode = "eng",
                        fields = null,
                        pageSize = 99,
                        pageToken = null,
                    )
                } returns PaginatedResponse(data = versions, nextPageToken = null, totalSize = null)
                val repository = createRepository()

                val sorted = repository.fetchVersionsInLanguage("eng")

                assertEquals(listOf(11, 10), sorted.map { it.id })
            } finally {
                unmockkObject(YouVersionApi)
            }
        }

    @Test
    fun `allPermittedLanguageTags returns distinct language tags from permitted versions`() =
        runTest {
            val bibleVersionRepository = mockk<BibleVersionRepository>()
            val versions =
                listOf(
                    BibleVersion(id = 1, languageTag = "en"),
                    BibleVersion(id = 2, languageTag = "en"),
                    BibleVersion(id = 3, languageTag = "fr"),
                )
            coEvery { bibleVersionRepository.permittedVersions(null) } returns versions
            val repository = createRepository(bibleVersionRepository = bibleVersionRepository)

            repository.permittedVersionsListing()

            assertContentEquals(listOf("en", "fr"), repository.allPermittedLanguageTags)
        }

    @Test
    fun `allPermittedLanguageTags returns empty list when permittedVersions is null`() {
        val repository = createRepository()

        assertContentEquals(emptyList(), repository.allPermittedLanguageTags)
    }

    @Test
    fun `suggestedLanguageTags returns codes from language repository`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            val languages =
                listOf(
                    Language(language = "de"),
                    Language(language = "fr"),
                )
            coEvery { languageRepository.suggestedLanguages(any()) } returns languages
            val repository = createRepository(languageRepository = languageRepository)

            assertContentEquals(listOf("de", "fr"), repository.suggestedLanguageTags())
        }

    @Test
    fun `suggestedLanguageTags fetches from language repository using localeCountryCode`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.suggestedLanguages("US") } returns listOf(Language(language = "en"))
            val repository = createRepository(languageRepository = languageRepository)

            repository.suggestedLanguageTags()

            coVerify(exactly = 1) { languageRepository.suggestedLanguages("US") }
        }

    @Test
    fun `suggestedLanguageTags uses en and es when API returns empty`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.suggestedLanguages(any()) } returns emptyList()
            val repository = createRepository(languageRepository = languageRepository)

            assertContentEquals(listOf("en", "es"), repository.suggestedLanguageTags())
        }

    @Test
    fun `suggestedLanguageTags filters by permitted versions when loaded`() =
        runTest {
            val bibleVersionRepository = mockk<BibleVersionRepository>()
            val languageRepository = mockk<LanguageRepository>()
            coEvery { bibleVersionRepository.permittedVersions(null) } returns
                listOf(BibleVersion(id = 1, languageTag = "en"))
            coEvery { languageRepository.suggestedLanguages(any()) } returns
                listOf(
                    Language(language = "en"),
                    Language(language = "de"),
                )
            val repository =
                createRepository(
                    bibleVersionRepository = bibleVersionRepository,
                    languageRepository = languageRepository,
                )

            repository.permittedVersionsListing()

            assertContentEquals(listOf("en"), repository.suggestedLanguageTags())
        }

    @Test
    fun `suggestedLanguageTags returns all codes when permittedVersions is empty list`() =
        runTest {
            val bibleVersionRepository = mockk<BibleVersionRepository>()
            val languageRepository = mockk<LanguageRepository>()
            coEvery { bibleVersionRepository.permittedVersions(null) } returns emptyList()
            coEvery { languageRepository.suggestedLanguages(any()) } returns
                listOf(
                    Language(language = "en"),
                    Language(language = "de"),
                )
            val repository =
                createRepository(
                    bibleVersionRepository = bibleVersionRepository,
                    languageRepository = languageRepository,
                )

            repository.permittedVersionsListing()

            assertContentEquals(listOf("en", "de"), repository.suggestedLanguageTags())
        }

    @Test
    fun `suggestedLanguageTags returns all codes when permittedVersions is null`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.suggestedLanguages(any()) } returns
                listOf(
                    Language(language = "en"),
                    Language(language = "de"),
                )
            val repository = createRepository(languageRepository = languageRepository)

            assertContentEquals(listOf("en", "de"), repository.suggestedLanguageTags())
        }

    @Test
    fun `suggestedLanguageTags returns cached value on second call`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.suggestedLanguages(any()) } returns listOf(Language(language = "en"))
            val repository = createRepository(languageRepository = languageRepository)

            val first = repository.suggestedLanguageTags()
            val second = repository.suggestedLanguageTags()

            assertEquals(first, second)
            coVerify(exactly = 1) { languageRepository.suggestedLanguages(any()) }
        }

    @Test
    fun `loadLanguageNames no-ops when languageNames already populated`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(language = "en", displayNames = mapOf("en" to "English")),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)
            repository.loadLanguageNames(null)

            coVerify(exactly = 1) { languageRepository.languages() }
        }

    @Test
    fun `loadLanguageNames builds language name map using best display name`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(
                        language = "en",
                        displayNames = mapOf("en" to "English", "fr" to "Anglais"),
                    ),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)

            assertEquals("English", repository.languageName("en"))
        }

    @Test
    fun `loadLanguageNames skips languages with null language code`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(language = null, displayNames = mapOf("en" to "X")),
                    Language(language = "en", displayNames = mapOf("en" to "English")),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)

            assertEquals("English", repository.languageName("en"))
        }

    @Test
    fun `loadLanguageNames skips languages with null displayNames`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(language = "en", displayNames = null),
                    Language(language = "en", displayNames = mapOf("en" to "English")),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)

            assertEquals("English", repository.languageName("en"))
        }

    @Test
    fun `loadLanguageNames uses single displayNames entry without locale match`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(language = "de", displayNames = mapOf("de" to "Deutsch")),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)

            assertEquals("Deutsch", repository.languageName("de"))
        }

    @Test
    fun `loadLanguageNames prefers locale language code in displayNames`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(
                        language = "de",
                        displayNames =
                            mapOf(
                                "en" to "German (en)",
                                "de" to "German (de)",
                            ),
                    ),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)

            assertEquals("German (en)", repository.languageName("de"))
        }

    @Test
    fun `loadLanguageNames prefers version languageTag when locale does not match`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(
                        language = "de",
                        displayNames =
                            mapOf(
                                "xx" to "Wrong",
                                "de" to "Deutsch",
                            ),
                    ),
                )
            val version = BibleVersion(id = 1, languageTag = "de")
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(version)

            assertEquals("Deutsch", repository.languageName("de"))
        }

    @Test
    fun `loadLanguageNames falls back to en key in displayNames`() =
        runTest {
            val savedLocale = Locale.getDefault()
            Locale.setDefault(Locale.FRANCE)
            try {
                val languageRepository = mockk<LanguageRepository>()
                coEvery { languageRepository.languages() } returns
                    listOf(
                        Language(
                            language = "de",
                            displayNames =
                                mapOf(
                                    "xx" to "Wrong",
                                    "en" to "German",
                                ),
                        ),
                    )
                val repository = createRepository(languageRepository = languageRepository)

                repository.loadLanguageNames(BibleVersion(id = 1, languageTag = "yy"))

                assertEquals("German", repository.languageName("de"))
            } finally {
                Locale.setDefault(savedLocale)
            }
        }

    @Test
    fun `loadLanguageNames falls back to first map entry when no preferred key matches`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(
                    Language(
                        language = "de",
                        displayNames =
                            mapOf(
                                "aa" to "First",
                                "bb" to "Second",
                            ),
                    ),
                )
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(BibleVersion(id = 1, languageTag = "yy"))

            assertEquals("First", repository.languageName("de"))
        }

    @Test
    fun `languageName returns cached name when available`() =
        runTest {
            val languageRepository = mockk<LanguageRepository>()
            coEvery { languageRepository.languages() } returns
                listOf(Language(language = "fr", displayNames = mapOf("en" to "French")))
            val repository = createRepository(languageRepository = languageRepository)

            repository.loadLanguageNames(null)

            assertEquals("French", repository.languageName("fr"))
        }

    @Test
    fun `languageName falls back to JVM display language when not cached`() {
        val repository = createRepository()

        assertEquals("French", repository.languageName("fr"))
    }

    @Test
    fun `languageName falls back to raw language code as last resort`() {
        val repository = createRepository()

        assertEquals("und", repository.languageName("und"))
    }

    private fun createRepository(
        storage: Storage = mockk(relaxed = true),
        bibleVersionRepository: BibleVersionRepository = mockk(relaxed = true),
        languageRepository: LanguageRepository = mockk(relaxed = true),
    ): BibleReaderRepository = BibleReaderRepository(storage, bibleVersionRepository, languageRepository)

    private fun book(
        id: String,
        chapterCount: Int,
    ): BibleBook {
        val chapters =
            List(chapterCount) {
                BibleChapter(
                    id = "${it + 1}",
                    passageId = null,
                    title = null,
                    verses = listOf(BibleVerse(id = "1", passageId = null, title = null)),
                )
            }
        return BibleBook(
            id = id,
            title = null,
            fullTitle = null,
            abbreviation = null,
            canon = null,
            chapters = chapters,
        )
    }

    private fun versionWithBooks(vararg books: BibleBook): BibleVersion =
        BibleVersion(
            id = 1,
            books = books.toList(),
        )

    private companion object {
        private const val STORAGE_KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
    }
}
