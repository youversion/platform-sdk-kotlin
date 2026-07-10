package com.youversion.platform.core.highlights.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A read-only [StateFlow] that projects [source] through [transform]. Unlike `source.map { }.stateIn(...)`, its [value]
 * is computed synchronously on read rather than by a collecting coroutine, so it reflects the source the instant the
 * source changes. This lets a layer expose a derived view without leaking the underlying rows, while keeping the
 * immediate-read semantics callers rely on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MappedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {
    override val value: R
        get() = transform(source.value)

    override val replayCache: List<R>
        get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        // Conflate like a real StateFlow: two distinct source values that project to equal results must not be
        // re-emitted, or collectors of a projection that drops fields (e.g. mapping a cached row to just its highlight)
        // would see spurious duplicates the source's own conflation cannot catch.
        source.map(transform).distinctUntilChanged().collect(collector)
        awaitCancellation()
    }
}
