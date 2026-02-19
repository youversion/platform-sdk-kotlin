package com.youversion.platform.reader.screens.references

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.R
import com.youversion.platform.reader.components.BibleReaderTopAppBar
import com.youversion.platform.reader.theme.readerColorScheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ReferencesScreen(
    bibleVersion: BibleVersion,
    bibleReference: BibleReference,
    onSelectionClick: (Int, String, String) -> Unit,
    onBackClick: () -> Unit,
) {
    val viewModel: ReferencesViewModel = viewModel(factory = ReferencesViewModel.factory(bibleVersion, bibleReference))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    var shouldAnimateScrollTo by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isSearching) {
        viewModel.stopSearching()
    }

    LaunchedEffect(state.expandedBookCode) {
        if (state.isSearching) return@LaunchedEffect
        state.expandedBookCode?.let { expandedBookCode ->
            val bookIndex = state.referenceRows.indexOfFirst { it.bookCode == expandedBookCode }
            // There are 2 items for each book (header and chapters)
            val listIndex = bookIndex * 2

            if (bookIndex >= 0) {
                val lastVisibleIndex =
                    lazyListState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index
                if (shouldAnimateScrollTo) {
                    if (lastVisibleIndex != null && lastVisibleIndex - listIndex < 4) {
                        delay(450.milliseconds)
                        lazyListState.animateScrollToItem(listIndex)
                    }
                } else {
                    shouldAnimateScrollTo = true
                    lazyListState.scrollToItem(listIndex)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (state.isSearching) {
                SearchTopBar(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClose = viewModel::stopSearching,
                )
            } else {
                BibleReaderTopAppBar(
                    title = stringResource(R.string.book_chapter_picker_title),
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(onClick = viewModel::startSearching) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        val rows = state.filteredReferenceRows

        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(state = lazyListState) {
                rows.forEach { row ->
                    stickyHeader(key = row.bookCode) {
                        RowHeader(
                            bookName = row.bookName ?: row.bookCode,
                            onClick = { viewModel.expandBook(row.bookCode) },
                        )
                    }

                    item(key = "chapters_${row.bookCode}") {
                        val isVisible = state.isSearching || state.expandedBookCode == row.bookCode
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            ChaptersGrid(
                                row = row,
                                onClick = { chapter ->
                                    onSelectionClick(bibleVersion.id, row.bookCode, chapter)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BibleReaderTopAppBar(
        title = {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_books_hint),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        },
        onBackClick = onClose,
        actions = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

@Composable
private fun RowHeader(
    bookName: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = null,
                    enabled = true,
                    indication = ripple(),
                    onClick = onClick,
                ).padding(vertical = 16.dp, horizontal = 24.dp),
    ) {
        Text(text = bookName)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChaptersGrid(
    row: ReferenceRow,
    onClick: (String) -> Unit,
) {
    FlowRow(
        maxItemsInEachRow = 5,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        row.chapters.forEach { chapter ->
            ChapterCell(
                chapter = chapter,
                onClick = { onClick(chapter) },
            )
        }
    }
}

@Composable
private fun ChapterCell(
    chapter: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .size(64.dp)
                .background(MaterialTheme.readerColorScheme.buttonPrimaryColor)
                .clickable(
                    interactionSource = null,
                    enabled = true,
                    indication = ripple(),
                    onClick = onClick,
                ),
    ) {
        Text(text = chapter)
    }
}
