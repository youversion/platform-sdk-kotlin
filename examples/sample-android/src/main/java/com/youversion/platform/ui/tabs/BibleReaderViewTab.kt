package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * The reader hosts the bottom bar inside its own bar, which has the reader's background and a small inset. The bar
 * paints its background past its own edges so that frame matches the sample's surface colour.
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
    val bottomBarColor = MaterialTheme.colorScheme.surface

    BibleReader(
        bibleReference = bibleReference,
        fontDefinitionProvider = fontDefinitionProvider,
        bottomBar = {
            Box(
                modifier =
                    Modifier.drawBehind {
                        drawRect(
                            color = bottomBarColor,
                            topLeft = Offset(-size.width, -size.height),
                            size = Size(size.width * 3, size.height * 3),
                        )
                    },
            ) {
                SampleBottomBar(
                    currentDestination = SampleDestination.Reader,
                    onDestinationClick = onDestinationClick,
                    containerColor = bottomBarColor,
                )
            }
        },
    )
}
