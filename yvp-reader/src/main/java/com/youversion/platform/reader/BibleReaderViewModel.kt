package com.youversion.platform.reader

import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BibleReaderViewModel(
    bibleReference: BibleReference?,
    private val bibleVersionRepository: BibleVersionRepository,
    private val store: Store,
) : ViewModel() {
    private val _state: MutableStateFlow<State>
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    internal var bibleReference: BibleReference
        get() = _state.value.bibleReference
        set(value) = _state.update { it.copy(bibleReference = value) }
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

        this._state = MutableStateFlow(State(bibleReference = reference))

        loadVersionIfNeeded(myVersionIds ?: emptySet())
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

    // ----- State
    data class State(
        val bibleReference: BibleReference,
        val bibleVersion: BibleVersion? = null,
        val showCopyright: Boolean = false,
        val showingFontList: Boolean = false,
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
    }

    // ----- Events
    sealed interface Event {
        data object OnErrorLoadingBibleVersion : Event
    }

    // ----- Actions
    sealed interface Action {
        data object OpenFontSettings : Action

        data object CloseFontSettings : Action
    }

    // ----- Injection
    companion object {
        fun factory(
            context: Context,
            bibleReference: BibleReference?,
        ): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        BibleReaderViewModel(
                            bibleReference = bibleReference,
                            bibleVersionRepository = BibleVersionRepository(context),
                            store = SharedPreferencesStore(context),
                        )
                    }
                }.build()
    }
}
