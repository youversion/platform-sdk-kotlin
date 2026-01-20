package com.youversion.platform.reader.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.Charcoal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) = BibleReaderTopAppBar(
    title = { Text(text = title) },
    onBackClick = onBackClick,
    actions = actions,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderTopAppBar(
    title: @Composable () -> Unit,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
            }
        },
        actions = actions,
    )
}

@Preview
@Composable
private fun Preview_BibleReaderTopAppBar() {
    BibleReaderMaterialTheme(Charcoal) {
        BibleReaderTopAppBar(title = "Title", onBackClick = {})
    }
}
