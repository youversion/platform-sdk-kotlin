package com.youversion.platform.ui.views

import androidx.compose.ui.text.font.FontWeight
import org.junit.Test
import kotlin.test.assertEquals

class BoldMarkdownTest {
    @Test
    fun `text with no markers renders plain`() {
        val result = boldMarkdownAnnotatedString("no emphasis here")

        assertEquals("no emphasis here", result.text)
        assertEquals(emptyList(), result.spanStyles)
    }

    @Test
    fun `paired markers become a bold span over the enclosed text only`() {
        val result = boldMarkdownAnnotatedString("connect to your **Bible App** account")

        assertEquals("connect to your Bible App account", result.text)
        val boldRange = result.spanStyles.single()
        assertEquals(FontWeight.Bold, boldRange.item.fontWeight)
        assertEquals("Bible App", result.text.substring(boldRange.start, boldRange.end))
    }

    @Test
    fun `several bold runs are each emphasized`() {
        val result = boldMarkdownAnnotatedString("**YouVersion** and **Bible Project**")

        assertEquals("YouVersion and Bible Project", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals(
            listOf("YouVersion", "Bible Project"),
            result.spanStyles.map { result.text.substring(it.start, it.end) },
        )
    }
}
