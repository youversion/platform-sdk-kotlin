package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleTextBlockTests {
    private val fixedId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private fun block(
        id: UUID = fixedId,
        text: String = "text",
        chapter: Int = 1,
        rows: List<List<AnnotatedString>> = emptyList(),
        firstLineHeadIndent: Int = 1,
        headIndent: Int = 2,
        marginTop: Dp = 3.dp,
        marginBottom: Dp = 4.dp,
        alignment: TextAlign = TextAlign.Start,
        footnotes: List<AnnotatedString> = emptyList(),
    ) = BibleTextBlock(
        id = id,
        text = AnnotatedString(text),
        chapter = chapter,
        rows = rows,
        firstLineHeadIndent = firstLineHeadIndent,
        headIndent = headIndent,
        marginTop = marginTop,
        marginBottom = marginBottom,
        alignment = alignment,
        footnotes = footnotes,
    )

    @Test
    fun `test blocks with equal values are equal`() {
        assertEquals(block(), block())
        assertEquals(block().hashCode(), block().hashCode())
    }

    @Test
    fun `test blocks differing in any value are not equal`() {
        val cell = AnnotatedString("cell")
        listOf(
            block(id = UUID.fromString("00000000-0000-0000-0000-000000000002")),
            block(text = "other"),
            block(chapter = 2),
            block(rows = listOf(listOf(cell))),
            block(firstLineHeadIndent = 9),
            block(headIndent = 9),
            block(marginTop = 9.dp),
            block(marginBottom = 9.dp),
            block(alignment = TextAlign.Center),
            block(footnotes = listOf(cell)),
        ).forEach { assertNotEquals(block(), it) }
    }

    @Test
    fun `test blocks with default ids are different blocks`() {
        val first =
            BibleTextBlock(
                id = UUID.randomUUID(),
                rows = emptyList(),
                text = AnnotatedString("text"),
                chapter = 1,
                firstLineHeadIndent = 0,
                headIndent = 0,
                marginTop = 0.dp,
                marginBottom = 0.dp,
                alignment = TextAlign.Start,
                footnotes = emptyList(),
            )
        val second =
            BibleTextBlock(
                id = UUID.randomUUID(),
                rows = emptyList(),
                text = AnnotatedString("text"),
                chapter = 1,
                firstLineHeadIndent = 0,
                headIndent = 0,
                marginTop = 0.dp,
                marginBottom = 0.dp,
                alignment = TextAlign.Start,
                footnotes = emptyList(),
            )
        assertNotEquals(first, second)
    }

    @Test
    fun `test unspecified margins compare equal so equal blocks always hash alike`() {
        val first = block(marginTop = Dp.Unspecified, marginBottom = Dp.Unspecified)
        val second = block(marginTop = Dp.Unspecified, marginBottom = Dp.Unspecified)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "BibleTextBlock(id=$fixedId, text=text, chapter=1, rows=[], firstLineHeadIndent=1, headIndent=2, " +
                "marginTop=${3.dp}, marginBottom=${4.dp}, alignment=${TextAlign.Start}, footnotes=[])",
            block().toString(),
        )
    }
}
