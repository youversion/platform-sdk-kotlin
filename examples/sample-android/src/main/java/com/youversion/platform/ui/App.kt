package com.youversion.platform.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.tabs.BibleReaderViewTab
import com.youversion.platform.ui.tabs.ProfileViewTab
import com.youversion.platform.ui.tabs.VotdViewTab
import com.youversion.platform.ui.tabs.WidgetViewTab

@Composable
fun App() {
    val navController = rememberNavController()
    val onDestinationClick: (SampleDestination) -> Unit = { destination ->
        navController.navigate(destination.route)
    }

    NavHost(
        navController = navController,
        startDestination = SampleDestination.Reader.route,
    ) {
        composable(
            route = SampleDestination.Reader.route,
        ) {
            BibleReaderViewTab(
                onDestinationClick = onDestinationClick,
            )
        }

        composable(
            route = SampleDestination.Votd.route,
        ) {
            VotdViewTab(
                onDestinationClick = onDestinationClick,
            )
        }

        composable(
            route = SampleDestination.Widget.route,
        ) {
            WidgetViewTab(onDestinationClick = onDestinationClick)
        }

        composable(
            route = SampleDestination.Profile.route,
        ) {
            ProfileViewTab(onDestinationClick = onDestinationClick)
        }
    }
}
