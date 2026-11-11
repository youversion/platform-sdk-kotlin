package com.youversion.platform.reader.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeScrollBehavior(
    override val state: PassageSelectionState,
) : PassageSelectionScrollBehavior {
    override val isPinned = false
    override val snapAnimationSpec: AnimationSpec<Float>? = null
    override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
    override val nestedScrollConnection = object : NestedScrollConnection {}
}

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class BibleReaderPassageSelectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders with book, chapter, and previous and next buttons`() {
        composeTestRule.setContent {
            BibleReaderPassageSelection(
                bookAndChapter = "Genesis 1",
                onReferenceClick = {},
                onPreviousChapter = {},
                onNextChapter = {},
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Previous chapter").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next chapter").assertIsDisplayed()
    }

    @Test
    fun `reference, previous chapter, and next chapter all trigger their respective onClicks`() {
        var referenceClicked = false
        var previousChapterClicked = false
        var nextChapterClicked = false
        composeTestRule.setContent {
            BibleReaderPassageSelection(
                bookAndChapter = "Genesis 1",
                onReferenceClick = { referenceClicked = true },
                onPreviousChapter = { previousChapterClicked = true },
                onNextChapter = { nextChapterClicked = true },
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").performClick()
        assertTrue(referenceClicked)

        composeTestRule.onNodeWithContentDescription("Previous chapter").performClick()
        assertTrue(previousChapterClicked)

        composeTestRule.onNodeWithContentDescription("Next chapter").performClick()
        assertTrue(nextChapterClicked)
    }

    @Test
    fun `buttons are disabled when alpha is at or below threshold`() {
        val state =
            PassageSelectionState(
                initialHeightOffsetLimit = -100f,
                initialHeightOffset = -90f,
                initialContentOffset = 0f,
            )
        composeTestRule.setContent {
            BibleReaderPassageSelection(
                bookAndChapter = "Genesis 1",
                onReferenceClick = {},
                onPreviousChapter = {},
                onNextChapter = {},
                scrollBehavior = FakeScrollBehavior(state),
            )
        }

        composeTestRule.onNodeWithContentDescription("Previous chapter").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Next chapter").assertIsNotEnabled()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [34], qualifiers = "xxhdpi")
    fun `previous and next arrows point outward in RTL`() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BibleReaderPassageSelection(
                    bookAndChapter = "Genesis 1",
                    onReferenceClick = {},
                    onPreviousChapter = {},
                    onNextChapter = {},
                )
            }
        }

        assertTrue(view.isArrowPointingRight(composeTestRule.onNodeWithContentDescription("Previous chapter")))
        assertFalse(view.isArrowPointingRight(composeTestRule.onNodeWithContentDescription("Next chapter")))
    }

    // A chevron's tip is the only ink on its middle row, so the side it falls on is the way it points.
    private fun View.isArrowPointingRight(node: SemanticsNodeInteraction): Boolean {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val y = bounds.center.y.toInt()
        val backgroundColor = bitmap.getPixel(bounds.left.toInt(), y)
        val inkColumns =
            (bounds.left.toInt() until bounds.right.toInt()).filter {
                bitmap.getPixel(it, y) !=
                    backgroundColor
            }
        assertTrue(inkColumns.isNotEmpty(), "no arrow drawn")
        return inkColumns.average() > bounds.center.x
    }
}
