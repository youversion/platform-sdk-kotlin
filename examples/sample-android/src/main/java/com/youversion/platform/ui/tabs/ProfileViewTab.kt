package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.views.SignInWithYouVersionButton

@Composable
fun ProfileViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    Scaffold(
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Profile,
                onDestinationClick = onDestinationClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            SignInWithYouVersionButton(
                onClick = {
                    // TODO: YV Auth
                },
                stroked = true,
            )
        }
    }
}
