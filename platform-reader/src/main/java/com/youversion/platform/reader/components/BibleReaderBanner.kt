package com.youversion.platform.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.reader.R
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme

/** The type of informational banner to display in the Bible reader. */
enum class BibleReaderBannerType {
    OFFLINE,
    VERSION_UNAVAILABLE,
}

/** A dismissible informational banner displayed at the top of the Bible reader. */
@Composable
fun BibleReaderBanner(
    bannerType: BibleReaderBannerType,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        val icon =
            when (bannerType) {
                BibleReaderBannerType.OFFLINE ->
                    ImageVector.vectorResource(R.drawable.ic_wifi_exclamation)
                BibleReaderBannerType.VERSION_UNAVAILABLE ->
                    Icons.Outlined.Sync
            }

        val message =
            when (bannerType) {
                BibleReaderBannerType.OFFLINE ->
                    stringResource(R.string.banner_offline_title) + " " +
                        stringResource(R.string.banner_offline_description)
                BibleReaderBannerType.VERSION_UNAVAILABLE ->
                    stringResource(R.string.banner_version_unavailable_title) + " " +
                        stringResource(R.string.banner_version_unavailable_description)
            }

        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BibleReaderTheme.colorScheme.surfaceSecondary)
                    .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = BibleReaderTheme.colorScheme.textMuted,
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = message,
                style = BibleReaderTheme.typography.captionL,
                color = BibleReaderTheme.colorScheme.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.banner_dismiss_content_description),
                    modifier = Modifier.size(16.dp),
                    tint = BibleReaderTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_BibleReaderBanner_Offline() {
    BibleReaderMaterialTheme {
        BibleReaderBanner(
            bannerType = BibleReaderBannerType.OFFLINE,
            isVisible = true,
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun Preview_BibleReaderBanner_VersionUnavailable() {
    BibleReaderMaterialTheme {
        BibleReaderBanner(
            bannerType = BibleReaderBannerType.VERSION_UNAVAILABLE,
            isVisible = true,
            onDismiss = {},
        )
    }
}
