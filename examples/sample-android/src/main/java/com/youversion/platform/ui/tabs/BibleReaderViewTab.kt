package com.youversion.platform.ui.tabs

import androidx.compose.runtime.Composable
import com.youversion.platform.reader.BibleReader
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination

@Composable
fun BibleReaderViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    BibleReader(
        appName = "Sample App",
        appSignInMessage = "Sign in to YouVersion to access your saved versions.",
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Reader,
                onDestinationClick = onDestinationClick,
            )
        },
    )
}
