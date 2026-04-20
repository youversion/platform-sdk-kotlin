package com.youversion.platform.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.CopyManager
import com.youversion.platform.reader.domain.ShareManager
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.reader.theme.ReaderTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import com.youversion.platform.ui.views.components.LanguageRowItem
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BibleReaderViewModel(
    bibleReference: BibleReference?,
    private val fontDefinitionProvider: FontDefinitionProvider?,
    private val bibleVersionRepository: BibleVersionRepository,
    private val bibleReaderRepository: BibleReaderRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val bibleChapterRepository: BibleChapterRepository,
    private val languageRepository: LanguageRepository,
    private val copyManager: CopyManager,
    private val shareManager: ShareManager,
) : ViewModel() {
    private val _state: MutableStateFlow<State>
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    internal var bibleReference: BibleReference
        get() = _state.value.bibleReference
        set(value) {
            bibleReaderRepository.lastBibleReference = value
            _state.update { it.copy(bibleReference = value) }
        }
    internal var bibleVersion: BibleVersion?
        get() = _state.value.bibleVersion
        set(value) = _state.update { it.copy(bibleVersion = value) }

    init {
        val reference = bibleReaderRepository.produceBibleReference(bibleReference)
        this._state =
            MutableStateFlow(
                State(
                    bibleReference = reference,
                    providedFontDefinitions = fontDefinitionProvider?.fonts() ?: listOf(),
                ),
            )

        loadUserSettingsFromStorage()
        loadVersionIfNeeded()
        loadLanguages()
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
    }

    private fun loadVersionIfNeeded() {
        if (bibleVersion == null || bibleVersion?.id != bibleReference.versionId) {
            viewModelScope.launch {
                try {
                    bibleVersion = bibleVersionRepository.version(id = bibleReference.versionId)
                } catch (e: Exception) {
                    // TODO: Select fallback version error
                }
            }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.OpenFontSettings -> {
                _state.update { it.copy(showingFontList = true) }
            }

            is Action.CloseFontSettings -> {
                _state.update { it.copy(showingFontList = false) }
            }

            is Action.DecreaseFontSize -> {
                decreaseFontSize()
            }

            is Action.IncreaseFontSize -> {
                increaseFontSize()
            }

            is Action.SetFontDefinition -> {
                setFontFamily(action)
            }

            is Action.OpenFootnotes -> {
                openFootnotes(action)
            }

            is Action.CloseFootnotes -> {
                closeFootnotes()
            }

            is Action.OpenIntroFootnotes -> {
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
                            bibleReference.copy(
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
                toggleVerseSelection(action.reference)
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
        }
    }

    fun switchToVersion(versionId: Int) {
        val newReference = bibleReference.copy(versionId = versionId)
        onHeaderSelectionChange(newReference)
    }

    fun onIntroSelected(
        bookUSFM: String,
        passageId: String,
    ) {
        _state.update { it.copy(introBookUSFM = bookUSFM, introPassageId = passageId) }
        bibleReference = bibleReference.copy(bookUSFM = bookUSFM, chapter = 1)
    }

    fun onHeaderSelectionChange(newReference: BibleReference) {
        clearVerseSelection()
        viewModelScope.launch {
            if (bibleVersion?.id != newReference.versionId) {
                val newVersion = bibleVersionRepository.version(id = newReference.versionId)
                bibleVersion = newVersion
                // TODO: INsert my version
            }
            _state.update { it.copy(introBookUSFM = null, introPassageId = null) }
            bibleReference = newReference
        }
    }

    private fun toggleVerseSelection(reference: BibleReference) {
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
        _state.update {
            it.copy(
                selectedVerses = emptySet(),
                showVerseActionSheet = false,
            )
        }
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
        val defaultFontDefinitions: List<FontDefinition> = ReaderFontSettings.defaultFontDefinitions,
        val providedFontDefinitions: List<FontDefinition> = listOf(),
        val selectedFontDefinition: FontDefinition = ReaderFontSettings.DEFAULT_FONT_DEFINITION,
        val fontSize: TextUnit = ReaderFontSettings.DEFAULT_FONT_SIZE,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
        val showingFootnotes: Boolean = false,
        val footnotesReference: BibleReference? = null,
        val footnotes: List<AnnotatedString> = emptyList(),
        val selectedVerses: Set<BibleReference> = emptySet(),
        val showVerseActionSheet: Boolean = false,
        val showingIntroFootnotes: Boolean = false,
        val introFootnotes: List<AnnotatedString> = emptyList(),
        val introBookUSFM: String? = null,
        val introPassageId: String? = null,
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

        val bookAndChapter: String
            get() =
                if (bookName.isNotEmpty()) {
                    if (isViewingIntro) "$bookName Intro" else "$bookName $chapterNumber"
                } else {
                    ""
                }

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
    }

    // ----- Actions
    sealed interface Action {
        data object OpenFontSettings : Action

        data object CloseFontSettings : Action

        data object DecreaseFontSize : Action

        data object IncreaseFontSize : Action

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

        data object ClearVerseSelection : Action

        data object CopySelectedVerses : Action

        data object ShareSelectedVerses : Action
    }
}
