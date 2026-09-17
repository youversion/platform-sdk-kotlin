package com.youversion.platform.reader.sheets

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Cream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BibleReaderSearchSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val query = mutableStateOf("")

    /** Renders the sheet with its query hoisted into [query], the way the search view model holds it. */
    private fun renderSheet(
        onDismissRequest: () -> Unit = {},
        onSubmit: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme(readerColorScheme = Cream) {
                BibleReaderSearchSheet(
                    onDismissRequest = onDismissRequest,
                    onQueryChange = { query.value = it },
                    onSubmit = onSubmit,
                    query = query.value,
                )
            }
        }
    }

    @Test
    fun `the field is focused and ready to type into`() {
        renderSheet()

        composeTestRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun `typing reports the query`() {
        renderSheet()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        assertEquals("Corinthians", query.value)
    }

    @Test
    fun `the clear button carries the reader's own label and empties the query`() {
        renderSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).performClick()

        assertEquals("", query.value)
    }

    @Test
    fun `the action key offers search`() {
        renderSheet()

        composeTestRule.onNode(hasImeAction(ImeAction.Search)).assertIsDisplayed()
    }

    @Test
    fun `typing past one hundred characters is refused`() {
        renderSheet()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("a".repeat(100))
        assertEquals("a".repeat(100), query.value)

        composeTestRule.onNode(hasSetTextAction()).performTextInput("b")

        assertEquals("a".repeat(100), query.value)
    }

    @Test
    fun `Done dismisses the sheet`() {
        var isDismissed = false
        renderSheet(onDismissRequest = { isDismissed = true })

        composeTestRule.onNodeWithText("Done").performClick()

        composeTestRule.waitUntil { isDismissed }
    }

    private companion object {
        /** The value of the reader's `cancel` key, which the clear button is labelled with. */
        const val CLEAR_LABEL = "Cancel"
    }
}
