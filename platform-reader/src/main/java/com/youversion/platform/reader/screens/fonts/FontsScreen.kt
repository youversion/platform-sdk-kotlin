package com.youversion.platform.reader.screens.fonts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.FontDefinition
import com.youversion.platform.reader.R
import com.youversion.platform.reader.components.BibleReaderTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontsScreen(
    viewModel: BibleReaderViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BibleReaderTopAppBar(title = "Fonts", onBackClick = onBackClick)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn {
                itemsIndexed(items = state.allFontDefinitions, key = { index, _ -> index }) { _, item ->
                    FontOptionRow(
                        option = item,
                        selected = item == state.selectedFontDefinition,
                        onClick = {
                            viewModel.onAction(
                                BibleReaderViewModel.Action.SetFontDefinition(it),
                            )
                            onBackClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FontOptionRow(
    option: FontDefinition,
    selected: Boolean,
    onClick: (FontDefinition) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clickable(
                    onClick = { onClick(option) },
                ).padding(horizontal = 24.dp),
    ) {
        Text(
            text = option.fontName,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
            style = TextStyle(fontFamily = option.fontFamily, fontSize = 20.sp),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.font_selected_check),
            )
        }
    }
}
