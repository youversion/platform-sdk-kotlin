package com.youversion.platform.ui.views.card

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class BibleCardViewModel(
    reference: BibleReference,
    bibleVersion: BibleVersion?,
    private val bibleVersionRepository: BibleVersionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State(reference = reference, bibleVersion = bibleVersion))
    val state = _state.asStateFlow()

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    // ViewModel Behavior
    init {
        loadMissingBibleVersion()
    }

    private fun loadMissingBibleVersion() {
        if (_state.value.bibleVersion == null) {
            viewModelScope.launch {
                try {
                    val bibleVersion = bibleVersionRepository.version(id = _state.value.reference.versionId)
                    _state.update { it.copy(bibleVersion = bibleVersion) }
                } catch (e: Exception) {
                    _events.send(Event.OnErrorLoadingBibleVersion)
                    Log.e("BibleCard", "Error loading Bible version", e)
                }
            }
        }
    }

    fun switchToVersion(versionId: Int) {
        viewModelScope.launch {
            try {
                val newVersion = bibleVersionRepository.version(versionId)
                _state.update {
                    it.copy(
                        reference = it.reference.copy(versionId = versionId),
                        bibleVersion = newVersion,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(Event.OnErrorLoadingBibleVersion)
                Log.e("BibleCard", "Error loading Bible version", e)
            }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.OnViewCopyright -> _state.update { it.copy(showCopyright = true) }
            is Action.OnCloseCopyright -> _state.update { it.copy(showCopyright = false) }
        }
    }

    // ----- State
    data class State(
        val reference: BibleReference,
        val bibleVersion: BibleVersion?,
        val showCopyright: Boolean = false,
    ) {
        val isReferenceUnavailable: Boolean
            get() = bibleVersion?.let { !reference.existsIn(it) } ?: false
    }

    // ----- Events
    sealed interface Event {
        data object OnErrorLoadingBibleVersion : Event
    }

    // ----- Actions
    sealed interface Action {
        data object OnViewCopyright : Action

        data object OnCloseCopyright : Action
    }
}
