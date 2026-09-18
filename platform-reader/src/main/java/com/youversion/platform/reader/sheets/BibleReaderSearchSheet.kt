package com.youversion.platform.reader.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.reader.BibleReaderSearchViewModel.SearchStatus
import com.youversion.platform.reader.BibleReaderSearchViewModel.State
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Cream
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.views.components.SearchBar
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.youversion.platform.ui.R as UiR

/** The query the search endpoint accepts, counted the way the field counts what is typed into it. */
private const val MAXIMUM_QUERY_GRAPHEME_CLUSTER_COUNT = 100

/** How much of a result's own verse text is shown, so several results can be compared at once. */
private const val RESULT_TEXT_MAXIMUM_LINE_COUNT = 3

/**
 * How near the end of what is loaded the reader gets before the next page is asked for. Asking short of the bottom
 * means the page is already on its way by the time they reach it.
 */
private const val RESULTS_REMAINING_COUNT_BEFORE_NEXT_PAGE = 5

/** The panel a search that failed or found nothing is said in. */
internal const val SEARCH_MESSAGE_TEST_TAG = "search_message"

/** The list the results are read down, which pages as it is scrolled. */
internal const val SEARCH_RESULTS_TEST_TAG = "search_results"

/** The list of queries offered to a reader who has not searched yet. */
internal const val SEARCH_SUGGESTED_QUERIES_TEST_TAG = "search_suggested_queries"

/**
 * The sheet a reader searches from, risen over the reader with the field already focused. It stops at the status bar
 * rather than covering it, so the dimmed reader still shows above it as it does in Swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleReaderSearchSheet(
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRequestResultText: (BibleReference) -> Unit,
    onLoadNextPage: () -> Unit,
    onSelectSuggestedQuery: (SearchQuery) -> Unit,
    state: State,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        modifier = Modifier.statusBarsPadding(),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SearchBar(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.readerColorScheme.buttonSecondaryColor,
                    onSubmit = onSubmit,
                    showsClearButton = true,
                    focusRequester = focusRequester,
                    maximumGraphemeClusterCount = MAXIMUM_QUERY_GRAPHEME_CLUSTER_COUNT,
                )

                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(
                        text = stringResource(UiR.string.done),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            when (state.status) {
                SearchStatus.IDLE ->
                    SuggestedQueries(
                        queries = state.suggestedQueries,
                        isLoading = state.isLoadingSuggestedQueries,
                        onSelectSuggestedQuery = onSelectSuggestedQuery,
                    )

                SearchStatus.SEARCHING -> SearchingIndicator()

                SearchStatus.FAILED ->
                    SearchMessage(
                        imageVector = Icons.Default.Warning,
                        message = stringResource(UiR.string.error),
                    )

                SearchStatus.COMPLETED ->
                    if (state.results.isEmpty()) {
                        SearchMessage(
                            imageVector = Icons.Default.Search,
                            message = stringResource(UiR.string.no_bible_search_results),
                        )
                    } else {
                        SearchResults(
                            results = state.results,
                            resultTextByPassageId = state.resultTextByPassageId,
                            searchVersion = state.searchVersion,
                            nextPageToken = state.nextPageToken,
                            isLoadingNextPage = state.isLoadingNextPage,
                            hasNextPageLoadError = state.hasNextPageLoadError,
                            onRequestResultText = onRequestResultText,
                            onLoadNextPage = onLoadNextPage,
                        )
                    }
            }
        }
    }
}

/**
 * The indicator shown while a search runs, labelled through the semantics modifier because the
 * Material indicator takes no content description of its own.
 */
@Composable
private fun SearchingIndicator() {
    val label = stringResource(UiR.string.search)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

/** A search that failed or found nothing, said plainly. The icon is decorative. */
@Composable
private fun SearchMessage(
    imageVector: ImageVector,
    message: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp)
                .testTag(SEARCH_MESSAGE_TEST_TAG),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The results, paged from the list's own scroll state: once the last row on screen is within
 * [RESULTS_REMAINING_COUNT_BEFORE_NEXT_PAGE] of the end of what is loaded, the next page is asked for. The effect is keyed
 * to [nextPageToken] so that a page landing restarts it and the reader standing still at the foot of the list is
 * asked about again; a list with no token left to page on is not watched at all.
 */
@Composable
private fun SearchResults(
    results: List<BibleReference>,
    resultTextByPassageId: Map<String, String?>,
    searchVersion: BibleVersion?,
    nextPageToken: String?,
    isLoadingNextPage: Boolean,
    hasNextPageLoadError: Boolean,
    onRequestResultText: (BibleReference) -> Unit,
    onLoadNextPage: () -> Unit,
) {
    val listState = rememberLazyListState()
    val loadedResultCount by rememberUpdatedState(results.size)
    val loadNextPage by rememberUpdatedState(onLoadNextPage)

    LaunchedEffect(listState, nextPageToken) {
        if (nextPageToken.isNullOrEmpty()) return@LaunchedEffect

        snapshotFlow {
            val lastVisibleIndex =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: -1
            lastVisibleIndex >= loadedResultCount - RESULTS_REMAINING_COUNT_BEFORE_NEXT_PAGE
        }.filter { it }
            .collect { loadNextPage() }
    }

    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(SEARCH_RESULTS_TEST_TAG),
    ) {
        items(results, key = { it.asUSFM }) { reference ->
            SearchResult(
                reference = reference,
                text = resultTextByPassageId[reference.asUSFM],
                searchVersion = searchVersion,
                onRequestResultText = onRequestResultText,
            )
        }

        if (isLoadingNextPage) {
            item { InlineSearchingIndicator() }
        } else if (hasNextPageLoadError) {
            item { NextPageRetry(onLoadNextPage = onLoadNextPage) }
        }
    }
}

/**
 * The indicator shown within a list that is being added to, labelled through the semantics modifier because the
 * Material indicator takes no content description of its own.
 */
@Composable
private fun InlineSearchingIndicator() {
    val label = stringResource(UiR.string.search)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .size(20.dp)
                    .semantics { contentDescription = label },
        )
    }
}

/**
 * What a page that would not load offers instead, the query no longer being the obvious way to ask again. It is only
 * ever shown while nothing is in flight, so tapping it twice over cannot start a second request. The icon is
 * decorative.
 */
@Composable
private fun NextPageRetry(onLoadNextPage: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
    ) {
        TextButton(onClick = onLoadNextPage) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )

            Text(
                text = stringResource(UiR.string.error),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * Somewhere for a reader who has not searched yet to start: what other readers are searching while the field is
 * empty, and what the platform makes of what has been entered once it is not.
 *
 * Taking one up drops the keyboard first, so the results it runs are on screen rather than behind it. The icon on
 * each row is decorative, the query itself being what the row says.
 */
@Composable
private fun SuggestedQueries(
    queries: List<SearchQuery>,
    isLoading: Boolean,
    onSelectSuggestedQuery: (SearchQuery) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(SEARCH_SUGGESTED_QUERIES_TEST_TAG),
    ) {
        items(queries) { query ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusManager.clearFocus()
                            onSelectSuggestedQuery(query)
                        }.padding(vertical = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )

                Text(
                    text = query.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isLoading) {
            item { InlineSearchingIndicator() }
        }
    }
}

/**
 * One result, which asks for its own verse text as it is composed — which a lazy list only does near the
 * viewport, so a result the reader has not scrolled to costs nothing. The text is left out entirely until it
 * arrives rather than held open, so the title does not move when it lands.
 */
@Composable
private fun SearchResult(
    reference: BibleReference,
    text: String?,
    searchVersion: BibleVersion?,
    onRequestResultText: (BibleReference) -> Unit,
) {
    LaunchedEffect(reference) {
        onRequestResultText(reference)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
    ) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = RESULT_TEXT_MAXIMUM_LINE_COUNT,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = reference.title(searchVersion),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The reference as the version the search ran against titles it, without that version's
 * abbreviation, falling back to the raw passage id when there is no version to ask.
 */
private fun BibleReference.title(searchVersion: BibleVersion?): String =
    searchVersion?.displayTitle(this, includesVersionAbbreviation = false)?.uppercase() ?: asUSFM

@Preview
@Composable
private fun Preview_BibleReaderSearchSheet() {
    BibleReaderMaterialTheme(readerColorScheme = Cream) {
        BibleReaderSearchSheet(
            onDismissRequest = {},
            onQueryChange = {},
            onSubmit = {},
            onRequestResultText = {},
            onLoadNextPage = {},
            onSelectSuggestedQuery = {},
            state = State(),
        )
    }
}
