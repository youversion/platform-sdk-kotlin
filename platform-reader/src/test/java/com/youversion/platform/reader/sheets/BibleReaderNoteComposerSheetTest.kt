package com.youversion.platform.reader.sheets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.domain.NoteDraft
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class BibleReaderNoteComposerSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var draft by mutableStateOf<NoteDraft?>(
        NoteDraft(listOf(BibleReference(111, "JHN", 3, 16)), emptySet(), "John 3:16", "user:a"),
    )

    private fun renderComposer() {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                draft?.let { current ->
                    BibleReaderNoteComposerSheet(
                        draft = current,
                        onUpdate = { content, isPublic, hexColor ->
                            draft = current.edited(content, isPublic, hexColor)
                        },
                        onSave = {},
                        onDismissRequest = {
                            draft = if (current.hasChanges) current.copy(showsDiscardConfirmation = true) else null
                        },
                        onConfirmDiscard = { draft = null },
                        onCancelDiscard = { draft = current.copy(showsDiscardConfirmation = false) },
                        onDismissSaveError = {},
                    )
                }
            }
        }
    }

    @Test
    fun `swiping away an edited note asks to discard and keeps the composer`() {
        renderComposer()
        composeTestRule.onNodeWithTag("note_content_field").performTextInput("Born again")

        composeTestRule.onNodeWithTag("note_composer_sheet").performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Discard note?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("note_content_field").assertIsDisplayed()
        assertEquals("Born again", assertNotNull(draft).content)
    }

    @Test
    fun `cancelling discard leaves the composer editable`() {
        renderComposer()
        composeTestRule.onNodeWithTag("note_content_field").performTextInput("Born")
        composeTestRule.onNodeWithTag("note_composer_sheet").performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("Cancel") and hasAnySibling(hasText("Discard"))).performClick()
        composeTestRule.onNodeWithTag("note_content_field").performTextInput(" again")

        composeTestRule.onNodeWithText("Discard note?").assertDoesNotExist()
        assertEquals("Born again", assertNotNull(draft).content)
    }

    @Test
    fun `swiping away an unedited note closes it`() {
        renderComposer()

        composeTestRule.onNodeWithTag("note_composer_sheet").performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        assertNull(draft)
    }
}
