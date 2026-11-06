package com.youversion.platform.reader.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.MidnightBlue
import com.youversion.platform.ui.theme.ui.BibleReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows offline banner`() {
        composeTestRule.setContent {
            BibleReaderBanner(
                bannerType = BibleReaderBannerType.OFFLINE,
                isVisible = true,
                onDismiss = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                "We’re having difficulties with your connection. Please download a Bible version when you’re online.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Offline").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun `shows version unavailable banner`() {
        composeTestRule.setContent {
            BibleReaderBanner(
                bannerType = BibleReaderBannerType.VERSION_UNAVAILABLE,
                isVisible = true,
                onDismiss = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Please switch to another one.",
            ).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Version unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun `triggers the onDismiss and is no longer visible`() {
        val isDismissed = mutableStateOf(false)
        composeTestRule.setContent {
            BibleReaderBanner(
                bannerType = BibleReaderBannerType.VERSION_UNAVAILABLE,
                isVisible = !isDismissed.value,
                onDismiss = { isDismissed.value = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()

        assertTrue(isDismissed.value)
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Please switch to another one.",
            ).assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Version unavailable").assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertIsNotDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [34], qualifiers = "xxhdpi")
    fun `tints the banner icon and close button with textPrimary in a dark reader scheme`() {
        var textPrimary = Color.Unspecified
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            BibleReaderMaterialTheme(readerColorScheme = MidnightBlue) {
                textPrimary = BibleReaderTheme.colorScheme.textPrimary
                BibleReaderBanner(
                    bannerType = BibleReaderBannerType.OFFLINE,
                    isVisible = true,
                    onDismiss = {},
                )
            }
        }

        assertTrue(view.containsColor(composeTestRule.onNodeWithContentDescription("Offline"), textPrimary))
        assertTrue(view.containsColor(composeTestRule.onNodeWithContentDescription("Close"), textPrimary))
    }

    private fun View.containsColor(
        node: SemanticsNodeInteraction,
        color: Color,
    ): Boolean {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        val bounds = node.fetchSemanticsNode().boundsInRoot
        return (bounds.left.toInt() until bounds.right.toInt()).any { x ->
            (bounds.top.toInt() until bounds.bottom.toInt()).any { y -> bitmap.getPixel(x, y) == color.toArgb() }
        }
    }
}
