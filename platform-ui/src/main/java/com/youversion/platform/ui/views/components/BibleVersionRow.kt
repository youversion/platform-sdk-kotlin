package com.youversion.platform.ui.views.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.ui.theme.ui.BibleReaderTheme

@Composable
fun BibleVersionRow(
    bibleVersion: BibleVersion,
    onVersionInfoClick: () -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val abbreviation =
        bibleVersion.localizedAbbreviation
            ?: bibleVersion.abbreviation
            ?: bibleVersion.id.toString()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    enabled = true,
                    onClick = onVersionClick,
                ).padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = abbreviation,
                maxLines = 1,
                style = BibleReaderTheme.typography.labelL,
                color = BibleReaderTheme.colorScheme.textPrimary,
            )
            Text(
                text = bibleVersion.localizedTitle ?: bibleVersion.title ?: bibleVersion.id.toString(),
                style = BibleReaderTheme.typography.labelM,
                color = BibleReaderTheme.colorScheme.textMuted,
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
