package com.youversion.platform.reader.screens.versions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.reader.R
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.Charcoal
import com.youversion.platform.reader.theme.UntitledSerif
import com.youversion.platform.reader.theme.readerColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionInfoBottomSheet(
    bibleVersion: BibleVersion,
    organization: Organization?,
    onDismissRequest: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            VersionHeader(
                bibleVersion = bibleVersion,
                publisherName = organization?.name,
            )
            PrimaryActions(
                onDownloadClick = {},
                onReadSampleClick = {},
            )
            if (
                bibleVersion.readerFooterUrl != null ||
                bibleVersion.copyrightLong != null ||
                bibleVersion.copyrightShort != null
            ) {
                Text(
                    text = "Details",
                    style =
                        TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.readerColorScheme.readerTextMutedColor,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            VersionWebsite(
                bibleVersion = bibleVersion,
                onClick = {},
            )
            VersionCopyright(
                bibleVersion = bibleVersion,
            )
        }
    }
}

@Composable
private fun VersionHeader(
    bibleVersion: BibleVersion,
    publisherName: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = bibleVersion.localizedAbbreviation ?: "",
            style =
                TextStyle(
                    fontSize = 64.sp,
                    fontFamily = UntitledSerif,
                ),
        )
        Text(
            text = bibleVersion.localizedTitle ?: "",
            textAlign = TextAlign.Center,
            style =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
        )

        publisherName?.let {
            Text(
                text = publisherName,
                textAlign = TextAlign.Center,
                style =
                    TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.readerColorScheme.readerTextMutedColor,
            )
        }
    }
}

@Composable
private fun PrimaryActions(
    onDownloadClick: () -> Unit,
    onReadSampleClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
    ) {
        Button(
            onClick = onDownloadClick,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.readerColorScheme.buttonPrimaryColor,
                    contentColor = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
                ),
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text(text = "Add")
        }

        Button(
            onClick = onReadSampleClick,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.readerColorScheme.buttonSecondaryColor,
                    contentColor = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
                ),
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Text(text = "Sample")
        }
    }
}

@Composable
private fun VersionWebsite(
    bibleVersion: BibleVersion,
    onClick: () -> Unit,
) {
    bibleVersion.readerFooterUrl?.let { url ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onClick,
                    ).padding(vertical = 8.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_material_language),
                contentDescription = null,
            )

            Text(
                text = url,
            )
        }
    }
}

@Composable
private fun VersionCopyright(bibleVersion: BibleVersion) {
    val copyright = bibleVersion.copyrightLong ?: bibleVersion.copyrightShort
    copyright?.let {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = copyright,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    } ?: Spacer(modifier = Modifier.height(20.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_VersionInfoBottomSheet() {
    BibleReaderMaterialTheme(Charcoal) {
        Scaffold { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                VersionInfoBottomSheet(
                    bibleVersion = BibleVersion.preview,
                    organization = Organization.preview,
                    onDismissRequest = {},
                )
            }
        }
    }
}
