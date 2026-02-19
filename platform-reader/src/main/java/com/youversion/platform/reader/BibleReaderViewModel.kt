package com.youversion.platform.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.screens.languages.LanguageRowItem
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.reader.theme.ReaderTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
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

        // Restore Line Spacing
        userSettingsRepository.readerLineSpacing?.let { savedLineSpacing ->
            _state.update { it.copy(lineSpacingMultiplier = savedLineSpacing) }
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

            is Action.NextLineSpacingMultiplierOption -> {
                nextLineSpacingMultiplierOption()
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

            is Action.SetReaderTheme -> {
                setReaderTheme(action)
            }

            is Action.GoToNextChapter -> {
                bibleReaderRepository
                    .nextChapter(bibleVersion, bibleReference)
                    ?.let { nextReference -> bibleReference = nextReference }
            }

            is Action.GoToPreviousChapter -> {
                bibleReaderRepository
                    .previousChapter(bibleVersion, bibleReference)
                    ?.let { prevReference -> bibleReference = prevReference }
            }

            is Action.UpdateVerseSelection -> {
                _state.update {
                    it.copy(
                        selectedVerses = action.selectedVerses,
                        isShowingVerseActions = action.selectedVerses.isNotEmpty(),
                    )
                }
            }

            is Action.OpenVerseActions -> {
                _state.update { it.copy(isShowingVerseActions = true) }
            }

            is Action.CloseVerseActions -> {
                _state.update { it.copy(isShowingVerseActions = false) }
            }

            is Action.ClearVerseSelection -> {
                _state.update {
                    it.copy(selectedVerses = emptySet(), isShowingVerseActions = false)
                }
            }

            is Action.CopySelectedVerses -> {
                copySelectedVerses(action.clipboardManager)
            }

            is Action.ShareSelectedVerses -> {
                shareSelectedVerses(action.context)
            }
        }
    }

    fun switchToVersion(versionId: Int) {
        val newReference = bibleReference.copy(versionId = versionId)
        onHeaderSelectionChange(newReference)
    }

    fun onHeaderSelectionChange(newReference: BibleReference) {
        viewModelScope.launch {
            if (bibleVersion?.id != newReference.versionId) {
                val newVersion = bibleVersionRepository.version(id = newReference.versionId)
                bibleVersion = newVersion
                // TODO: INsert my version
            }
            bibleReference = newReference
        }
    }

    fun decreaseFontSize() {
        val currentFontSize = _state.value.fontSize
        val nextFontSize = ReaderFontSettings.nextSmallerFontSize(currentFontSize)
        setFontSize(nextFontSize)
    }

    fun increaseFontSize() {
        val currentFontSize = _state.value.fontSize
        val nextFontSize = ReaderFontSettings.nextLargerFontSize(currentFontSize)
        setFontSize(nextFontSize)
    }

    private fun setFontSize(size: TextUnit) {
        userSettingsRepository.readerFontSize = size.value
        _state.update { it.copy(fontSize = size) }
    }

    fun nextLineSpacingMultiplierOption() {
        val currentLineSpacing = _state.value.lineSpacingMultiplier
        val nextLineSpacing = ReaderFontSettings.nextLineSpacingMultiplier(currentLineSpacing)
        userSettingsRepository.readerLineSpacing = nextLineSpacing
        _state.update { it.copy(lineSpacingMultiplier = nextLineSpacing) }
    }

    fun setFontFamily(action: Action.SetFontDefinition) {
        userSettingsRepository.readerFontFamilyName = action.fontDefinition.fontName
        _state.update { it.copy(selectedFontDefinition = action.fontDefinition) }
    }

    fun openFootnotes(action: Action.OpenFootnotes) {
        _state.update {
            it.copy(
                showingFootnotes = true,
                footnotesReference = action.reference,
                footnotes = action.footnotes,
            )
        }
    }

    fun closeFootnotes() {
        _state.update {
            it.copy(
                showingFootnotes = false,
                footnotesReference = null,
                footnotes = emptyList(),
            )
        }
    }

    fun setReaderTheme(action: Action.SetReaderTheme) {
        BibleReaderTheme.selectedColorScheme.value = action.readerTheme.colorScheme
        userSettingsRepository.readerThemeId = action.readerTheme.id
    }

    // ----- Verse Actions

    private fun copySelectedVerses(clipboardManager: ClipboardManager) {
        viewModelScope.launch {
            val clipboardText = formattedSelectedVerseText()
            if (clipboardText != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("verse", clipboardText))
            }
            onAction(Action.ClearVerseSelection)
        }
    }

    private fun shareSelectedVerses(context: Context) {
        viewModelScope.launch {
            val version = _state.value.bibleVersion
            val mergedReferences = mergedSelectedReferences()
            val title =
                mergedReferences.joinToString(", ") { ref ->
                    version?.displayTitle(ref, includesVersionAbbreviation = true) ?: ""
                }
            val shareUrl = version?.shareUrl(mergedReferences.first())
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareUrl ?: "")
                    putExtra(Intent.EXTRA_TITLE, title)
                }
            context.startActivity(Intent.createChooser(intent, null))
            onAction(Action.ClearVerseSelection)
        }
    }

    private suspend fun formattedSelectedVerseText(): String? {
        val version = _state.value.bibleVersion
        val mergedReferences = mergedSelectedReferences()
        val textParts =
            mergedReferences.map { ref ->
                BibleVersionRendering.plainTextOf(bibleChapterRepository, ref) ?: ""
            }
        val verseText = textParts.joinToString("\n")
        val title =
            mergedReferences.joinToString(", ") { ref ->
                version?.displayTitle(ref, includesVersionAbbreviation = true) ?: ""
            }
        val shareUrl = version?.shareUrl(mergedReferences.first())
        return buildString {
            append(verseText)
            append("\n")
            append(title)
            if (shareUrl != null) {
                append("\n")
                append(shareUrl)
            }
        }
    }

    private fun mergedSelectedReferences(): List<BibleReference> {
        val references =
            _state.value.selectedVerses
                .toList()
                .sorted()
        return BibleReference.referencesByMerging(references)
    }

    // ----- Languages
    private fun loadLanguages() {
        viewModelScope.launch {
            try {
                bibleReaderRepository.loadLanguageNames(bibleVersion)
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
        val lineSpacingMultiplier: Float = ReaderFontSettings.DEFAULT_LINE_SPACING_MULTIPLIER,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
        val showingFootnotes: Boolean = false,
        val footnotesReference: BibleReference? = null,
        val footnotes: List<AnnotatedString> = emptyList(),
        val selectedVerses: Set<BibleReference> = emptySet(),
        val isShowingVerseActions: Boolean = false,
    ) {
        val bookAndChapter: String
            get() =
                bibleVersion?.let { version ->
                    val bookUsfm = bibleReference.bookUSFM
                    val book = version.bookName(bookUsfm) ?: bookUsfm
                    val chapter = bibleReference.chapter

                    "$book $chapter"
                } ?: ""

        val versionAbbreviation: String
            get() =
                bibleVersion?.let { version ->
                    version.localizedAbbreviation
                        ?: version.abbreviation
                        ?: version.id.toString()
                } ?: ""

        val lineSpacingSettingsIndex: Int
            get() =
                ReaderFontSettings.getLineSpacingSettingIndex(
                    lineSpacingMultiplier,
                )

        val lineSpacing: TextUnit
            get() = fontSize * lineSpacingMultiplier

        val fontFamily: FontFamily
            get() = selectedFontDefinition.fontFamily

        val allFontDefinitions: List<FontDefinition>
            get() = defaultFontDefinitions + providedFontDefinitions
    }

    // ----- Events
    sealed interface Event {
        data object OnErrorLoadingBibleVersion : Event
    }

    // ----- Actions
    sealed interface Action {
        data object OpenFontSettings : Action

        data object CloseFontSettings : Action

        data object DecreaseFontSize : Action

        data object IncreaseFontSize : Action

        data object NextLineSpacingMultiplierOption : Action

        data class SetFontDefinition(
            val fontDefinition: FontDefinition,
        ) : Action

        data class OpenFootnotes(
            val reference: BibleReference,
            val footnotes: List<AnnotatedString>,
        ) : Action

        data object CloseFootnotes : Action

        data class SetReaderTheme(
            val readerTheme: ReaderTheme,
        ) : Action

        data object GoToNextChapter : Action

        data object GoToPreviousChapter : Action

        data class UpdateVerseSelection(
            val selectedVerses: Set<BibleReference>,
        ) : Action

        data object OpenVerseActions : Action

        data object CloseVerseActions : Action

        data class CopySelectedVerses(
            val clipboardManager: ClipboardManager,
        ) : Action

        data class ShareSelectedVerses(
            val context: Context,
        ) : Action

        data object ClearVerseSelection : Action
    }
}
