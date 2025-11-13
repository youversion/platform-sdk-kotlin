package com.youversion.platform.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.R

@Composable
fun SampleBottomBar(
    currentDestination: SampleDestination,
    onDestinationClick: (SampleDestination) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == SampleDestination.Reader,
            onClick = { onDestinationClick(SampleDestination.Reader) },
            icon = { Icon(Icons.Filled.Book, contentDescription = null) },
            label = { Text("Bible") },
        )

        NavigationBarItem(
            selected = currentDestination == SampleDestination.Votd,
            onClick = { onDestinationClick(SampleDestination.Votd) },
            icon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_votd),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            label = { Text("VOTD") },
        )

        NavigationBarItem(
            selected = currentDestination == SampleDestination.Widget,
            onClick = { onDestinationClick(SampleDestination.Widget) },
            icon = { Icon(Icons.Filled.Description, contentDescription = null) },
            label = { Text("Widget") },
        )
        NavigationBarItem(
            selected = currentDestination == SampleDestination.Profile,
            onClick = { onDestinationClick(SampleDestination.Profile) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Profile") },
        )
    }
}
