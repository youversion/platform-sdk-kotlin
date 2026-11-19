package com.youversion.platform.ui.tabs

import androidx.compose.runtime.Composable
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.BibleReader
import com.youversion.platform.reader.FontDefinition
import com.youversion.platform.reader.FontDefinitionProvider
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.theme.CormorantGaramond
import com.youversion.platform.ui.theme.Tinos

/**
 * The Bible tab, showing the SDK's Bible reader with the sample's fonts.
 *
 * @param bibleReference The passage to open, or null to restore the last-read passage.
 * @param onDestinationClick Called when the user picks a tab in the bottom bar.
 */
@Composable
fun BibleReaderViewTab(
    bibleReference: BibleReference?,
    onDestinationClick: (SampleDestination) -> Unit,
) {
    val fontDefinitionProvider =
        object : FontDefinitionProvider {
            override fun fonts(): List<FontDefinition> =
                listOf(
                    FontDefinition("Tinos", Tinos),
                    FontDefinition("Cormorant Garamond", CormorantGaramond),
                )
        }

    BibleReader(
        bibleReference = bibleReference,
        fontDefinitionProvider = fontDefinitionProvider,
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Reader,
                onDestinationClick = onDestinationClick,
            )
        },
    )
}
