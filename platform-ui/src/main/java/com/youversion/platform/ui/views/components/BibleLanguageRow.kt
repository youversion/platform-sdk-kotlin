package com.youversion.platform.ui.views.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BibleLanguageRow(
    language: LanguageRowItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    enabled = true,
                    indication = ripple(),
                    onClick = onClick,
                ).padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = language.displayName,
            color = MaterialTheme.colorScheme.onSurface,
        )
        language.localeDisplayName?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
