package com.youversion.platform.reader.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.BibleReaderSearchViewModel.SearchStatus
import com.youversion.platform.reader.BibleReaderSearchViewModel.State
import com.youversion.platform.reader.R
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Cream
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.views.components.SearchBar
import kotlinx.coroutines.launch

/** The query the search endpoint accepts, counted the way the field counts what is typed into it. */
private const val MAXIMUM_QUERY_GRAPHEME_CLUSTER_COUNT = 100

/** The panel a search that failed or found nothing is said in. */
internal const val SEARCH_MESSAGE_TEST_TAG = "search_message"

/** The full-height sheet a reader searches from, risen over the reader with the field already focused. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleReaderSearchSheet(
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    state: State,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
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
                    clearButtonContentDescription = stringResource(R.string.cancel),
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
                        text = stringResource(R.string.done),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            when (state.status) {
                SearchStatus.IDLE -> Unit

                SearchStatus.SEARCHING -> SearchingIndicator()

                SearchStatus.FAILED ->
                    SearchMessage(
                        imageVector = Icons.Default.Warning,
                        message = stringResource(R.string.error),
                    )

                SearchStatus.COMPLETED ->
                    if (state.results.isEmpty()) {
                        SearchMessage(
                            imageVector = Icons.Default.Search,
                            message = stringResource(R.string.no_bible_search_results),
                        )
                    } else {
                        SearchResults(results = state.results, searchVersion = state.searchVersion)
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
    val label = stringResource(R.string.search)

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

@Composable
private fun SearchResults(
    results: List<BibleReference>,
    searchVersion: BibleVersion?,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(results) { reference ->
            Text(
                text = reference.title(searchVersion),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
            )
        }
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
            state = State(),
        )
    }
}
