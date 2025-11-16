package com.youversion.platform.reader.screens.versions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.components.BibleReaderTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VersionsScreen(
    onBackClick: () -> Unit,
    onLanguagesClick: () -> Unit,
) {
    var selectedVersion: BibleVersion? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            BibleReaderTopAppBar(title = "Versions", onBackClick = onBackClick)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column {
                Text(text = "Versions Screen")
                Button(onClick = onLanguagesClick) {
                    Text("Languages")
                }

                Button(onClick = {
                    selectedVersion =
                        BibleVersion(id = 206, abbreviation = "KJV", localizedTitle = "King James Version")
                }) {
                    Text("KJV")
                }
            }

            selectedVersion?.let {
                ModalBottomSheet(
                    onDismissRequest = { selectedVersion = null },
                ) {
                    Column {
                        Text(text = it.localizedTitle ?: it.abbreviation ?: it.id.toString())
                    }
                }
            }
        }
    }
}
