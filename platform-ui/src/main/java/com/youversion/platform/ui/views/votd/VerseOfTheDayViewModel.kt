package com.youversion.platform.ui.views.votd

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

internal class VerseOfTheDayViewModel private constructor(
    private val bibleVersionId: Int,
    private val bibleVersionRepository: BibleVersionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val verseOfTheDay = YouVersionApi.votd.verseOfTheDay(dayOfYear)
                val bibleVersion = bibleVersionRepository.version(bibleVersionId)
                val reference = bibleVersion.reference(verseOfTheDay.passageUsfm)

                if (reference != null) {
                    _state.update { it.copy(bibleReference = reference, bibleVersion = bibleVersion) }
                } else {
                    _events.send(Event.OnErrorLoadingVerseOfTheDay)
                    Log.e("VerseOfTheDay", "Error Invalid reference")
                }
            } catch (e: Exception) {
                _events.send(Event.OnErrorLoadingVerseOfTheDay)
                Log.e("VerseOfTheDay", "Error loading VOTD", e)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ----- State
    data class State(
        val isLoading: Boolean = true,
        val bibleReference: BibleReference? = null,
        val bibleVersion: BibleVersion? = null,
        val showIcon: Boolean = true,
    )

    // ----- Events
    sealed interface Event {
        data object OnErrorLoadingVerseOfTheDay : Event
    }

    // ----- Actions
    sealed interface Action

    // ----- Injection
    companion object {
        fun factory(
            context: Context,
            bibleVersionId: Int,
        ): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        VerseOfTheDayViewModel(
                            bibleVersionId = bibleVersionId,
                            bibleVersionRepository = BibleVersionRepository(context),
                        )
                    }
                }.build()
    }
}
