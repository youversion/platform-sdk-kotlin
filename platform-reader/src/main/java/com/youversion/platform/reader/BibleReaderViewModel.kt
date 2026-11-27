package com.youversion.platform.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.core.highlights.models.highlightRgbOrNull
import com.youversion.platform.core.highlights.models.isSameHighlightColor
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.notes.api.NoteChanges
import com.youversion.platform.core.notes.api.NotesApi
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.users.model.YouVersionPermissions
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.CopyManager
import com.youversion.platform.reader.domain.HighlightRequest
import com.youversion.platform.reader.domain.NoteDraft
import com.youversion.platform.reader.domain.ShareManager
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.sheets.HighlightColor
import com.youversion.platform.reader.sheets.HighlightSwatch
import com.youversion.platform.ui.theme.ReaderTheme
import com.youversion.platform.ui.theme.ui.BibleReaderTheme
import com.youversion.platform.ui.views.components.LanguageRowItem
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import com.youversion.platform.ui.views.versions.BibleVersionsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.HttpURLConnection

internal class BibleReaderViewModel(
    bibleReference: BibleReference?,
    fontDefinitionProvider: FontDefinitionProvider?,
    private val bibleVersionRepository: BibleVersionRepository,
    private val bibleReaderRepository: BibleReaderRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val bibleChapterRepository: BibleChapterRepository,
    private val languageRepository: LanguageRepository,
    private val bibleHighlightsRepository: BibleHighlightsRepository,
    bibleVersionsViewModel: BibleVersionsViewModel? = null,
    private val copyManager: CopyManager,
    private val shareManager: ShareManager,
    private val isSignedIn: () -> Boolean = { YouVersionApi.isSignedIn },
    private val hasHighlightsPermission: () -> Boolean = {
        YouVersionApi.hasPermission(YouVersionPermissions.HIGHLIGHTS)
    },
    private val hasWriteNotesPermission: () -> Boolean = {
        YouVersionApi.hasPermission(YouVersionPermissions.WRITE_NOTES)
    },
    private val isSignInEnabled: () -> Boolean = { YouVersionPlatformConfiguration.isSignInEnabled },
    private val notesApi: NotesApi = YouVersionApi.notes,
    private val hasValidToken: suspend () -> Boolean = { YouVersionApi.hasValidToken() },
    private val currentSessionId: () -> String? = { YouVersionApi.currentSessionId },
    sessionIdChanges: Flow<String?> = YouVersionPlatformConfiguration.configState.map { currentSessionId() },
) : ViewModel() {
    /**
     * A highlight change the reader asked for but that is waiting on the highlights permission. Captured when the
     * change is requested and applied once the user grants access, so the change the reader intended survives the
     * grant round-trip without them having to ask again.
     *
     * [BibleReaderRepository] holds it rather than this view model, so it also survives the reader being recreated
     * while the grant happens in the browser. Keeping no copy here matters: the repository clears it when the signed-in
     * account changes, and a copy held here would outlive that clear and be applied under the account that signed in
     * next.
     */
    private var highlightRequest: HighlightRequest?
        get() = bibleReaderRepository.highlightRequest
        set(value) {
            bibleReaderRepository.highlightRequest = value
        }

    /**
     * The verse a search result asked the reader to point at, kept here rather than on state because it is not the
     * reader's business until the search sheet has finished closing over the move. Consumed by [Action.CloseSearch],
     * so the dim is seen fading in rather than arriving already applied from behind the sheet.
     */
    private var pendingFocusedReference: BibleReference? = null

    /**
     * A note the reader started that waits on signing in for the notes permission. Held in memory only, so it survives
     * the reader being recreated but not the process ending while the browser is open; it holds a selection and no
     * text yet, so tapping Save again is all that is lost.
     */
    private var pendingNoteDraft: NoteDraft? = null
    private var noteMutationJob: Job? = null
    private val _state: MutableStateFlow<State>
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    /**
     * The signed-in user's cached highlights. Exposed so the verse action sheet recomposes when highlights change and
     * re-evaluates [highlightSwatches].
     */
    internal val highlights: StateFlow<List<BibleHighlight>>
        get() = bibleHighlightsRepository.highlights

    internal var bibleReference: BibleReference
        get() = _state.value.bibleReference
        set(value) {
            bibleReaderRepository.lastBibleReference = value
            pendingFocusedReference = pendingFocusedReference?.takeIf { focus -> value.contains(focus) }
            _state.update {
                it.copy(
                    bibleReference = value,
                    focusedReference = it.focusedReference?.takeIf { focus -> value.contains(focus) },
                    scrollTargetReference = it.scrollTargetReference?.takeIf { target -> value.contains(target) },
                )
            }
        }

    internal var bibleVersion: BibleVersion?
        get() = _state.value.bibleVersion
        set(value) = _state.update { it.copy(bibleVersion = value) }

    internal val bibleVersionsViewModel: BibleVersionsViewModel

    init {
        val reference = bibleReaderRepository.produceBibleReference(bibleReference)
        this._state =
            MutableStateFlow(
                State(
                    bibleReference = reference,
                    providedFontDefinitions = fontDefinitionProvider?.fonts() ?: listOf(),
                ),
            )

        this.bibleVersionsViewModel =
            bibleVersionsViewModel ?: BibleVersionsViewModel(
                initialVersionId = reference.versionId,
                onVersionChange = { version ->
                    onHeaderSelectionChange(this.bibleReference.withVersionId(version.id))
                },
                languageRepository = languageRepository,
                bibleVersionRepository = bibleVersionRepository,
            )
        loadUserSettingsFromStorage()
        loadLanguages()
        observeHighlightRequest()
        restoreHighlightRequest()
        sessionIdChanges.onEach(::dropNoteDraftsOfOtherSessions).launchIn(viewModelScope)
    }

    /**
     * Drops the drafts that belong to a session other than [sessionId], so a sign-out or another account signing in
     * never saves or resumes the previous account's note. A pending draft captured while signed out belongs to no
     * session yet and is bound to the first one that signs in, so it is dropped once that account leaves even if its
     * notes permission is still pending.
     */
    private fun dropNoteDraftsOfOtherSessions(sessionId: String?) {
        bindPendingNoteDraft(sessionId)
        if (pendingNoteDraft?.sessionId?.let { it != sessionId } == true) {
            pendingNoteDraft = null
            _state.update {
                it.copy(shouldStartNoteSignIn = false, selectedVerses = emptySet(), showVerseActionSheet = false)
            }
        }
        if (_state.value.noteDraft?.let { it.sessionId != sessionId } == true) {
            noteMutationJob?.cancel()
            noteMutationJob = null
            _state.update { it.copy(noteDraft = null, selectedVerses = emptySet(), showVerseActionSheet = false) }
        }
    }

    /**
     * Mirrors the repository's highlight request into [State.hasHighlightRequest]. Observed rather than written
     * alongside each change so a clear the reader did not make — the signed-in account changing — reaches the state
     * too, and the reader stops offering to apply a request that is no longer held.
     */
    private fun observeHighlightRequest() {
        bibleReaderRepository
            .highlightRequestChanges
            .onEach { request -> _state.update { it.copy(hasHighlightRequest = request != null) } }
            .launchIn(viewModelScope)
    }

    /**
     * Reapplies a highlight requested before a permission grant once that grant has landed. The request is persisted
     * by [BibleReaderRepository], so a reader recreated while the browser grant flow ran still completes it. Anything
     * that did not end in a grant is dropped rather than reprompted.
     */
    private fun restoreHighlightRequest() {
        val restored = bibleReaderRepository.highlightRequest ?: return
        if (hasHighlightsPermission()) {
            applyHighlight(restored)
        } else {
            // The grant may still be settling — sign-in persists it asynchronously. Keep the request so the reader
            // can apply it once the permission lands, via [applyHighlightRequestIfPermitted].
            highlightRequest = restored
        }
    }

    /**
     * Applies a held highlight now that highlights access is available. Called by the reader when the permission
     * lands — whether granted synchronously through data exchange or asynchronously through sign-in — so a request
     * that outlived the reader's recreation still completes. A no-op when nothing is held or access is still absent.
     */
    fun applyHighlightRequestIfPermitted() {
        val request = highlightRequest ?: return
        if (!hasHighlightsPermission()) {
            return
        }
        highlightRequest = null
        applyHighlight(request)
    }

    private fun loadUserSettingsFromStorage() {
        // Restore Theme
        val savedReaderThemeId = userSettingsRepository.readerThemeId
        val savedReaderTheme = ReaderTheme.themeById(savedReaderThemeId)
        BibleReaderTheme.selectedColorScheme.value = savedReaderTheme.colorScheme

        // Restore Font
        val savedFontDefinitionName = userSettingsRepository.readerFontFamilyName
        val allFontDefinitions = _state.value.allFontDefinitions
        allFontDefinitions.find { it.fontName == savedFontDefinitionName }?.let { savedFontDefinition ->
            _state.update { it.copy(selectedFontDefinition = savedFontDefinition) }
        }

        userSettingsRepository.readerFontSize?.let { savedFontSize ->
            _state.update { it.copy(fontSize = savedFontSize.sp) }
        }

        userSettingsRepository.readerLineSpacingFraction?.let { savedFraction ->
            // Snap the stored value to the nearest supported step so a value written by an
            // older app version (or otherwise nudged out of range) still renders correctly on
            // first paint. The next cycle would clamp it back anyway, but the initial frame
            // should not use an arbitrary fraction.
            val clampedFraction =
                ReaderFontSettings.availableLineSpacingFractions
                    .minByOrNull { kotlin.math.abs(it - savedFraction) }
                    ?: ReaderFontSettings.DEFAULT_LINE_SPACING_FRACTION
            _state.update { it.copy(lineSpacingFraction = clampedFraction) }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.OpenFontSettings -> {
                clearFocusedReference()
                _state.update { it.copy(showingFontList = true) }
            }

            is Action.CloseFontSettings -> {
                _state.update { it.copy(showingFontList = false) }
            }

            is Action.OpenSearch -> {
                clearFocusedReference()
                _state.update { it.copy(showingSearch = true) }
            }

            is Action.CloseSearch -> {
                _state.update { it.copy(showingSearch = false) }
                pendingFocusedReference?.let { reference ->
                    pendingFocusedReference = null
                    focusReference(reference)
                }
            }

            is Action.GoToSearchResult -> {
                goToSearchResult(action.reference)
            }

            is Action.DecreaseFontSize -> {
                decreaseFontSize()
            }

            is Action.IncreaseFontSize -> {
                increaseFontSize()
            }

            is Action.CycleLineSpacing -> {
                cycleLineSpacing()
            }

            is Action.SetFontDefinition -> {
                setFontFamily(action)
            }

            is Action.OpenFootnotes -> {
                clearFocusedReference()
                openFootnotes(action)
            }

            is Action.CloseFootnotes -> {
                closeFootnotes()
            }

            is Action.OpenIntroFootnotes -> {
                clearFocusedReference()
                openIntroFootnotes(action)
            }

            is Action.CloseIntroFootnotes -> {
                closeIntroFootnotes()
            }

            is Action.SetReaderTheme -> {
                setReaderTheme(action)
            }

            is Action.GoToNextChapter -> {
                clearVerseSelection()
                if (_state.value.isViewingIntro) {
                    val bookUSFM = _state.value.introBookUSFM ?: bibleReference.bookUSFM
                    _state.update { it.copy(introBookUSFM = null, introPassageId = null) }
                    bibleReference =
                        BibleReference(
                            versionId = bibleReference.versionId,
                            bookUSFM = bookUSFM,
                            chapter = 1,
                        )
                } else {
                    bibleReaderRepository
                        .nextChapter(bibleVersion, bibleReference)
                        ?.let { nextReference ->
                            val nextBook = bibleVersion?.book(nextReference.bookUSFM)
                            if (nextReference.chapter == 1 &&
                                nextReference.bookUSFM != bibleReference.bookUSFM &&
                                nextBook?.hasIntro == true
                            ) {
                                _state.update {
                                    it.copy(
                                        introBookUSFM = nextReference.bookUSFM,
                                        introPassageId = nextBook.intro?.passageId,
                                        focusedReference = null,
                                        scrollTargetReference = null,
                                    )
                                }
                            } else {
                                bibleReference = nextReference
                            }
                        }
                }
            }

            is Action.GoToPreviousChapter -> {
                clearVerseSelection()
                if (_state.value.isViewingIntro) {
                    val books = bibleVersion?.books ?: emptyList()
                    val bookUSFM = _state.value.introBookUSFM
                    val currentBookIndex = books.indexOfFirst { it.id == bookUSFM }
                    if (currentBookIndex > 0) {
                        _state.update { it.copy(introBookUSFM = null, introPassageId = null) }
                        val previousBook = books[currentBookIndex - 1]
                        val lastChapter = previousBook.chapters?.count() ?: 1
                        bibleReference =
                            BibleReference(
                                versionId = bibleReference.versionId,
                                bookUSFM = previousBook.id ?: "",
                                chapter = lastChapter,
                            )
                    }
                } else if (bibleReference.chapter == 1) {
                    val currentBook = bibleVersion?.book(bibleReference.bookUSFM)
                    if (currentBook?.hasIntro == true) {
                        _state.update {
                            it.copy(
                                introBookUSFM = bibleReference.bookUSFM,
                                introPassageId = currentBook.intro?.passageId,
                                focusedReference = null,
                                scrollTargetReference = null,
                            )
                        }
                    } else {
                        bibleReaderRepository
                            .previousChapter(bibleVersion, bibleReference)
                            ?.let { prevReference -> bibleReference = prevReference }
                    }
                } else {
                    bibleReaderRepository
                        .previousChapter(bibleVersion, bibleReference)
                        ?.let { prevReference -> bibleReference = prevReference }
                }
            }

            is Action.OnVerseTap -> {
                clearFocusedReference()
                toggleVerseSelection(action.reference)
            }

            is Action.ScrollToReference -> {
                _state.update { it.copy(scrollTargetReference = action.reference) }
            }

            is Action.ScrollTargetReached -> {
                _state.update { it.copy(scrollTargetReference = null) }
            }

            is Action.FocusReference -> {
                focusReference(action.reference)
            }

            is Action.ClearFocusedReference -> {
                clearFocusedReference()
            }

            is Action.ClearVerseSelection -> {
                clearVerseSelection()
            }

            is Action.CopySelectedVerses -> {
                copySelectedVerses()
            }

            is Action.ShareSelectedVerses -> {
                shareSelectedVerses()
            }

            is Action.PerformCustomVerseAction -> {
                performCustomVerseAction(action.verseAction, action.awaitSheetClosed)
            }

            is Action.AddHighlight -> {
                addHighlight(action.hexColor)
            }

            is Action.RemoveHighlight -> {
                removeHighlight(action.hexColor)
            }

            is Action.StartNote -> {
                startNote()
            }

            is Action.ShowSavedNotes -> {
                showSavedNotes(action.reference, action.notes)
            }

            is Action.NoteSignInCompleted -> {
                continuePendingNote()
            }

            is Action.UpdateNote -> {
                updateNoteDraft { it.edited(action.content, action.isPublic, action.hexColor) }
            }

            is Action.SaveNote -> {
                saveNote()
            }

            is Action.EditSavedNote -> {
                updateNoteDraft { it.editing(action.note) }
            }

            is Action.DeleteSavedNote -> {
                deleteSavedNote(action.note)
            }

            is Action.RequestDismissNote -> {
                requestDismissNote()
            }

            is Action.ConfirmDiscardNote -> {
                discardNote()
            }

            is Action.CancelDiscardNote -> {
                updateNoteDraft { it.copy(showsDiscardConfirmation = false) }
            }

            is Action.DismissNoteSaveError -> {
                updateNoteDraft { it.copy(showsSaveError = false) }
            }

            is Action.SignInCompleted -> {
                continueAfterSignIn()
            }

            is Action.CancelSignIn -> {
                cancelSignIn()
            }

            is Action.ConfirmDataExchange -> {
                confirmDataExchange()
            }

            is Action.CancelDataExchange -> {
                cancelDataExchange()
            }

            is Action.DataExchangeCompleted -> {
                completeDataExchange(action.isHighlightsGranted)
            }
        }
    }

    fun switchToVersion(versionId: Int) {
        val newReference = bibleReference.withVersionId(versionId)
        onHeaderSelectionChange(newReference)
    }

    fun onIntroSelected(
        bookUSFM: String,
        passageId: String,
    ) {
        _state.update { it.copy(introBookUSFM = bookUSFM, introPassageId = passageId) }
        bibleReference = BibleReference(bibleReference.versionId, bookUSFM, chapter = 1)
    }

    fun onHeaderSelectionChange(newReference: BibleReference) {
        clearVerseSelection()
        viewModelScope.launch {
            if (bibleVersion?.id != newReference.versionId) {
                val newVersion = bibleVersionRepository.version(id = newReference.versionId)
                bibleVersion = newVersion
            }
            _state.update { it.copy(introBookUSFM = null, introPassageId = null) }
            bibleReference = newReference
        }
    }

    private fun toggleVerseSelection(reference: BibleReference) {
        highlightRequest = null
        pendingNoteDraft = null
        _state.update { currentState ->
            val newSelection =
                if (currentState.selectedVerses.contains(reference)) {
                    currentState.selectedVerses - reference
                } else {
                    currentState.selectedVerses + reference
                }
            currentState.copy(
                selectedVerses = newSelection,
                showVerseActionSheet = newSelection.isNotEmpty(),
            )
        }
    }

    private fun clearVerseSelection() {
        pendingNoteDraft = null
        _state.update {
            it.copy(
                selectedVerses = emptySet(),
                showVerseActionSheet = false,
            )
        }
    }

    private fun focusReference(reference: BibleReference) {
        val isFocusable = reference.verseStart != null && _state.value.bibleReference.contains(reference)
        if (isFocusable) {
            _state.update { it.copy(focusedReference = reference) }
        }
    }

    private fun clearFocusedReference() {
        // The staged focus goes with it: the reader clears the focus when the verse turns out not to be rendered,
        // which can land before the search sheet has closed over the move that staged it.
        pendingFocusedReference = null
        _state.update { it.copy(focusedReference = null) }
    }

    private fun goToSearchResult(reference: BibleReference) {
        pendingFocusedReference = reference
        // Search is left showing so the sheet can close over the move it started, and closes itself once it has.
        _state.update { it.copy(scrollTargetReference = reference) }
        onHeaderSelectionChange(BibleReference(reference.versionId, reference.bookUSFM, reference.chapter))
    }

    private fun addHighlight(hexColor: String) {
        val references = _state.value.selectedVerses.toList()
        if (references.isEmpty()) return
        highlightOrRequestPermission(
            HighlightRequest(references = references, hexColor = hexColor, isRemoval = false),
        )
    }

    private fun removeHighlight(hexColor: String) {
        val references = _state.value.selectedVerses.toList()
        if (references.isEmpty()) return
        highlightOrRequestPermission(
            HighlightRequest(references = references, hexColor = hexColor, isRemoval = true),
        )
    }

    /**
     * Resolves a highlight change against the reader's state, holding it as [highlightRequest] whenever it cannot be
     * applied yet. A signed-out reader is sent to sign in first; a signed-in reader without highlights access is asked
     * to grant it; otherwise the change applies at once. Add, remove, and recolor all pass through here, so each is
     * gated the same way.
     *
     * Sign-in and the permission prompt are never raised together: a signed-out reader only reaches the permission
     * check after signing in, via [continueAfterSignIn].
     */
    private fun highlightOrRequestPermission(request: HighlightRequest) {
        when {
            !isSignedIn() -> {
                highlightRequest = request
                _state.update { it.copy(shouldStartSignIn = true) }
            }

            !hasHighlightsPermission() -> {
                highlightRequest = request
                _state.update { it.copy(showDataExchangeConfirmation = true) }
            }

            else -> {
                applyHighlight(request)
            }
        }
    }

    /**
     * Continues a held highlight after a sign-in attempt. When the reader is now signed in, it resolves exactly as a
     * fresh request would — applying immediately if they already hold highlights access, or asking for it otherwise —
     * so a reader who granted highlights during sign-in never sees a second prompt. When sign-in did not complete, the
     * held change is dropped and the verse selection left intact.
     */
    private fun continueAfterSignIn() {
        _state.update { it.copy(shouldStartSignIn = false) }
        continuePendingNote()
        val request = highlightRequest ?: return
        if (!isSignedIn()) {
            highlightRequest = null
            return
        }
        if (hasHighlightsPermission()) {
            highlightRequest = null
            applyHighlight(request)
        } else {
            _state.update { it.copy(showDataExchangeConfirmation = true) }
        }
    }

    private fun cancelSignIn() {
        highlightRequest = null
        _state.update { it.copy(shouldStartSignIn = false) }
        pendingNoteDraft?.let { draft ->
            pendingNoteDraft = null
            restoreSelection(draft)
        }
    }

    /**
     * Captures the selection as a note draft and opens the composer, or holds the draft and asks the reader to sign in
     * when they have no account with the notes permission. As in the Swift SDK, a draft already held for the same
     * passage is reused, so retrying after a cancelled grant keeps what was typed. A signed-out reader sees the sign-in prompt first; a
     * signed-in reader goes straight to the browser, since only signing in again grants the notes permission.
     */
    private fun startNote() {
        val selection = _state.value.selectedVerses
        if (selection.isEmpty()) return
        val canWriteNotes = isSignedIn() && hasWriteNotesPermission()
        if (!canWriteNotes && !isSignInEnabled()) return
        val references = BibleReference.referencesByMerging(selection.toList())
        val draft =
            pendingNoteDraft
                ?.takeIf { it.references == references }
                ?.let { it.copy(sessionId = it.sessionId ?: currentSessionId()) }
                ?: NoteDraft(
                    references = references,
                    selection = selection,
                    referenceTitle = noteReferenceTitle(references),
                    sessionId = currentSessionId(),
                )
        if (canWriteNotes) {
            pendingNoteDraft = null
            openNoteDraft(draft)
        } else {
            pendingNoteDraft = draft
            val isSignedIn = isSignedIn()
            _state.update {
                it.copy(
                    showVerseActionSheet = false,
                    shouldStartSignIn = !isSignedIn,
                    shouldStartNoteSignIn = isSignedIn,
                )
            }
        }
    }

    /**
     * Opens a held draft now that the notes permission is available. Called by the reader when the permission lands,
     * since the sign-in that grants it can finish its token exchange after the browser has already returned. A no-op
     * when nothing is held or the permission is still absent.
     */
    fun openPendingNoteIfPermitted() {
        if (pendingNoteDraft != null && isSignedIn() && hasWriteNotesPermission()) {
            continuePendingNote()
        }
    }

    /**
     * Resumes the held draft after a sign-in attempt. It opens only for the account that started it, or for the one
     * that signed in to continue a signed-out draft, and only once the notes permission was granted. One that landed
     * in another account drops the draft. Otherwise the reader returns to their selection and the draft stays held
     * until the permission lands, through [openPendingNoteIfPermitted], or the selection changes.
     */
    private fun continuePendingNote() {
        _state.update { it.copy(shouldStartNoteSignIn = false) }
        val draft = pendingNoteDraft ?: return
        val sessionId = currentSessionId()
        when {
            draft.sessionId != null && draft.sessionId != sessionId -> {
                pendingNoteDraft = null
                clearVerseSelection()
            }

            isSignedIn() && hasWriteNotesPermission() -> {
                pendingNoteDraft = null
                openNoteDraft(draft.copy(sessionId = sessionId))
            }

            else -> {
                bindPendingNoteDraft(sessionId)
                restoreSelection(draft)
            }
        }
    }

    /** Binds a held draft captured while signed out to [sessionId], the first session seen after it. */
    private fun bindPendingNoteDraft(sessionId: String?) {
        val draft = pendingNoteDraft ?: return
        if (draft.sessionId == null && sessionId != null) {
            pendingNoteDraft = draft.copy(sessionId = sessionId)
        }
    }

    private fun openNoteDraft(draft: NoteDraft) {
        _state.update { it.copy(noteDraft = draft, showVerseActionSheet = false, selectedVerses = draft.selection) }
    }

    private fun restoreSelection(draft: NoteDraft) {
        _state.update { it.copy(selectedVerses = draft.selection, showVerseActionSheet = true) }
    }

    /**
     * Opens the composer for [reference] with the [notes] already attached to it listed below, unless they are empty,
     * nobody is signed in, or a draft is already open. As in the Swift SDK, the verse selection is left as it is.
     */
    private fun showSavedNotes(
        reference: BibleReference,
        notes: List<Note>,
    ) {
        if (notes.isEmpty() || !isSignedIn() || _state.value.noteDraft != null) return
        pendingNoteDraft = null
        val references = listOf(reference)
        val draft =
            NoteDraft(
                references = references,
                selection = setOf(reference),
                referenceTitle = noteReferenceTitle(references),
                sessionId = currentSessionId(),
                savedNotes = notes,
            )
        _state.update { it.copy(noteDraft = draft, showVerseActionSheet = false) }
    }

    /** The passage [references] name in the reader's version, with gaps kept, such as `John 3:1-2, 7`. */
    fun noteReferenceTitle(references: List<BibleReference>): String =
        references.joinToString(", ") { reference ->
            bibleVersion?.displayTitle(reference, includesVersionAbbreviation = false) ?: reference.asUSFM
        }

    /** Applies [change] to the open draft, unless there is none or a save or deletion is in flight. */
    private fun updateNoteDraft(change: (NoteDraft) -> NoteDraft) {
        _state.update { state ->
            val draft = state.noteDraft
            if (draft == null || draft.isBusy) state else state.copy(noteDraft = change(draft))
        }
    }

    /**
     * Creates the open draft's note, or changes the saved note it is editing, sending only the fields that changed.
     * Without the write permission the draft is held, text included, while the reader grants it, and reopens once it
     * lands. A failure keeps the draft, request id included, so retrying cannot create the note twice. Success closes
     * the composer; the note indicators then reload the chapter's notes and highlights.
     */
    private fun saveNote() {
        val draft = _state.value.noteDraft ?: return
        if (!draft.canSave || draft.sessionId != currentSessionId() || !holdNoteDraftUnlessWritable(draft)) return
        val editingNote = draft.editingNote
        val save: suspend () -> Unit = {
            if (editingNote == null) {
                notesApi.createNote(draft.createRequest())
            } else {
                notesApi.updateNote(editingNote.id, draft.update())
            }
        }
        mutateNote(draft, draft.copy(isSaving = true), "Failed to save note", save) {
            _state.update { it.copy(noteDraft = null) }
            clearVerseSelection()
        }
    }

    /**
     * Deletes [note], one of the open draft's saved notes, keeping the composer and anything the reader typed. It
     * needs the write permission as saving does, and a failure leaves the note listed. A note the server no longer
     * has counts as deleted, so retrying a delete whose response was lost does not fail forever.
     */
    private fun deleteSavedNote(note: Note) {
        val draft = _state.value.noteDraft ?: return
        if (draft.isBusy || draft.sessionId != currentSessionId() || note !in draft.savedNotes) return
        if (!holdNoteDraftUnlessWritable(draft)) return
        val delete: suspend () -> Unit = {
            try {
                notesApi.deleteNote(note.id)
            } catch (e: YouVersionNetworkException) {
                if (e.statusCode != HttpURLConnection.HTTP_NOT_FOUND) throw e
                NoteChanges.notifyChanged()
            }
        }
        mutateNote(draft, draft.copy(isDeleting = true), "Failed to delete note", delete) {
            _state.update { state -> state.copy(noteDraft = state.noteDraft?.deleting(note)?.copy(isDeleting = false)) }
        }
    }

    /**
     * Whether the reader can write notes now. Otherwise the open [draft] is held while they sign in for the
     * permission, or, when sign-in is disabled, shows an error.
     */
    private fun holdNoteDraftUnlessWritable(draft: NoteDraft): Boolean {
        if (hasWriteNotesPermission()) return true
        if (isSignInEnabled()) {
            pendingNoteDraft = draft
            _state.update { it.copy(noteDraft = null, shouldStartNoteSignIn = true) }
        } else {
            updateNoteDraft { it.copy(showsSaveError = true) }
        }
        return false
    }

    /**
     * Shows [busyDraft] while [mutation] runs, then calls [onSuccess] or shows the error over [draft]. A result that
     * returns after the draft closed or the account changed is ignored.
     */
    private fun mutateNote(
        draft: NoteDraft,
        busyDraft: NoteDraft,
        failureMessage: String,
        mutation: suspend () -> Unit,
        onSuccess: () -> Unit,
    ) {
        _state.update { it.copy(noteDraft = busyDraft.copy(showsSaveError = false)) }
        noteMutationJob =
            viewModelScope.launch {
                val isMutated =
                    try {
                        hasValidToken()
                        mutation()
                        true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.w(failureMessage, e)
                        false
                    }
                noteMutationJob = null
                if (_state.value.noteDraft?.requestId != draft.requestId || draft.sessionId != currentSessionId()) {
                    return@launch
                }
                if (isMutated) {
                    onSuccess()
                } else {
                    _state.update { it.copy(noteDraft = draft.copy(showsSaveError = true)) }
                }
            }
    }

    private fun requestDismissNote() {
        val draft = _state.value.noteDraft ?: return
        when {
            draft.isBusy -> Unit
            draft.hasChanges -> updateNoteDraft { it.copy(showsDiscardConfirmation = true) }
            else -> discardNote()
        }
    }

    private fun discardNote() {
        if (_state.value.noteDraft?.isBusy != false) return
        _state.update { it.copy(noteDraft = null) }
        clearVerseSelection()
    }

    private fun applyHighlight(request: HighlightRequest) {
        if (request.isRemoval) {
            bibleHighlightsRepository.removeHighlights(request.references, matchingColor = request.hexColor)
        } else {
            bibleHighlightsRepository.addHighlights(request.references, request.hexColor)
        }
        highlightRequest = null
        clearVerseSelection()
    }

    private fun confirmDataExchange() {
        _state.update { it.copy(showDataExchangeConfirmation = false, shouldStartDataExchangeFlow = true) }
    }

    private fun cancelDataExchange() {
        highlightRequest = null
        _state.update { it.copy(showDataExchangeConfirmation = false) }
    }

    /**
     * Finishes the permission flow. Applies the held highlight when highlights access was granted; otherwise drops
     * it. Either way the verse selection is left intact on a non-grant, so the reader can still copy or share.
     */
    private fun completeDataExchange(isHighlightsGranted: Boolean) {
        val request = highlightRequest
        highlightRequest = null
        _state.update { it.copy(shouldStartDataExchangeFlow = false) }
        if (isHighlightsGranted && request != null) {
            applyHighlight(request)
        }
    }

    /**
     * The color picker's swatches for the selection, as the Swift SDK builds them: one per red, green and blue, the
     * palette in order and then each other color on the selection. A color is selected, and removed when tapped, when
     * it is on every selected verse or is outside the palette; a palette color on only some of them is added to all.
     */
    fun highlightSwatches(): List<HighlightSwatch> {
        val paletteHexColors = HighlightColor.entries.map { it.hexColor }
        val selectedHexColors =
            _state.value.selectedVerses
                .flatMap { bibleHighlightsRepository.highlights(overlapping = it) }
                .map { it.hexColor.lowercase() }
                .distinct()
                .filter { highlightRgbOrNull(it) != null }
                .sortedWith(
                    compareBy(
                        { hex ->
                            paletteHexColors.indexOfFirst { isSameHighlightColor(it, hex) }.takeIf { it >= 0 }
                                ?: Int.MAX_VALUE
                        },
                        { it },
                    ),
                )
        val selectedByRgb = selectedHexColors.reversed().associateBy { highlightRgbOrNull(it) }
        val availableRgb =
            paletteHexColors.filterNot { isColorPresentOnAllSelectedVerses(it) }.mapNotNullTo(mutableSetOf()) {
                highlightRgbOrNull(it)
            }
        val seenRgb = mutableSetOf<Long>()
        return (paletteHexColors + selectedHexColors).mapNotNull { hex ->
            val rgb = highlightRgbOrNull(hex)?.takeIf { seenRgb.add(it) } ?: return@mapNotNull null
            val isSelected = rgb !in availableRgb
            HighlightSwatch(
                hexColor = if (isSelected) selectedByRgb[rgb] ?: hex else hex,
                highlightColor = HighlightColor.entries.firstOrNull { highlightRgbOrNull(it.hexColor) == rgb },
                isSelected = isSelected,
            )
        }
    }

    /**
     * Whether [hexColor] is already highlighted on every currently selected verse. Returns false when nothing is
     * selected. The color picker uses this to decide between an add and a remove affordance for the color.
     */
    fun isColorPresentOnAllSelectedVerses(hexColor: String): Boolean {
        val selectedVerses = _state.value.selectedVerses
        return selectedVerses.isNotEmpty() && selectedVersesWithColor(hexColor).size == selectedVerses.size
    }

    private fun selectedVersesWithColor(hexColor: String): Set<BibleReference> =
        _state.value.selectedVerses.filterTo(mutableSetOf()) { reference ->
            bibleHighlightsRepository
                .highlights(overlapping = reference)
                .any { isSameHighlightColor(it.hexColor, hexColor) }
        }

    private fun copySelectedVerses() {
        val version = bibleVersion ?: return
        val selectedVerses = _state.value.selectedVerses.toList()
        if (selectedVerses.isEmpty()) return

        val mergedReferences = BibleReference.referencesByMerging(selectedVerses)

        clearVerseSelection()

        viewModelScope.launch {
            val textSegments =
                mergedReferences.mapNotNull { reference ->
                    val plainText =
                        BibleVersionRendering.plainTextOf(
                            bibleChapterRepository,
                            reference,
                        ) ?: return@mapNotNull null
                    val title = version.displayTitle(reference)
                    val url = version.shareUrl(reference)
                    buildString {
                        append(plainText)
                        append("\n")
                        append(title)
                        if (url != null) {
                            append("\n")
                            append(url)
                        }
                    }
                }

            if (textSegments.isNotEmpty()) {
                copyManager.copyText(label = "verse", text = textSegments.joinToString("\n\n"))
            }
        }
    }

    private fun performCustomVerseAction(
        verseAction: BibleReaderVerseAction,
        awaitSheetClosed: suspend () -> Unit,
    ) {
        val selectedVerses = _state.value.selectedVerses.toList()
        if (selectedVerses.isEmpty()) return
        val mergedReferences = BibleReference.referencesByMerging(selectedVerses)
        clearVerseSelection()
        viewModelScope.launch {
            awaitSheetClosed()
            verseAction.onClick?.invoke(mergedReferences)
        }
    }

    private fun shareSelectedVerses() {
        val version = bibleVersion ?: return
        val selectedVerses = _state.value.selectedVerses.toList()
        if (selectedVerses.isEmpty()) return

        val mergedReferences = BibleReference.referencesByMerging(selectedVerses)
        val shareTitle = mergedReferences.joinToString(", ") { version.displayTitle(it) }
        val shareUrl = mergedReferences.firstNotNullOfOrNull { version.shareUrl(it) } ?: ""
        val shareText =
            buildString {
                append(shareTitle)
                if (shareUrl.isNotEmpty()) {
                    append("\n")
                    append(shareUrl)
                }
            }

        clearVerseSelection()

        shareManager.shareText(text = shareText, title = shareTitle)
    }

    private fun decreaseFontSize() {
        val currentFontSize = _state.value.fontSize
        val nextFontSize = ReaderFontSettings.nextSmallerFontSize(currentFontSize)
        setFontSize(nextFontSize)
    }

    private fun increaseFontSize() {
        val currentFontSize = _state.value.fontSize
        val nextFontSize = ReaderFontSettings.nextLargerFontSize(currentFontSize)
        setFontSize(nextFontSize)
    }

    private fun setFontSize(size: TextUnit) {
        userSettingsRepository.readerFontSize = size.value
        _state.update { it.copy(fontSize = size) }
    }

    private fun cycleLineSpacing() {
        val current = _state.value.lineSpacingFraction
        val next = ReaderFontSettings.nextLineSpacingFraction(current)
        userSettingsRepository.readerLineSpacingFraction = next
        _state.update { it.copy(lineSpacingFraction = next) }
    }

    private fun setFontFamily(action: Action.SetFontDefinition) {
        userSettingsRepository.readerFontFamilyName = action.fontDefinition.fontName
        _state.update { it.copy(selectedFontDefinition = action.fontDefinition) }
    }

    private fun openFootnotes(action: Action.OpenFootnotes) {
        _state.update {
            it.copy(
                showingFootnotes = true,
                footnotesReference = action.reference,
                footnotes = action.footnotes,
            )
        }
    }

    private fun closeFootnotes() {
        _state.update {
            it.copy(
                showingFootnotes = false,
                footnotesReference = null,
                footnotes = emptyList(),
            )
        }
    }

    private fun openIntroFootnotes(action: Action.OpenIntroFootnotes) {
        _state.update {
            it.copy(
                showingIntroFootnotes = true,
                introFootnotes = action.footnotes,
            )
        }
    }

    private fun closeIntroFootnotes() {
        _state.update {
            it.copy(
                showingIntroFootnotes = false,
                introFootnotes = emptyList(),
            )
        }
    }

    private fun setReaderTheme(action: Action.SetReaderTheme) {
        BibleReaderTheme.selectedColorScheme.value = action.readerTheme.colorScheme
        userSettingsRepository.readerThemeId = action.readerTheme.id
    }

    // ----- Languages
    private fun loadLanguages() {
        viewModelScope.launch {
            try {
                languageRepository.loadLanguageNames(bibleVersion)
            } catch (e: Exception) {
                Logger.w("Failed to get languages", e)
            }
        }
    }

    // ----- State
    data class State(
        val bibleReference: BibleReference,
        val bibleVersion: BibleVersion? = null,
        val showCopyright: Boolean = false,
        val showingFontList: Boolean = false,
        val showingSearch: Boolean = false,
        val defaultFontDefinitions: List<FontDefinition> = ReaderFontSettings.defaultFontDefinitions,
        val providedFontDefinitions: List<FontDefinition> = listOf(),
        val selectedFontDefinition: FontDefinition = ReaderFontSettings.DEFAULT_FONT_DEFINITION,
        val fontSize: TextUnit = ReaderFontSettings.DEFAULT_FONT_SIZE,
        val lineSpacingFraction: Float = ReaderFontSettings.DEFAULT_LINE_SPACING_FRACTION,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
        val showingFootnotes: Boolean = false,
        val footnotesReference: BibleReference? = null,
        val footnotes: List<AnnotatedString> = emptyList(),
        val selectedVerses: Set<BibleReference> = emptySet(),
        val showVerseActionSheet: Boolean = false,
        val shouldStartSignIn: Boolean = false,
        val showDataExchangeConfirmation: Boolean = false,
        val shouldStartDataExchangeFlow: Boolean = false,
        val hasHighlightRequest: Boolean = false,
        /** The note the composer is showing, or `null` when it is closed. */
        val noteDraft: NoteDraft? = null,
        /** Whether a signed-in reader is to be sent to sign in again for the notes permission. */
        val shouldStartNoteSignIn: Boolean = false,
        val showingIntroFootnotes: Boolean = false,
        val introFootnotes: List<AnnotatedString> = emptyList(),
        val introBookUSFM: String? = null,
        val introPassageId: String? = null,
        val scrollTargetReference: BibleReference? = null,
        /** The verse the reader is being pointed at, drawn at full strength while the rest of the chapter dims. */
        val focusedReference: BibleReference? = null,
    ) {
        val isViewingIntro: Boolean
            get() = introBookUSFM != null && introPassageId != null

        val bookName: String
            get() =
                bibleVersion?.let { version ->
                    val usfm = introBookUSFM ?: bibleReference.bookUSFM
                    version.bookName(usfm) ?: usfm
                } ?: ""

        val chapterNumber: Int
            get() = bibleReference.chapter

        val versionAbbreviation: String
            get() =
                bibleVersion?.let { version ->
                    version.localizedAbbreviation
                        ?: version.abbreviation
                        ?: version.id.toString()
                } ?: ""

        val fontFamily: FontFamily
            get() = selectedFontDefinition.fontFamily

        val allFontDefinitions: List<FontDefinition>
            get() = defaultFontDefinitions + providedFontDefinitions

        /**
         * The reference label shown in the reader's passage picker, empty while the book name is unknown.
         *
         * @param introLabel the localized word for an intro chapter, used in place of a chapter number.
         */
        fun bookAndChapter(introLabel: String): String =
            if (bookName.isNotEmpty()) {
                if (isViewingIntro) "$bookName $introLabel" else "$bookName $chapterNumber"
            } else {
                ""
            }
    }

    // ----- Actions
    sealed interface Action {
        data object OpenFontSettings : Action

        data object CloseFontSettings : Action

        /** Raise the search sheet over the reader. */
        data object OpenSearch : Action

        /** The search sheet is down; focus whatever result it sent the reader to, if any. */
        data object CloseSearch : Action

        data object DecreaseFontSize : Action

        data object IncreaseFontSize : Action

        data object CycleLineSpacing : Action

        data class SetFontDefinition(
            val fontDefinition: FontDefinition,
        ) : Action

        data class OpenFootnotes(
            val reference: BibleReference,
            val footnotes: List<AnnotatedString>,
        ) : Action

        data object CloseFootnotes : Action

        data class OpenIntroFootnotes(
            val footnotes: List<AnnotatedString>,
        ) : Action

        data object CloseIntroFootnotes : Action

        data class SetReaderTheme(
            val readerTheme: ReaderTheme,
        ) : Action

        data object GoToNextChapter : Action

        data object GoToPreviousChapter : Action

        data class OnVerseTap(
            val reference: BibleReference,
        ) : Action

        /** Stage a reference the reader should arrive scrolled to, now or as soon as its chapter has loaded. */
        data class ScrollToReference(
            val reference: BibleReference,
        ) : Action

        /** The staged scroll target has been scrolled to; clear it so it cannot fire a second time. */
        data object ScrollTargetReached : Action

        /**
         * Point the reader at [reference], dimming the rest of the chapter around it.
         *
         * Ignored unless [reference] names a verse in the chapter on display: a focus names one verse to stand
         * out, and a chapter-wide or off-chapter focus would dim everything with nothing left at full strength.
         * Focus a verse after moving the reader to its chapter, never before.
         */
        data class FocusReference(
            val reference: BibleReference,
        ) : Action

        /** Lift the focus, returning the chapter to full strength. */
        data object ClearFocusedReference : Action

        /**
         * Close search and take the reader to [reference], arriving scrolled to that verse with the rest of its
         * chapter dimmed. The focus is staged until the chapter has loaded, since a verse cannot be focused in a
         * chapter that is not on display yet.
         */
        data class GoToSearchResult(
            val reference: BibleReference,
        ) : Action

        data object ClearVerseSelection : Action

        data object CopySelectedVerses : Action

        data object ShareSelectedVerses : Action

        /**
         * Runs a host app's own verse action: the selection is merged into ranges and cleared, and once
         * [awaitSheetClosed] returns, handed to the action's callback.
         */
        data class PerformCustomVerseAction(
            val verseAction: BibleReaderVerseAction,
            val awaitSheetClosed: suspend () -> Unit = {},
        ) : Action

        data class AddHighlight(
            val hexColor: String,
        ) : Action

        data class RemoveHighlight(
            val hexColor: String,
        ) : Action

        /** Open the notes composer for the selection, signing in first when the reader cannot save notes yet. */
        data object StartNote : Action

        /** Open the composer for [reference] with the saved [notes] attached to it, from a tapped note indicator. */
        data class ShowSavedNotes(
            val reference: BibleReference,
            val notes: List<Note>,
        ) : Action

        /** The sign-in [State.shouldStartNoteSignIn] asked for finished, whether or not it succeeded. */
        data object NoteSignInCompleted : Action

        /** The reader changed the open draft's text, visibility, or color. */
        data class UpdateNote(
            val content: String,
            val isPublic: Boolean,
            val hexColor: String?,
        ) : Action

        data object SaveNote : Action

        /** Load the saved [note] into the composer for editing, replacing any unsaved changes. */
        data class EditSavedNote(
            val note: Note,
        ) : Action

        /** Delete the saved [note] listed below the composer, which the reader has confirmed. */
        data class DeleteSavedNote(
            val note: Note,
        ) : Action

        /** The reader asked to close the composer; unsaved changes are confirmed first. */
        data object RequestDismissNote : Action

        data object ConfirmDiscardNote : Action

        data object CancelDiscardNote : Action

        data object DismissNoteSaveError : Action

        /** A sign-in attempt finished; continue any highlight the reader asked for before signing in. */
        data object SignInCompleted : Action

        /** The reader dismissed the sign-in prompt without signing in. */
        data object CancelSignIn : Action

        /** The reader agreed to grant highlights access; proceed to the grant flow. */
        data object ConfirmDataExchange : Action

        /** The reader declined the highlights permission prompt. */
        data object CancelDataExchange : Action

        /**
         * The grant flow finished.
         *
         * @property isHighlightsGranted Whether the user granted highlights access.
         */
        data class DataExchangeCompleted(
            val isHighlightsGranted: Boolean,
        ) : Action
    }
}
