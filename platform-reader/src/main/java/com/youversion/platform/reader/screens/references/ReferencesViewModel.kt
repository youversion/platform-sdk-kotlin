package com.youversion.platform.reader.screens.references

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReferencesViewModel(
    private val bibleVersion: BibleVersion,
    bibleReference: BibleReference,
) : ViewModel() {
    private val _state: MutableStateFlow<State>
    val state by lazy { _state.asStateFlow() }

    init {
        val rows =
            bibleVersion.bookCodes
                ?.map { bookUsfm ->
                    ReferenceRow(
                        bookCode = bookUsfm,
                        bookName = bibleVersion.bookName(bookUsfm),
                        chapters = bibleVersion.chapterLabels(bookUsfm),
                    )
                } ?: emptyList()

        val initialState =
            State(
                referenceRows = rows,
                expandedBookCode = bibleReference.bookUSFM,
            )
        _state = MutableStateFlow(initialState)
    }

    fun expandBook(bookCode: String) {
        val expanded = if (_state.value.expandedBookCode == bookCode) null else bookCode
        _state.update { it.copy(expandedBookCode = expanded) }
    }

    // ----- State
    data class State(
        val referenceRows: List<ReferenceRow>,
        val expandedBookCode: String?,
    )

    // ----- Injection
    companion object {
        fun factory(
            bibleVersion: BibleVersion,
            bibleReference: BibleReference,
        ): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        ReferencesViewModel(
                            bibleVersion = bibleVersion,
                            bibleReference = bibleReference,
                        )
                    }
                }.build()
    }
}
