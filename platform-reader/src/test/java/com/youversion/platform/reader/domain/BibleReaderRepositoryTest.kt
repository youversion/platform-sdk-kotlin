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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    @Test
    fun `a session change clears the persisted highlight request`() {
        val storage = mockk<Storage>(relaxed = true)
        val sessionIdChanges = MutableStateFlow<String?>("session-a")
        val repository =
            createRepository(
                storage = storage,
                currentSessionId = { "session-a" },
                sessionIdChanges = sessionIdChanges,
            )
        repository.highlightRequest = highlightRequest()
        assertNotNull(repository.highlightRequest)

        sessionIdChanges.value = "session-b"

        assertNull(repository.highlightRequest)
        assertNull(repository.highlightRequestChanges.value)
        verify { storage.putString(STORAGE_KEY_HIGHLIGHT_REQUEST, null) }
    }

    @Test
    fun `a session change that lands before the first emission clears the highlight request`() {
        val repository =
            createRepository(
                currentSessionId = { "session-a" },
                sessionIdChanges = MutableStateFlow("session-b"),
                storedHighlightRequest = highlightRequest(sessionId = "session-a"),
            )

        assertNull(repository.highlightRequest)
        assertNull(repository.highlightRequestChanges.value)
    }

    @Test
    fun `a highlight request reads as absent in another session before the clear runs`() {
        var sessionId: String? = "session-a"
        val repository =
            createRepository(
                currentSessionId = { sessionId },
                sessionIdChanges = MutableSharedFlow(),
                storedHighlightRequest = highlightRequest(sessionId = "session-a"),
            )
        assertNotNull(repository.highlightRequest)

        sessionId = "session-b"

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `signing in does not clear a highlight request held while signed out`() {
        val sessionIdChanges = MutableStateFlow<String?>(null)
        val repository = createRepository(sessionIdChanges = sessionIdChanges)
        repository.highlightRequest = highlightRequest()

        sessionIdChanges.value = "session-a"

        assertNotNull(repository.highlightRequest)
    }

    @Test
    fun `signing in binds a highlight request held while signed out to that session`() {
        var sessionId: String? = null
        val sessionIdChanges = MutableStateFlow<String?>(null)
        val repository =
            createRepository(
                currentSessionId = { sessionId },
                sessionIdChanges = sessionIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        sessionId = "session-a"
        sessionIdChanges.value = "session-a"

        assertEquals("session-a", repository.highlightRequest?.sessionId)
    }

    @Test
    fun `a highlight request answered by one sign-in is refused by the session that signs in after it`() {
        var sessionId: String? = null
        val sessionIdChanges = MutableStateFlow<String?>(null)
        val repository =
            createRepository(
                currentSessionId = { sessionId },
                sessionIdChanges = sessionIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        sessionId = "session-a"
        sessionIdChanges.value = "session-a"
        sessionId = "session-b"
        sessionIdChanges.value = "session-b"

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `signing out clears the highlight request`() {
        var sessionId: String? = "session-a"
        val sessionIdChanges = MutableSharedFlow<String?>(extraBufferCapacity = 1)
        val repository =
            createRepository(
                currentSessionId = { sessionId },
                sessionIdChanges = sessionIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        sessionId = null
        sessionIdChanges.tryEmit(null)

        assertNull(repository.highlightRequestChanges.value)
    }

    @Test
    fun `the highlight request survives a config change that keeps the same session`() =
        runTest {
            val sessionIdChanges = MutableSharedFlow<String?>(replay = 1)
            sessionIdChanges.emit("session-a")
            val repository =
                createRepository(
                    currentSessionId = { "session-a" },
                    sessionIdChanges = sessionIdChanges,
                )
            repository.highlightRequest = highlightRequest()

            sessionIdChanges.emit("session-a")

            assertNotNull(repository.highlightRequest)
        }

    @Test
    fun `a highlight request already in storage survives construction`() {
        val stored = highlightRequest(sessionId = "session-a")

        val repository =
            createRepository(
                currentSessionId = { "session-a" },
                sessionIdChanges = MutableStateFlow("session-a"),
                storedHighlightRequest = stored,
            )

        assertEquals(stored, repository.highlightRequest)
    }

    @Test
    fun `a highlight request stored by another session is dropped at construction`() {
        val storage = mockk<Storage>(relaxed = true)

        val repository =
            createRepository(
                storage = storage,
                currentSessionId = { "session-b" },
                sessionIdChanges = MutableStateFlow("session-b"),
                storedHighlightRequest = highlightRequest(sessionId = "session-a"),
            )

        assertNull(repository.highlightRequest)
        assertNull(repository.highlightRequestChanges.value)
        verify { storage.putString(STORAGE_KEY_HIGHLIGHT_REQUEST, null) }
    }

    @Test
    fun `a highlight request stored by a session is dropped at construction when signed out`() {
        val repository =
            createRepository(
                storedHighlightRequest = highlightRequest(sessionId = "session-a"),
            )

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `a highlight request stored while signed out is bound to the session that restores it`() {
        val storage = mockk<Storage>(relaxed = true)

        val repository =
            createRepository(
                storage = storage,
                currentSessionId = { "session-a" },
                sessionIdChanges = MutableStateFlow("session-a"),
                storedHighlightRequest = highlightRequest(),
            )

        assertEquals(highlightRequest(sessionId = "session-a"), repository.highlightRequest)
        verify {
            storage.putString(
                STORAGE_KEY_HIGHLIGHT_REQUEST,
                Json.encodeToString(highlightRequest(sessionId = "session-a")),
            )
        }
    }

    @Test
    fun `a highlight request stored while signed out is refused by a session after the one that restored it`() {
        var sessionId: String? = "session-a"
        val repository =
            createRepository(
                currentSessionId = { sessionId },
                sessionIdChanges = MutableSharedFlow(),
                storedHighlightRequest = highlightRequest(),
            )
        assertNotNull(repository.highlightRequest)

        sessionId = "session-b"

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `setting a highlight request stamps it with the current session`() {
        val repository = createRepository(currentSessionId = { "session-a" })

        repository.highlightRequest = highlightRequest()

        assertEquals("session-a", repository.highlightRequest?.sessionId)
    }

    @Test
    fun `setting a highlight request while signed out leaves it unstamped`() {
        val repository = createRepository()

        repository.highlightRequest = highlightRequest()

        assertNotNull(repository.highlightRequest)
        assertNull(repository.highlightRequest?.sessionId)
    }

    private fun highlightRequest(sessionId: String? = null): HighlightRequest =
        HighlightRequest(
            references = listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
            hexColor = "#ff00ff",
            isRemoval = false,
            sessionId = sessionId,
        )

    private fun createRepository(
        storage: Storage = mockk(relaxed = true),
        bibleVersionRepository: BibleVersionRepository = mockk(relaxed = true),
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        currentSessionId: () -> String? = { null },
        sessionIdChanges: Flow<String?> = MutableStateFlow(null),
        storedHighlightRequest: HighlightRequest? = null,
    ): BibleReaderRepository {
        every { storage.getStringOrNull(STORAGE_KEY_HIGHLIGHT_REQUEST) } returns
            storedHighlightRequest?.let { Json.encodeToString(it) }
        return BibleReaderRepository(
            storage = storage,
            bibleVersionRepository = bibleVersionRepository,
            scope = scope,
            currentSessionId = currentSessionId,
            sessionIdChanges = sessionIdChanges,
        )
    }

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
        private const val STORAGE_KEY_HIGHLIGHT_REQUEST = "bible-reader-view--highlight-request"
    }
}
