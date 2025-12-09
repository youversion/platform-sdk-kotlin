package com.youversion.platform.reader

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.utilities.dependencies.SharedPreferencesStore
import com.youversion.platform.core.utilities.dependencies.Store
import com.youversion.platform.reader.screens.languages.LanguageRowItem
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.reader.theme.UntitledSerif
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
    private val languagesRepository: LanguageRepository,
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

        loadVersionIfNeeded(myVersionIds ?: emptySet())
        loadSuggestedLanguages()
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
        _state.update {
            it.copy(
                fontSize = ReaderFontSettings.nextSmallerFontSize(it.fontSize),
            )
        }
    }

    fun increaseFontSize() {
        _state.update {
            it.copy(
                fontSize = ReaderFontSettings.nextLargerFontSize(it.fontSize),
            )
        }
    }

    fun nextLineSpacingMultiplierOption() {
        _state.update {
            it.copy(
                lineSpacingMultiplier =
                    ReaderFontSettings.nextLineSpacingMultiplier(
                        it.lineSpacingMultiplier,
                    ),
            )
        }
    }

    fun setFontFamily(action: Action.SetFontDefinition) {
        _state.update { it.copy(selectedFontDefinition = action.fontDefinition) }
    }

    // ----- Languages
    val countryCode: String by lazy { Locale.getDefault().country }
    val languageCode: String by lazy { Locale.getDefault().language }

    private fun loadSuggestedLanguages() {
        viewModelScope.launch {
            try {
                val languages =
                    languagesRepository
                        .suggestedLanguages(country = countryCode)
                        .map { LanguageRowItem(it, languageCode) }
                _state.update { it.copy(suggestedLanguages = languages) }
            } catch (e: Exception) {
                Logger.w("Failed to get languages", e)
            }
        }
    }

    private fun extractLanguageCodes(languages: List<LanguageRowItem>): Set<String> =
        languages
            .map { it.language.id }
            .toSet()

    // ----- State
    data class State(
        val bibleReference: BibleReference,
        val bibleVersion: BibleVersion? = null,
        val showCopyright: Boolean = false,
        val showingFontList: Boolean = false,
        val defaultFontDefinitions: List<FontDefinition> =
            listOf(
                FontDefinition("Untitled Serif", UntitledSerif),
                FontDefinition("Serif", FontFamily.Serif),
                FontDefinition("System Default", FontFamily.Default),
                FontDefinition("Cursive", FontFamily.Cursive),
                FontDefinition("Sans Serif", FontFamily.SansSerif),
                FontDefinition("Monospace", FontFamily.Monospace),
            ),
        val providedFontDefinitions: List<FontDefinition> = listOf(),
        val selectedFontDefinition: FontDefinition = ReaderFontSettings.DEFAULT_FONT_DEFINITION,
        val fontSize: TextUnit = ReaderFontSettings.DEFAULT_FONT_SIZE,
        val lineSpacingMultiplier: Float = ReaderFontSettings.DEFAULT_LINE_SPACING_MULTIPLIER,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
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
                            languagesRepository = LanguageRepository(),
                            store = SharedPreferencesStore(context),
                        )
                    }
                }.build()
    }
}

fun BibleReaderViewModel.loadVersionsList() {
}
