package com.youversion.platform.reader

import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.notes.api.NotesApi
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.notes.models.NoteCreateRequest
import com.youversion.platform.core.notes.models.NoteStatus
import com.youversion.platform.reader.BibleReaderViewModel.Action
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleReaderViewModelNotesTests {
    private val chapter = BibleReference(versionId = 111, bookUSFM = "JHN", chapter = 3)
    private val verse1 = BibleReference(111, "JHN", 3, 1)
    private val verse2 = BibleReference(111, "JHN", 3, 2)
    private val verse7 = BibleReference(111, "JHN", 3, 7)

    private var isSignedIn = true
    private var hasWriteNotes = true
    private var isSignInEnabled = true
    private val sessionId = MutableStateFlow<String?>("user:a")
    private val createdRequests = mutableListOf<NoteCreateRequest>()
    private var createNote: suspend (NoteCreateRequest) -> Unit = {}
    private lateinit var highlightsRepository: BibleHighlightsRepository
    private lateinit var viewModel: BibleReaderViewModel

    private val state get() = viewModel.state.value

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val bibleReaderRepository = mockk<BibleReaderRepository>(relaxed = true)
        every { bibleReaderRepository.produceBibleReference(any()) } returns chapter
        every { bibleReaderRepository.highlightRequest } returns null
        val userSettingsRepository = mockk<UserSettingsRepository>(relaxed = true)
        every { userSettingsRepository.readerLineSpacingFraction } returns null
        highlightsRepository = mockk(relaxed = true)
        val notesApi = mockk<NotesApi>()
        coEvery { notesApi.createNote(any()) } coAnswers {
            val request = firstArg<NoteCreateRequest>()
            createdRequests += request
            createNote(request)
            mockk()
        }
        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = mockk(relaxed = true),
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = userSettingsRepository,
                bibleChapterRepository = mockk(relaxed = true),
                languageRepository = mockk(relaxed = true),
                bibleHighlightsRepository = highlightsRepository,
                copyManager = mockk(relaxed = true),
                shareManager = mockk(relaxed = true),
                isSignedIn = { isSignedIn },
                hasHighlightsPermission = { true },
                hasWriteNotesPermission = { hasWriteNotes },
                isSignInEnabled = { isSignInEnabled },
                notesApi = notesApi,
                hasValidToken = { true },
                currentSessionId = { sessionId.value },
                sessionIdChanges = sessionId,
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun select(vararg references: BibleReference) {
        references.forEach { viewModel.onAction(Action.OnVerseTap(it)) }
    }

    @Test
    fun `opens the composer for a nonconsecutive selection and closes the action sheet`() {
        select(verse7, verse2, verse1)

        viewModel.onAction(Action.StartNote)

        val draft = assertNotNull(state.noteDraft)
        assertEquals(listOf(BibleReference(111, "JHN", 3, 1, 2), verse7), draft.references)
        assertEquals("user:a", draft.sessionId)
        assertFalse(state.showVerseActionSheet)
        assertEquals(setOf(verse1, verse2, verse7), state.selectedVerses)
    }

    @Test
    fun `a failed save keeps the draft and retries with the same request id`() {
        createNote = { throw YouVersionNetworkException(YouVersionNetworkException.Reason.SERVER_ERROR) }
        select(verse1)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.UpdateNote(content = "Born again", isPublic = true, hexColor = "ffec5b"))

        viewModel.onAction(Action.SaveNote)

        val failed = assertNotNull(state.noteDraft)
        assertTrue(failed.showsSaveError)
        assertFalse(failed.isSaving)
        assertEquals("Born again", failed.content)

        createNote = {}
        viewModel.onAction(Action.DismissNoteSaveError)
        viewModel.onAction(Action.SaveNote)

        assertEquals(2, createdRequests.size)
        assertEquals(createdRequests[0].requestId, createdRequests[1].requestId)
        assertEquals(NoteStatus.PUBLIC, createdRequests[1].status)
        assertEquals("ffec5b", createdRequests[1].color)
        assertNull(state.noteDraft)
        assertTrue(state.selectedVerses.isEmpty())
    }

    @Test
    fun `editing a draft after a failure saves under a new request id`() {
        createNote = { throw YouVersionNetworkException(YouVersionNetworkException.Reason.SERVER_ERROR) }
        select(verse1)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.SaveNote)

        viewModel.onAction(Action.UpdateNote(content = "text", isPublic = false, hexColor = null))
        viewModel.onAction(Action.SaveNote)

        assertEquals(2, createdRequests.size)
        assertTrue(createdRequests[0].requestId != createdRequests[1].requestId)
        assertEquals("", createdRequests[0].content)
    }

    @Test
    fun `a signed-in reader without the notes permission signs in again and resumes the same draft`() {
        hasWriteNotes = false
        select(verse1)

        viewModel.onAction(Action.StartNote)

        assertTrue(state.shouldStartNoteSignIn)
        assertFalse(state.shouldStartSignIn)
        assertNull(state.noteDraft)

        hasWriteNotes = true
        viewModel.onAction(Action.NoteSignInCompleted)

        assertFalse(state.shouldStartNoteSignIn)
        assertEquals(listOf(verse1), assertNotNull(state.noteDraft).references)
    }

    @Test
    fun `a sign-in that lands in another account does not resume the previous account's draft`() {
        hasWriteNotes = false
        select(verse1)
        viewModel.onAction(Action.StartNote)

        sessionId.value = "user:b"
        hasWriteNotes = true
        viewModel.onAction(Action.NoteSignInCompleted)

        assertNull(state.noteDraft)
        assertTrue(state.selectedVerses.isEmpty())
    }

    @Test
    fun `a sign-in that does not grant the notes permission returns the reader to their selection`() {
        hasWriteNotes = false
        select(verse1, verse7)
        viewModel.onAction(Action.StartNote)

        viewModel.onAction(Action.NoteSignInCompleted)

        assertNull(state.noteDraft)
        assertEquals(setOf(verse1, verse7), state.selectedVerses)
        assertTrue(state.showVerseActionSheet)
    }

    @Test
    fun `a grant that lands after sign-in returns still opens the held draft`() {
        hasWriteNotes = false
        select(verse1, verse7)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.NoteSignInCompleted)

        hasWriteNotes = true
        viewModel.openPendingNoteIfPermitted()

        assertEquals(listOf(verse1, verse7), assertNotNull(state.noteDraft).references)
    }

    @Test
    fun `putting the selection down drops a draft still waiting on the grant`() {
        hasWriteNotes = false
        select(verse1)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.NoteSignInCompleted)
        viewModel.onAction(Action.ClearVerseSelection)

        hasWriteNotes = true
        viewModel.openPendingNoteIfPermitted()

        assertNull(state.noteDraft)
    }

    @Test
    fun `a signed-out reader's draft goes to whoever signs in to continue it`() {
        isSignedIn = false
        hasWriteNotes = false
        sessionId.value = null
        select(verse1)

        viewModel.onAction(Action.StartNote)
        assertTrue(state.shouldStartSignIn)
        assertFalse(state.shouldStartNoteSignIn)

        isSignedIn = true
        hasWriteNotes = true
        sessionId.value = "user:b"
        viewModel.onAction(Action.SignInCompleted)

        assertEquals("user:b", assertNotNull(state.noteDraft).sessionId)
    }

    @Test
    fun `a signed-out draft stays with the first account even while its grant is pending`() {
        isSignedIn = false
        hasWriteNotes = false
        sessionId.value = null
        select(verse1)
        viewModel.onAction(Action.StartNote)
        isSignedIn = true
        sessionId.value = "user:a"
        viewModel.onAction(Action.SignInCompleted)

        isSignedIn = false
        sessionId.value = null
        isSignedIn = true
        hasWriteNotes = true
        sessionId.value = "user:b"
        viewModel.openPendingNoteIfPermitted()

        assertNull(state.noteDraft)
    }

    @Test
    fun `dismissing the sign-in prompt keeps the selection`() {
        isSignedIn = false
        sessionId.value = null
        select(verse2)
        viewModel.onAction(Action.StartNote)

        viewModel.onAction(Action.CancelSignIn)

        assertEquals(setOf(verse2), state.selectedVerses)
        assertTrue(state.showVerseActionSheet)
        assertNull(state.noteDraft)
    }

    @Test
    fun `an account change drops the open draft and its save`() {
        val response = CompletableDeferred<Unit>()
        createNote = { response.await() }
        select(verse1)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.SaveNote)
        assertTrue(assertNotNull(state.noteDraft).isSaving)

        sessionId.value = "user:b"
        response.complete(Unit)

        assertNull(state.noteDraft)
        assertTrue(state.selectedVerses.isEmpty())
    }

    @Test
    fun `dismissing asks to discard only when the draft has changes`() {
        select(verse1)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.UpdateNote(content = "", isPublic = false, hexColor = "b4ffc1"))

        viewModel.onAction(Action.RequestDismissNote)
        assertTrue(assertNotNull(state.noteDraft).showsDiscardConfirmation)

        viewModel.onAction(Action.CancelDiscardNote)
        viewModel.onAction(Action.UpdateNote(content = "", isPublic = false, hexColor = null))
        viewModel.onAction(Action.RequestDismissNote)

        assertNull(state.noteDraft)
        assertTrue(state.selectedVerses.isEmpty())
    }

    @Test
    fun `does nothing for a signed-out reader when sign-in is disabled`() {
        isSignedIn = false
        isSignInEnabled = false
        select(verse1)

        viewModel.onAction(Action.StartNote)

        assertNull(state.noteDraft)
        assertFalse(state.shouldStartSignIn)
        assertTrue(state.showVerseActionSheet)
    }

    @Test
    fun `tapping a note indicator opens the composer for that verse with its saved notes`() {
        val note = mockk<Note>()

        viewModel.onAction(Action.ShowSavedNotes(verse7, listOf(note)))

        val draft = assertNotNull(state.noteDraft)
        assertEquals(listOf(verse7), draft.references)
        assertEquals(listOf(note), draft.savedNotes)
        assertEquals("user:a", draft.sessionId)
    }

    @Test
    fun `saving from a note indicator without write access keeps the typed note through a cancelled grant`() {
        hasWriteNotes = false
        viewModel.onAction(Action.ShowSavedNotes(verse7, listOf(mockk<Note>())))
        viewModel.onAction(Action.UpdateNote("kept", isPublic = false, hexColor = null))

        viewModel.onAction(Action.SaveNote)

        assertTrue(createdRequests.isEmpty())
        assertNull(state.noteDraft)
        assertTrue(state.shouldStartNoteSignIn)

        viewModel.onAction(Action.NoteSignInCompleted)
        assertTrue(state.showVerseActionSheet)
        viewModel.onAction(Action.StartNote)
        assertTrue(state.shouldStartNoteSignIn)

        hasWriteNotes = true
        viewModel.openPendingNoteIfPermitted()

        assertEquals("kept", assertNotNull(state.noteDraft).content)
        viewModel.onAction(Action.SaveNote)
        assertEquals(listOf("kept"), createdRequests.map { it.content })
    }

    @Test
    fun `a note indicator opens nothing while signed out, without notes, or over an open draft`() {
        val note = mockk<Note>()
        viewModel.onAction(Action.ShowSavedNotes(verse7, emptyList()))
        assertNull(state.noteDraft)

        isSignedIn = false
        viewModel.onAction(Action.ShowSavedNotes(verse7, listOf(note)))
        assertNull(state.noteDraft)

        isSignedIn = true
        select(verse1)
        viewModel.onAction(Action.StartNote)
        viewModel.onAction(Action.ShowSavedNotes(verse7, listOf(note)))
        assertEquals(listOf(verse1), assertNotNull(state.noteDraft).references)
    }
}
