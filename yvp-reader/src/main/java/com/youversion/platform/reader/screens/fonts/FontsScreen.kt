package com.youversion.platform.reader.screens.fonts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.youversion.platform.reader.components.BibleReaderTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            BibleReaderTopAppBar(title = "Fonts", onBackClick = onBackClick)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Text(text = "Fonts Screen")
        }
    }
}
