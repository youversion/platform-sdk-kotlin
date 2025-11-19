package com.youversion.platform.reader.screens.fonts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.FontDefinition
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
                items(items = state.allFontDefinitions, key = { it.fontName }) { item ->
                    FontOptionRow(
                        option = item,
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
    onClick: (FontDefinition) -> Unit,
) {
    Row {
        Text(
            text = option.fontName,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { onClick(option) },
                    ).padding(vertical = 16.dp, horizontal = 24.dp),
            style = TextStyle(fontFamily = option.fontFamily, fontSize = 20.sp),
        )
    }
}
