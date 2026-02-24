package com.youversion.platform.reader.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.R

@Composable
internal fun BibleReaderVerseActionSheet(
    selectedVerses: Set<BibleReference>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VerseActionButton(
                icon = ImageVector.vectorResource(R.drawable.ic_copy),
                label = stringResource(R.string.verse_action_copy),
                onClick = onCopy,
            )
            VerseActionButton(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.verse_action_share),
                onClick = onShare,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun VerseActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .size(56.dp)
                .clip(buttonShape)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClick = onClick,
                ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
