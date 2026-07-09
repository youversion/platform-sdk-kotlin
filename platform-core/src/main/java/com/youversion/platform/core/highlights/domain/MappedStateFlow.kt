package com.youversion.platform.core.highlights.domain

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * A read-only [StateFlow] that projects [source] through [transform]. Unlike `source.map { }.stateIn(...)`, its [value]
 * is computed synchronously on read rather than by a collecting coroutine, so it reflects the source the instant the
 * source changes. This lets a layer expose a derived view without leaking the underlying rows, while keeping the
 * immediate-read semantics callers rely on.
 */
internal class MappedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {
    override val value: R
        get() = transform(source.value)

    override val replayCache: List<R>
        get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing =
        source.collect { collector.emit(transform(it)) }
}
