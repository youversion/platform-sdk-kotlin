package com.youversion.platform.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.screens.languages.LanguageRowItem
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.reader.theme.ReaderTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class BibleReaderViewModel(
    bibleReference: BibleReference?,
    private val fontDefinitionProvider: FontDefinitionProvider?,
    private val bibleVersionRepository: BibleVersionRepository,
    private val bibleReaderRepository: BibleReaderRepository,
    private val userSettingsRepository: UserSettingsRepository,
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
        loadSuggestedLanguages()
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

    // ----- Languages
    val localeCountryCode: String
        get() = Locale.getDefault().country ?: "US"
    val localeLanguageCode: String
        get() = Locale.getDefault().language ?: "en"

    private fun loadSuggestedLanguages() {
        viewModelScope.launch {
            try {
//                val languages =
//                    languagesRepository
//                        .suggestedLanguages(country = localeCountryCode)
//                        .map { LanguageRowItem(it, localeLanguageCode) }
//                _state.update { it.copy(suggestedLanguages = languages) }
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
    }
}
