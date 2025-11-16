package com.youversion.platform.reader.screens.versions

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
internal fun LanguagesScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            BibleReaderTopAppBar(title = "Languages", onBackClick = onBackClick)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Text(text = "Languages Screen")
        }
    }
}
