package com.youversion.platform.reader.screens.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LanguagesViewModel(
    bibleVersion: BibleVersion?,
    private val bibleReaderRepository: BibleReaderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state by lazy { _state.asStateFlow() }

    init {
        loadLanguages(bibleVersion)
    }

    private fun loadLanguages(bibleVersion: BibleVersion?) {
        viewModelScope.launch {
            try {
                bibleReaderRepository.loadLanguageNames(bibleVersion)

                val allPermittedLanguageTags = bibleReaderRepository.allPermittedLanguageTags
                val allLanguages =
                    allPermittedLanguageTags
                        .map { tag ->
                            LanguageRowItem(
                                languageTag = tag,
                                displayName = bibleReaderRepository.languageName(tag),
                                localeDisplayName = null,
                            )
                        }

                val suggestedLanguageTags = bibleReaderRepository.suggestedLanguageTags()
                val suggestedLanguages =
                    suggestedLanguageTags
                        .map { tag ->
                            LanguageRowItem(
                                languageTag = tag,
                                displayName = bibleReaderRepository.languageName(tag),
                                localeDisplayName = null,
                            )
                        }

                _state.update {
                    it.copy(
                        suggestedLanguages = suggestedLanguages,
                        allLanguages = allLanguages,
                    )
                }
            } catch (e: Exception) {
                Logger.e("Failed to get languages", e)
            } finally {
                _state.update { it.copy(initializing = false) }
            }
        }
    }

    // ----- State
    data class State(
        val initializing: Boolean = true,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
        val allLanguages: List<LanguageRowItem> = emptyList(),
    )

    // ----- Events
    sealed interface Event

    // ----- Actions
    sealed interface Action
}
