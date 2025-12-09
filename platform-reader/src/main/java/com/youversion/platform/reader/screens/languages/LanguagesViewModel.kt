package com.youversion.platform.reader.screens.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.languages.domain.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class LanguagesViewModel(
    private val permittedVersions: List<BibleVersion>,
    private val languageRepository: LanguageRepository,
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
                val languages =
                    languageRepository
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
        val initializing: Boolean = true,
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
    )

    // ----- Events
    sealed interface Event

    // ----- Actions
    sealed interface Action

    // ----- Injection
    companion object {
        fun factory(permittedVersions: List<BibleVersion>): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        LanguagesViewModel(
                            permittedVersions = permittedVersions,
                            languageRepository = LanguageRepository(),
                        )
                    }
                }.build()
    }
}
