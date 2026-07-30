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
import kotlin.test.assertFalse
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
    fun `an account change clears the persisted highlight request`() {
        val storage = mockk<Storage>(relaxed = true)
        val accountIdChanges = MutableStateFlow<String?>("account-a")
        val repository =
            createRepository(
                storage = storage,
                currentAccountId = "account-a",
                accountIdChanges = accountIdChanges,
            )
        repository.highlightRequest = highlightRequest()
        assertNotNull(repository.highlightRequest)

        accountIdChanges.value = "account-b"

        assertNull(repository.highlightRequest)
        assertNull(repository.highlightRequestChanges.value)
        verify { storage.putString(STORAGE_KEY_HIGHLIGHT_REQUEST, null) }
    }

    @Test
    fun `an account change that lands before the first emission clears the highlight request`() {
        val repository =
            createRepository(
                currentAccountId = "account-a",
                accountIdChanges = MutableStateFlow("account-b"),
                storedHighlightRequest = highlightRequest(accountId = "account-a"),
            )

        assertNull(repository.highlightRequest)
        assertNull(repository.highlightRequestChanges.value)
    }

    @Test
    fun `a highlight request reads as absent under another account before the clear runs`() {
        val storage = mockk<Storage>(relaxed = true)
        every { storage.getStringOrNull(STORAGE_KEY_HIGHLIGHT_REQUEST) } returns
            Json.encodeToString(highlightRequest(accountId = "account-a"))
        var signedInAccountId: String? = "account-a"
        val repository =
            BibleReaderRepository(
                storage = storage,
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { signedInAccountId },
                isSignedIn = { signedInAccountId != null },
                accountIdChanges = MutableSharedFlow(),
            )
        assertNotNull(repository.highlightRequest)

        signedInAccountId = "account-b"

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `signing in does not clear a highlight request held while signed out`() {
        val accountIdChanges = MutableStateFlow<String?>(null)
        val repository = createRepository(accountIdChanges = accountIdChanges)
        repository.highlightRequest = highlightRequest()

        accountIdChanges.value = "account-a"

        assertNotNull(repository.highlightRequest)
    }

    @Test
    fun `signing in binds a highlight request held while signed out to that account`() {
        var signedInAccountId: String? = null
        val accountIdChanges = MutableStateFlow<String?>(null)
        val repository =
            BibleReaderRepository(
                storage = storageWithNoStoredRequest(),
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { signedInAccountId },
                isSignedIn = { signedInAccountId != null },
                accountIdChanges = accountIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        signedInAccountId = "account-a"
        accountIdChanges.value = "account-a"

        assertEquals("account-a", repository.highlightRequest?.accountId)
    }

    @Test
    fun `a highlight request answered by one sign-in is refused by the account that signs in after it`() {
        var signedInAccountId: String? = null
        val accountIdChanges = MutableStateFlow<String?>(null)
        val repository =
            BibleReaderRepository(
                storage = storageWithNoStoredRequest(),
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { signedInAccountId },
                isSignedIn = { signedInAccountId != null },
                accountIdChanges = accountIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        signedInAccountId = "account-a"
        accountIdChanges.value = "account-a"
        signedInAccountId = "account-b"
        accountIdChanges.value = "account-b"

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `signing out of a session with no account id clears the highlight request`() {
        var isSignedIn = true
        val accountIdChanges = MutableSharedFlow<String?>(extraBufferCapacity = 1)
        val repository =
            BibleReaderRepository(
                storage = storageWithNoStoredRequest(),
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { null },
                isSignedIn = { isSignedIn },
                accountIdChanges = accountIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        isSignedIn = false
        accountIdChanges.tryEmit(null)

        assertNull(repository.highlightRequestChanges.value)
    }

    @Test
    fun `the highlight request survives a config change that keeps the same account`() =
        runTest {
            val accountIdChanges = MutableSharedFlow<String?>(replay = 1)
            accountIdChanges.emit("account-a")
            val repository = createRepository(accountIdChanges = accountIdChanges)
            repository.highlightRequest = highlightRequest()

            accountIdChanges.emit("account-a")

            assertNotNull(repository.highlightRequest)
        }

    @Test
    fun `a highlight request already in storage survives construction`() {
        val stored = highlightRequest(accountId = "account-a")

        val repository =
            createRepository(
                currentAccountId = "account-a",
                accountIdChanges = MutableStateFlow("account-a"),
                storedHighlightRequest = stored,
            )

        assertEquals(stored, repository.highlightRequest)
    }

    @Test
    fun `a highlight request stored by another account is dropped at construction`() {
        val storage = mockk<Storage>(relaxed = true)

        val repository =
            createRepository(
                storage = storage,
                currentAccountId = "account-b",
                accountIdChanges = MutableStateFlow("account-b"),
                storedHighlightRequest = highlightRequest(accountId = "account-a"),
            )

        assertNull(repository.highlightRequest)
        assertNull(repository.highlightRequestChanges.value)
        verify { storage.putString(STORAGE_KEY_HIGHLIGHT_REQUEST, null) }
    }

    @Test
    fun `a highlight request stored by an account is dropped at construction when signed out`() {
        val repository =
            createRepository(
                currentAccountId = null,
                storedHighlightRequest = highlightRequest(accountId = "account-a"),
            )

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `a highlight request stored while signed out survives construction under a signed-in account`() {
        val stored = highlightRequest()

        val repository =
            createRepository(
                currentAccountId = "account-a",
                accountIdChanges = MutableStateFlow("account-a"),
                storedHighlightRequest = stored,
            )

        assertEquals(stored, repository.highlightRequest)
    }

    @Test
    fun `setting a highlight request stamps it with the signed-in account`() {
        val repository = createRepository(currentAccountId = "account-a")

        repository.highlightRequest = highlightRequest()

        assertEquals("account-a", repository.highlightRequest?.accountId)
    }

    @Test
    fun `setting a highlight request while signed out records that no session made it`() {
        val repository = createRepository(currentAccountId = null, isSignedIn = false)

        repository.highlightRequest = highlightRequest()

        assertNull(repository.highlightRequest?.accountId)
        assertFalse(repository.highlightRequest?.isRequestedWhileSignedIn == true)
    }

    @Test
    fun `a highlight request made by a session with no account id is not handed to a named account`() {
        val storage = storageWithNoStoredRequest()
        var signedInAccountId: String? = null
        val repository =
            BibleReaderRepository(
                storage = storage,
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { signedInAccountId },
                isSignedIn = { true },
                accountIdChanges = MutableSharedFlow(),
            )
        repository.highlightRequest = highlightRequest()
        assertNotNull(repository.highlightRequest)

        signedInAccountId = "account-b"

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `a highlight request made by a session with no account id is not handed to a signed-out reader`() {
        val storage = storageWithNoStoredRequest()
        var isSignedIn = true
        val repository =
            BibleReaderRepository(
                storage = storage,
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { null },
                isSignedIn = { isSignedIn },
                accountIdChanges = MutableSharedFlow(),
            )
        repository.highlightRequest = highlightRequest()
        assertNotNull(repository.highlightRequest)

        isSignedIn = false

        assertNull(repository.highlightRequest)
    }

    @Test
    fun `a highlight request made by a session with no account id survives while that session lasts`() {
        val repository = createRepository(currentAccountId = null, isSignedIn = true)

        repository.highlightRequest = highlightRequest()

        assertNotNull(repository.highlightRequest)
    }

    @Test
    fun `leaving a session with no account id clears the highlight request`() {
        val storage = mockk<Storage>(relaxed = true)
        val accountIdChanges = MutableStateFlow<String?>(null)
        val repository =
            createRepository(
                storage = storage,
                currentAccountId = null,
                isSignedIn = true,
                accountIdChanges = accountIdChanges,
            )
        repository.highlightRequest = highlightRequest()

        accountIdChanges.value = "account-b"

        assertNull(repository.highlightRequestChanges.value)
        verify { storage.putString(STORAGE_KEY_HIGHLIGHT_REQUEST, null) }
    }

    @Test
    fun `a stored highlight request that names no session is not applied to a signed-in account`() {
        val legacyJson =
            Json.encodeToString(highlightRequest(accountId = null, isRequestedWhileSignedIn = true))
        assertFalse(legacyJson.contains("accountId"))
        assertFalse(legacyJson.contains("isRequestedWhileSignedIn"))
        val storage = mockk<Storage>(relaxed = true)
        every { storage.getStringOrNull(STORAGE_KEY_HIGHLIGHT_REQUEST) } returns legacyJson

        val repository =
            BibleReaderRepository(
                storage = storage,
                bibleVersionRepository = mockk(relaxed = true),
                scope = CoroutineScope(Dispatchers.Unconfined),
                currentAccountId = { "account-a" },
                isSignedIn = { true },
                accountIdChanges = MutableStateFlow("account-a"),
            )

        assertNull(repository.highlightRequest)
        verify { storage.putString(STORAGE_KEY_HIGHLIGHT_REQUEST, null) }
    }

    private fun storageWithNoStoredRequest(): Storage =
        mockk<Storage>(relaxed = true).also {
            every { it.getStringOrNull(STORAGE_KEY_HIGHLIGHT_REQUEST) } returns null
        }

    private fun highlightRequest(
        accountId: String? = null,
        isRequestedWhileSignedIn: Boolean = accountId != null,
    ): HighlightRequest =
        HighlightRequest(
            references = listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
            hexColor = "#ff00ff",
            isRemoval = false,
            accountId = accountId,
            isRequestedWhileSignedIn = isRequestedWhileSignedIn,
        )

    private fun createRepository(
        storage: Storage = mockk(relaxed = true),
        bibleVersionRepository: BibleVersionRepository = mockk(relaxed = true),
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        currentAccountId: String? = null,
        isSignedIn: Boolean = currentAccountId != null,
        accountIdChanges: Flow<String?> = MutableStateFlow(null),
        storedHighlightRequest: HighlightRequest? = null,
    ): BibleReaderRepository {
        every { storage.getStringOrNull(STORAGE_KEY_HIGHLIGHT_REQUEST) } returns
            storedHighlightRequest?.let { Json.encodeToString(it) }
        return BibleReaderRepository(
            storage = storage,
            bibleVersionRepository = bibleVersionRepository,
            scope = scope,
            currentAccountId = { currentAccountId },
            isSignedIn = { isSignedIn },
            accountIdChanges = accountIdChanges,
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
