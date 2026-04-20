package com.youversion.platform.reader.screens.versions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.components.BibleReaderTopAppBar
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import com.youversion.platform.ui.views.components.BibleVersionRow
import com.youversion.platform.ui.views.components.LanguageSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VersionsScreen(
    viewModel: VersionsViewModel,
    onBackClick: () -> Unit,
    onLanguagesClick: () -> Unit,
    onVersionSelect: (BibleVersion) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BibleReaderTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Versions",
                            style = BibleReaderTheme.typography.headerM,
                        )
                        if (state.versionsCount > 0) {
                            Text(
                                text = "${state.versionsCount} Versions in ${state.languagesCount} Languages",
                                style = BibleReaderTheme.typography.captionXS,
                            )
                        }
                    }
                },
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            ) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        LanguageSelector(
                            activeLanguageName = state.activeLanguageName,
                            enabled = !state.initializing,
                            onClick = onLanguagesClick,
                        )
                    }
                }

                item {
                    BibleVersionsSectionHeader(
                        title = "${state.activeLanguageName} Versions (${state.activeLanguageVersionsCount})",
                    )
                }

                when {
                    state.initializing -> {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    state.showEmptyState -> {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            ) {
                                Text("No versions found for this language")
                            }
                        }
                    }

                    else -> {
                        items(
                            items = state.activeLanguageVersions,
                            key = { it.id },
                        ) { version ->
                            BibleVersionRow(
                                bibleVersion = version,
                                onVersionInfoClick = {
                                    viewModel.onAction(
                                        VersionsViewModel.Action.VersionInfoTapped(version),
                                    )
                                },
                                onVersionClick = {
                                    onVersionSelect(version)
                                },
                            )
                        }
                    }
                }
            }

            state.selectedBibleVersion?.let {
                VersionInfoBottomSheet(
                    bibleVersion = it,
                    organization = state.selectedOrganization,
                    onDismissRequest = { viewModel.onAction(VersionsViewModel.Action.VersionDismissed) },
                )
            }
        }
    }
}

@Composable
private fun BibleVersionsSectionHeader(title: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            style = BibleReaderTheme.typography.headerM,
        )
    }
}

@Composable
@Preview
private fun Preview_VersionsList() {
    BibleReaderMaterialTheme {
        Surface {
            LazyColumn {
                items(items = listOf(BibleVersion.preview, BibleVersion.preview, BibleVersion.preview)) {
                    BibleVersionRow(
                        bibleVersion = it,
                        onVersionInfoClick = {},
                        onVersionClick = {},
                    )
                }
            }
        }
    }
}
