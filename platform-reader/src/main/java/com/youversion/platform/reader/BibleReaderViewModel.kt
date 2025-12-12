package com.youversion.platform.reader

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.utilities.dependencies.SharedPreferencesStore
import com.youversion.platform.core.utilities.dependencies.Store
import com.youversion.platform.reader.theme.BibleReaderTheme
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.reader.theme.ReaderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BibleReaderViewModel(
    bibleReference: BibleReference?,
    private val fontDefinitionProvider: FontDefinitionProvider?,
    private val bibleVersionRepository: BibleVersionRepository,
    private val store: Store,
) : ViewModel() {
    private val _state: MutableStateFlow<State>
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    internal var bibleReference: BibleReference
        get() = _state.value.bibleReference
        set(value) {
            store.bibleReference = value
            _state.update { it.copy(bibleReference = value) }
        }
    internal var bibleVersion: BibleVersion?
        get() = _state.value.bibleVersion
        set(value) = _state.update { it.copy(bibleVersion = value) }

    init {
        val myVersionIds: Set<Int>? = store.myVersionIds

        val reference =
            if (bibleReference != null) {
                bibleReference
            } else {
                val savedReference = store.bibleReference
                if (savedReference != null) {
                    savedReference
                } else {
                    // No specified or saved version so pick a downloaded one. If none
                    // have been downloaded, then use the default version.
                    val downloadedVersions = bibleVersionRepository.downloadedVersions
                    val versionId =
                        downloadedVersions.firstOrNull()
                            ?: myVersionIds?.firstOrNull()
                            ?: 111 // NIV
                    BibleReference(versionId = versionId, bookUSFM = "JHN", chapter = 1)
                }
            }

        this._state =
            MutableStateFlow(
                State(
                    bibleReference = reference,
                    providedFontDefinitions = fontDefinitionProvider?.fonts() ?: listOf(),
                ),
            )

        loadUserSettingsFromStorage()
        loadVersionIfNeeded(myVersionIds ?: emptySet())
    }

    private fun loadUserSettingsFromStorage() {
        // Restore Theme
        val savedReaderThemeId = store.readerThemeId
        val savedReaderTheme = ReaderTheme.themeById(savedReaderThemeId)
        BibleReaderTheme.selectedColorScheme.value = savedReaderTheme.colorScheme

        // Restore Font
        val savedFontDefinitionName = store.readerFontFamilyName
        val allFontDefinitions = _state.value.allFontDefinitions
        allFontDefinitions.find { it.fontName == savedFontDefinitionName }?.let { savedFontDefinition ->
            _state.update { it.copy(selectedFontDefinition = savedFontDefinition) }
        }

        // Restore Line Spacing
        val savedLineSpacing = store.readerLineSpacing
        if (savedLineSpacing != null && savedLineSpacing > -1f) {
            _state.update { it.copy(lineSpacingMultiplier = savedLineSpacing) }
        }

        // Restore Font Size
        val savedFontSize = store.readerFontSize
        if (savedFontSize != null && savedFontSize > -1f) {
            _state.update { it.copy(fontSize = savedFontSize.sp) }
        }
    }

    private fun loadVersionIfNeeded(mySavedVersionIds: Set<Int>) {
        if (bibleVersion == null || bibleVersion?.id != bibleReference.versionId) {
            viewModelScope.launch {
                try {
                    bibleVersion = bibleVersionRepository.version(id = bibleReference.versionId)
                    bibleVersion?.let {
                        // TODO: Add to saved versions
                    }
                } catch (e: Exception) {
                    // TODO: Select fallback version error
                }
            }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.OpenFontSettings -> _state.update { it.copy(showingFontList = true) }
            is Action.CloseFontSettings -> _state.update { it.copy(showingFontList = false) }
            is Action.DecreaseFontSize -> decreaseFontSize()
            is Action.IncreaseFontSize -> increaseFontSize()
            is Action.NextLineSpacingMultiplierOption -> nextLineSpacingMultiplierOption()
            is Action.SetFontDefinition -> setFontFamily(action)
            is Action.SetReaderTheme -> setReaderTheme(action)
        }
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
        store.readerFontSize = size.value
        _state.update { it.copy(fontSize = size) }
    }

    fun nextLineSpacingMultiplierOption() {
        val currentLineSpacing = _state.value.lineSpacingMultiplier
        val nextLineSpacing = ReaderFontSettings.nextLineSpacingMultiplier(currentLineSpacing)
        store.readerLineSpacing = nextLineSpacing
        _state.update { it.copy(lineSpacingMultiplier = nextLineSpacing) }
    }

    fun setFontFamily(action: Action.SetFontDefinition) {
        store.readerFontFamilyName = action.fontDefinition.fontName
        _state.update { it.copy(selectedFontDefinition = action.fontDefinition) }
    }

    fun setReaderTheme(action: Action.SetReaderTheme) {
        BibleReaderTheme.selectedColorScheme.value = action.readerTheme.colorScheme
        store.readerThemeId = action.readerTheme.id
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

        data class SetReaderTheme(
            val readerTheme: ReaderTheme,
        ) : Action
    }

    // ----- Injection
    companion object {
        fun factory(
            context: Context,
            bibleReference: BibleReference?,
            fontDefinitionProvider: FontDefinitionProvider?,
        ): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        BibleReaderViewModel(
                            bibleReference = bibleReference,
                            fontDefinitionProvider = fontDefinitionProvider,
                            bibleVersionRepository = BibleVersionRepository(context),
                            store = SharedPreferencesStore(context),
                        )
                    }
                }.build()
    }
}
