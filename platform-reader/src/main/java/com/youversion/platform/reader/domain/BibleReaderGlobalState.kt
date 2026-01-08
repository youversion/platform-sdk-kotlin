package com.youversion.platform.reader.domain

import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BibleReaderGlobalState {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    val value: State
        get() = _state.value

    fun update(function: (State) -> State) {
        _state.update(function)
    }

    // ----- Convenience Accessors
    val bibleVersion: BibleVersion?
        get() = value.bibleVersion
    val bibleVersionLanguageTag: String
        get() = value.bibleVersion?.languageTag ?: "en"

    val permittedVersions: List<BibleVersion>
        get() = value.permittedVersions

    // ----- State
    data class State(
        val bibleVersion: BibleVersion? = null,
        val permittedVersions: List<BibleVersion> = emptyList(),
        val chosenLanguageTag: String? = null,
    ) {
        val activeLanguageTag: String
            get() = chosenLanguageTag ?: bibleVersion?.languageTag ?: "en"
    }
}
