package com.youversion.platform.reader

import kotlin.test.Test
import kotlin.test.assertEquals

class BibleReaderSearchViewModelTest {
    private val viewModel = BibleReaderSearchViewModel()

    @Test
    fun `the query starts empty`() {
        assertEquals("", viewModel.state.value.query)
    }

    @Test
    fun `SetQuery records what was typed`() {
        viewModel.onAction(BibleReaderSearchViewModel.Action.SetQuery("Corinthians"))

        assertEquals("Corinthians", viewModel.state.value.query)
    }

    @Test
    fun `OpenSearch empties the query left behind by the last one`() {
        viewModel.onAction(BibleReaderSearchViewModel.Action.SetQuery("Corinthians"))

        viewModel.onAction(BibleReaderSearchViewModel.Action.OpenSearch)

        assertEquals("", viewModel.state.value.query)
    }
}
