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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Charcoal
import com.youversion.platform.ui.theme.UntitledSerif
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.R as UiR

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
            modifier =
                Modifier
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            VersionHeader(
                bibleVersion = bibleVersion,
                publisherName = organization?.name,
            )
            OfflineAgreement()
            VersionCopyright(
                bibleVersion = bibleVersion,
            )
            VersionWebsite(
                bibleVersion = bibleVersion,
                onClick = {},
            )
            PrimaryActions(
                onDownloadClick = {},
                onDismissClick = onDismissRequest,
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
private fun OfflineAgreement() {
    Column(
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Text(
            text = stringResource(UiR.string.version_info_offline_agreement),
            style =
                TextStyle(
                    fontSize = 14.sp,
                ),
            color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(UiR.string.version_info_offline_tagline),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PrimaryActions(
    onDownloadClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 20.dp),
    ) {
        Button(
            onClick = onDownloadClick,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.readerColorScheme.buttonContrastColor,
                    contentColor = MaterialTheme.readerColorScheme.textInvertedColor,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),
        ) {
            Text(
                text = stringResource(UiR.string.version_info_agree_and_download),
                style =
                    TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }

        TextButton(
            onClick = onDismissClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(UiR.string.version_info_maybe_later),
                style =
                    TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                    ),
                color = MaterialTheme.readerColorScheme.readerTextPrimaryColor.copy(alpha = 0.5f),
            )
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
                imageVector = ImageVector.vectorResource(UiR.drawable.ic_material_language),
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
    val copyright = bibleVersion.promotionalContent ?: bibleVersion.copyright
    copyright?.let {
        Column {
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
