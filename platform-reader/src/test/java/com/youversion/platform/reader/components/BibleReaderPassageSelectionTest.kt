package com.youversion.platform.reader.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

private class FakeScrollBehavior(
    override val state: PassageSelectionState,
) : PassageSelectionScrollBehavior {
    override val isPinned = false
    override val snapAnimationSpec: AnimationSpec<Float>? = null
    override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
    override val nestedScrollConnection = object : NestedScrollConnection {}
}

@ExperimentalMaterial3Api
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
        composeTestRule.onNodeWithContentDescription("Previous Chapter").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Next Chapter").assertIsDisplayed()
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

        composeTestRule.onNodeWithContentDescription("Previous Chapter").performClick()
        assertTrue(previousChapterClicked)

        composeTestRule.onNodeWithContentDescription("Next Chapter").performClick()
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

        composeTestRule.onNodeWithContentDescription("Previous Chapter").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Next Chapter").assertIsNotEnabled()
    }
}
