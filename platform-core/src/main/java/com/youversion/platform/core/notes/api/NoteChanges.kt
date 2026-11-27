package com.youversion.platform.core.notes.api

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Counts the note changes this process has made, so views showing notes or the highlights a note's color sets know
 * to reload them. [NotesApi] reports every create, update and delete that succeeds.
 */
@PlatformInternalApi
public object NoteChanges {
    private val _changeCount = MutableStateFlow(0)

    /** How many note changes have succeeded; it only grows. */
    public val changeCount: StateFlow<Int> = _changeCount.asStateFlow()

    /** Records a note change that succeeded. */
    public fun notifyChanged() {
        _changeCount.update { it + 1 }
    }
}
