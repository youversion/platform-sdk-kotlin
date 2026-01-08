package com.youversion.platform.reader.screens.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.reader.domain.BibleReaderGlobalState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class LanguagesViewModel(
    private val languageRepository: LanguageRepository,
    private val globalState: BibleReaderGlobalState,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state by lazy { _state.asStateFlow() }

    val countryCode by lazy { Locale.getDefault().country }
    val languageCode by lazy { Locale.getDefault().language }

    init {
        loadSuggestedLanguages()
    }

    private fun loadSuggestedLanguages() {
        viewModelScope.launch {
            try {
                val permittedLanguageTags =
                    globalState.permittedVersions
                        .mapNotNull { it.languageTag }

                val permittedLanguages =
                    languageRepository
                        .suggestedLanguages(country = countryCode)
                        .filter { it.language in permittedLanguageTags }
                        .map { LanguageRowItem(it, languageCode) }
                _state.update { it.copy(suggestedLanguages = permittedLanguages) }
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
        val initializing: Boolean = true,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
    )

    // ----- Events
    sealed interface Event

    // ----- Actions
    sealed interface Action
}
