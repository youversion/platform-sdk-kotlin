package com.youversion.platform.ui.tabs

import androidx.compose.runtime.Composable
import com.youversion.platform.reader.BibleReader
import com.youversion.platform.reader.FontDefinition
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.theme.Tinos

@Composable
fun BibleReaderViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val fontDefinitionProvider =
        object : FontDefinitionProvider {
            override fun fonts(): List<FontDefinition> = listOf(FontDefinition("Tinos", Tinos))
        }

    BibleReader(
        appName = "Sample App",
        appSignInMessage = "Sign in to YouVersion to access your saved versions.",
        fontDefinitionProvider = fontDefinitionProvider,
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Reader,
                onDestinationClick = onDestinationClick,
            )
        },
    )
}
