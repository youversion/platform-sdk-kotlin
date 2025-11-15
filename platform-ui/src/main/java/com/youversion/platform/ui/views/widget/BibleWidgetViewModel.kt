package com.youversion.platform.ui.views.widget

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class BibleWidgetViewModel private constructor(
    private val reference: BibleReference,
    bibleVersion: BibleVersion?,
    private val bibleVersionRepository: BibleVersionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State(bibleVersion))
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
                    val bibleVersion = bibleVersionRepository.version(id = reference.versionId)
                    _state.update { it.copy(bibleVersion = bibleVersion) }
                } catch (e: Exception) {
                    _events.send(Event.OnErrorLoadingBibleVersion)
                    Log.e("BibleWidget", "Error loading Bible version", e)
                }
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
        val bibleVersion: BibleVersion?,
        val showCopyright: Boolean = false,
    )

    // ----- Events
    sealed interface Event {
        data object OnErrorLoadingBibleVersion : Event
    }

    // ----- Actions
    sealed interface Action {
        data object OnViewCopyright : Action

        data object OnCloseCopyright : Action
    }

    // ----- Injection
    companion object {
        fun factory(
            context: Context,
            reference: BibleReference,
            bibleVersion: BibleVersion?,
        ): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        BibleWidgetViewModel(
                            reference,
                            bibleVersion,
                            BibleVersionRepository(context),
                        )
                    }
                }.build()
    }
}
