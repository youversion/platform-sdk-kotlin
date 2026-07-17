package com.youversion.platform.reader.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.R

@Composable
internal fun BibleReaderVerseActionSheet(
    colorsToRemove: List<HighlightColor>,
    colorsToAdd: List<HighlightColor>,
    onAddHighlight: (String) -> Unit,
    onRemoveHighlight: (String) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (colorsToRemove.isNotEmpty() || colorsToAdd.isNotEmpty()) {
                HighlightColorPicker(
                    colorsToRemove = colorsToRemove,
                    colorsToAdd = colorsToAdd,
                    onAddHighlight = onAddHighlight,
                    onRemoveHighlight = onRemoveHighlight,
                )
            }
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
private fun HighlightColorPicker(
    colorsToRemove: List<HighlightColor>,
    colorsToAdd: List<HighlightColor>,
    onAddHighlight: (String) -> Unit,
    onRemoveHighlight: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp),
    ) {
        colorsToRemove.forEach { highlightColor ->
            HighlightColorButton(
                highlightColor = highlightColor,
                isSelected = true,
                onClick = { onRemoveHighlight(highlightColor.hexColor) },
            )
        }
        colorsToAdd.forEach { highlightColor ->
            HighlightColorButton(
                highlightColor = highlightColor,
                isSelected = false,
                onClick = { onAddHighlight(highlightColor.hexColor) },
            )
        }
    }
}

@Composable
private fun HighlightColorButton(
    highlightColor: HighlightColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentDescription =
        stringResource(
            if (isSelected) {
                R.string.verse_action_remove_highlight
            } else {
                R.string.verse_action_add_highlight
            },
        )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(highlightColor.color)
                .border(
                    width = 1.dp,
                    color = Color(0xFF121212).copy(alpha = 0.2f),
                    shape = CircleShape,
                ).clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClick = onClick,
                ).semantics { this.contentDescription = contentDescription },
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF121212),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun VerseActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
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
