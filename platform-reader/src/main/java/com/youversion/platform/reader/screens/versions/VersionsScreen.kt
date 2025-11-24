package com.youversion.platform.reader.screens.versions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.youversion.platform.reader.theme.UntitledSerif
import com.youversion.platform.reader.theme.readerColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VersionsScreen(
    bibleVersion: BibleVersion?,
    onBackClick: () -> Unit,
    onLanguagesClick: () -> Unit,
) {
    val viewModel: VersionsViewModel = viewModel(factory = VersionsViewModel.factory(bibleVersion))
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedVersion: BibleVersion? by remember { mutableStateOf(null) }

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
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    LanguageSelector(
                        onClick = onLanguagesClick,
                    )
                }

                item {
                    CurrentLanguageHeader(
                        language = "English",
                        versionCount = 84,
                    )
                }

                when {
                    state.initializing -> {
                        item {
                            CircularProgressIndicator()
                        }
                    }

                    state.showEmptyState -> {
                        item {
                            Text("No versions found for this language")
                        }
                    }

                    else -> {
                        items(
                            items = state.filteredVersions,
                            key = { it.id },
                        ) { version ->
                            BibleVersionRow(
                                bibleVersion = version,
                                onClick = {},
                            )
                        }
                    }
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
private fun CurrentLanguageHeader(
    language: String,
    versionCount: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
    ) {
        Text(
            text = "$language Versions ($versionCount)",
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
    onClick: () -> Unit,
) {
    val abbreviation = bibleVersion.localizedAbbreviation ?: bibleVersion.abbreviation ?: bibleVersion.id.toString()
    val (letters, numbers) = splitAbbreviation(abbreviation)

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    enabled = true,
                    onClick = onClick,
                ).padding(vertical = 8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .size(56.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.readerColorScheme.borderPrimaryColor,
                        shape = RoundedCornerShape(4.dp),
                    ).background(
                        color = MaterialTheme.readerColorScheme.buttonPrimaryColor,
                        shape = RoundedCornerShape(4.dp),
                    ).padding(6.dp),
        ) {
            Text(
                text = letters,
                maxLines = 1,
                color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp, stepSize = 1.sp),
                style =
                    TextStyle(
                        fontFamily = UntitledSerif,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            if (numbers.isNotBlank()) {
                Text(
                    text = numbers,
                    maxLines = 1,
                    style =
                        TextStyle(
                            fontFamily = UntitledSerif,
                            fontSize = 14.sp,
                        ),
                )
            }
        }
        Text(
            text = bibleVersion.localizedTitle ?: bibleVersion.title ?: bibleVersion.id.toString(),
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
                    BibleVersionRow(it) { }
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
        )
    }
}
