package com.youversion.platform.reader.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.reader.R
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Cream
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.views.components.SearchBar
import kotlinx.coroutines.launch

/** The query the search endpoint accepts, counted the way the field counts what is typed into it. */
private const val MAXIMUM_QUERY_GRAPHEME_CLUSTER_COUNT = 100

/** The full-height sheet a reader searches from, risen over the reader with the field already focused. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleReaderSearchSheet(
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    query: String,
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
                    query = query,
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
        }
    }
}

@Preview
@Composable
private fun Preview_BibleReaderSearchSheet() {
    BibleReaderMaterialTheme(readerColorScheme = Cream) {
        BibleReaderSearchSheet(
            onDismissRequest = {},
            onQueryChange = {},
            onSubmit = {},
            query = "",
        )
    }
}
