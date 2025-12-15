package com.youversion.platform.reader.screens.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.utilities.splitAbbreviation
import com.youversion.platform.reader.R
import com.youversion.platform.reader.components.BibleReaderTopAppBar
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.readerColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VersionsScreen(
    bibleVersion: BibleVersion?,
    onBackClick: () -> Unit,
    onLanguagesClick: () -> Unit,
    onVersionSelect: (BibleVersion) -> Unit,
) {
    val viewModel: VersionsViewModel = viewModel(factory = VersionsViewModel.factory(bibleVersion))
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BibleReaderTopAppBar(
                title = "Versions",
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
                            onClick = onLanguagesClick,
                        )
                    }
                }

                item {
                    BibleVersionsSectionHeader(
                        title = "English Versions",
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
                            items = state.filteredVersions,
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
private fun LanguageSelector(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(CircleShape)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.readerColorScheme.buttonPrimaryColor,
                ).clickable(
                    interactionSource = null,
                    indication = ripple(),
                    enabled = true,
                    onClick = onClick,
                ).padding(16.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_material_language),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Language",
            modifier = Modifier.weight(1f),
        )

        Text("English")
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_expand_circle_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
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
            style =
                TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
        )
    }
}

@Composable
private fun BibleVersionRow(
    bibleVersion: BibleVersion,
    onVersionInfoClick: () -> Unit,
    onVersionClick: () -> Unit,
) {
    val abbreviation = bibleVersion.localizedAbbreviation ?: bibleVersion.abbreviation ?: bibleVersion.id.toString()
    val (letters, numbers) = splitAbbreviation(abbreviation)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    enabled = true,
                    onClick = onVersionClick,
                ).padding(vertical = 6.dp, horizontal = 20.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = abbreviation,
                maxLines = 1,
                color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
            )
            Text(
                text = bibleVersion.localizedTitle ?: bibleVersion.title ?: bibleVersion.id.toString(),
            )
        }

        IconButton(
            onClick = onVersionInfoClick,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Bible Version Details",
            )
        }
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

@Composable
@Preview
private fun Preview_LanguagesScreen() {
    BibleReaderMaterialTheme {
        VersionsScreen(
            bibleVersion = BibleVersion.preview,
            onBackClick = {},
            onLanguagesClick = {},
            onVersionSelect = {},
        )
    }
}
