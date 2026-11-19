package com.youversion.platform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.icons.BibleFilledIcon
import com.youversion.platform.ui.icons.BibleStrokedIcon
import com.youversion.platform.ui.icons.ContentIcon
import com.youversion.platform.ui.icons.ExamplesFilledIcon
import com.youversion.platform.ui.icons.ExamplesStrokedIcon
import com.youversion.platform.ui.theme.TabSelectedColor

/**
 * The sample app's bottom tab bar, styled like the Swift sample's.
 *
 * @param currentDestination The tab to show as selected.
 * @param onDestinationClick Called when the user picks a tab.
 */
@Composable
fun SampleBottomBar(
    currentDestination: SampleDestination,
    onDestinationClick: (SampleDestination) -> Unit,
) {
    val isBibleSelected = currentDestination == SampleDestination.Reader
    val isExamplesSelected = currentDestination == SampleDestination.Examples

    NavigationBar {
        SampleTab(
            label = "Bible",
            icon = if (isBibleSelected) BibleFilledIcon else BibleStrokedIcon,
            isSelected = isBibleSelected,
            onClick = { onDestinationClick(SampleDestination.Reader) },
        )
        SampleTab(
            label = "Content",
            icon = ContentIcon,
            isSelected = currentDestination == SampleDestination.Content,
            onClick = { onDestinationClick(SampleDestination.Content) },
        )
        SampleTab(
            label = "Examples",
            icon = if (isExamplesSelected) ExamplesFilledIcon else ExamplesStrokedIcon,
            isSelected = isExamplesSelected,
            onClick = { onDestinationClick(SampleDestination.Examples) },
        )
        SampleTab(
            label = "Profile",
            icon = Icons.Filled.Person,
            isSelected = currentDestination == SampleDestination.Profile,
            onClick = { onDestinationClick(SampleDestination.Profile) },
        )
    }
}

@Composable
private fun RowScope.SampleTab(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val iconColor = if (isSelected) TabSelectedColor else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .heightIn(min = 57.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
                ).selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
