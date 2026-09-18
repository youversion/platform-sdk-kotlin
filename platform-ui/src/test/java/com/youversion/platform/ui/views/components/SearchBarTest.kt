package com.youversion.platform.ui.views.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SearchBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val query = mutableStateOf("")

    /** Renders the field with its query hoisted into [query], the way a real screen holds it. */
    private fun setContent(
        onSubmit: (() -> Unit)? = null,
        showsClearButton: Boolean = false,
        maximumGraphemeClusterCount: Int? = null,
    ) {
        composeTestRule.setContent {
            SearchBar(
                query = query.value,
                onQueryChange = { query.value = it },
                onSubmit = onSubmit,
                showsClearButton = showsClearButton,
                maximumGraphemeClusterCount = maximumGraphemeClusterCount,
            )
        }
    }

    // ----- Clear button

    @Test
    fun `clear button appears only once there is text`() {
        setContent(showsClearButton = true)

        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertDoesNotExist()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertIsDisplayed()
    }

    @Test
    fun `clear button empties the query`() {
        setContent(showsClearButton = true)
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).performClick()

        assertEquals("", query.value)
        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertDoesNotExist()
    }

    @Test
    fun `the clear button takes the label it is given`() {
        composeTestRule.setContent {
            SearchBar(
                query = query.value,
                onQueryChange = { query.value = it },
                showsClearButton = true,
                clearButtonContentDescription = "Cancel",
            )
        }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        composeTestRule.onNodeWithContentDescription("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertDoesNotExist()
    }

    @Test
    fun `there is no clear button by default`() {
        setContent()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertDoesNotExist()
    }

    // ----- Maximum length

    @Test
    fun `typing past the maximum is refused`() {
        setContent(maximumGraphemeClusterCount = 3)

        composeTestRule.onNode(hasSetTextAction()).performTextInput("abc")
        assertEquals("abc", query.value)

        composeTestRule.onNode(hasSetTextAction()).performTextInput("d")
        assertEquals("abc", query.value)
    }

    /** Two letters but four code units, so a cap counting `length` would refuse what this one accepts. */
    @Test
    fun `the maximum counts a combined letter as one character`() {
        assertEquals(4, COMBINED_LETTERS.length)
        setContent(maximumGraphemeClusterCount = 2)

        composeTestRule.onNode(hasSetTextAction()).performTextInput(COMBINED_LETTERS)
        assertEquals(COMBINED_LETTERS, query.value)

        composeTestRule.onNode(hasSetTextAction()).performTextInput("x")
        assertEquals(COMBINED_LETTERS, query.value)
    }

    @Test
    fun `there is no maximum by default`() {
        setContent()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("a".repeat(500))

        assertEquals("a".repeat(500), query.value)
    }

    // ----- Action key

    @Test
    fun `a submit callback gives the field a search action key`() {
        var submissions = 0
        setContent(onSubmit = { submissions++ })

        composeTestRule.onNode(hasImeAction(ImeAction.Search)).performImeAction()

        assertEquals(1, submissions)
    }

    @Test
    fun `without a submit callback the field keeps the default action key`() {
        setContent()

        composeTestRule.onNode(hasImeAction(ImeAction.Search)).assertDoesNotExist()
    }

    private companion object {
        /** The value of `R.string.clear_search`, which the clear button is labelled with. */
        const val CLEAR_LABEL = "Clear search"

        /** `e` followed by a combining acute accent, twice. */
        const val COMBINED_LETTERS = "e\u0301e\u0301"
    }
}
