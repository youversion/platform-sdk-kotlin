package com.youversion.platform.reader.domain

import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
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
        unmockkAll()
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

    private fun createRepository(
        storage: Storage = mockk(relaxed = true),
        bibleVersionRepository: BibleVersionRepository = mockk(relaxed = true),
    ): BibleReaderRepository = BibleReaderRepository(storage, bibleVersionRepository)

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
