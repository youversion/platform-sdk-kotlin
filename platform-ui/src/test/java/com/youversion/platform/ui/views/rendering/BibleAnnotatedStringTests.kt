package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.buildAnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleAnnotatedStringTests {
    @Test
    fun `trailing spaces are trimmed`() {
        val annotatedString = buildAnnotatedString { append("hello   ") }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("hello", result.text)
    }

    @Test
    fun `trailing newlines are trimmed`() {
        val annotatedString = buildAnnotatedString { append("hello\n\n") }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("hello", result.text)
    }

    @Test
    fun `mixed trailing whitespace is trimmed`() {
        val annotatedString = buildAnnotatedString { append("hello \n \t") }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("hello", result.text)
    }

    @Test
    fun `string with no trailing whitespace is unchanged`() {
        val annotatedString = buildAnnotatedString { append("hello") }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("hello", result.text)
    }

    @Test
    fun `empty string returns empty`() {
        val annotatedString = buildAnnotatedString { }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("", result.text)
    }

    @Test
    fun `string that is only whitespace returns empty`() {
        val annotatedString = buildAnnotatedString { append("   ") }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("", result.text)
    }

    @Test
    fun `annotations are preserved after trimming`() {
        val annotatedString =
            buildAnnotatedString {
                append("hello")
                addStringAnnotation(
                    tag = BibleTextCategoryAttribute.NAME,
                    annotation = BibleTextCategory.SCRIPTURE.name,
                    start = 0,
                    end = 5,
                )
                append("   ")
            }

        val result = annotatedString.trimTrailingWhitespace()

        assertEquals("hello", result.text)
        val annotations =
            result.getStringAnnotations(
                tag = BibleTextCategoryAttribute.NAME,
                start = 0,
                end = result.length,
            )
        assertEquals(1, annotations.size)
        assertEquals(BibleTextCategory.SCRIPTURE.name, annotations.first().item)
        assertEquals(0, annotations.first().start)
        assertEquals(5, annotations.first().end)
    }
}
