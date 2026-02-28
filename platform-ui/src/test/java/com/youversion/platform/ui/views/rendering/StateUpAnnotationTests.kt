package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.text.SpanStyle
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.views.fromAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateUpAnnotationTests {
    private fun createStateUp(verse: Int = 1): StateUp =
        StateUp(
            rendering = true,
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
            verse = verse,
        )

    private fun annotationString(
        versionId: Int = 1,
        bookUSFM: String = "GEN",
        chapter: Int = 1,
        verse: Int,
    ): String = "$versionId:$bookUSFM:$chapter:$verse"

    @Test
    fun `append with SCRIPTURE category adds BibleReferenceAttribute`() {
        val stateUp = createStateUp(verse = 3)

        stateUp.append("In the beginning", SpanStyle(), BibleTextCategory.SCRIPTURE)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = result.length,
            )
        assertEquals(1, annotations.size)
        assertEquals(annotationString(verse = 3), annotations.first().item)
    }

    @Test
    fun `append with HEADER category does not add BibleReferenceAttribute`() {
        val stateUp = createStateUp(verse = 3)

        stateUp.append("The Creation", SpanStyle(), BibleTextCategory.HEADER)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = result.length,
            )
        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `append with HEADER category still adds BibleTextCategoryAttribute`() {
        val stateUp = createStateUp(verse = 3)

        stateUp.append("The Creation", SpanStyle(), BibleTextCategory.HEADER)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleTextCategoryAttribute.NAME,
                start = 0,
                end = result.length,
            )
        assertEquals(1, annotations.size)
        assertEquals(BibleTextCategory.HEADER.name, annotations.first().item)
    }

    @Test
    fun `append with VERSE_LABEL category adds BibleReferenceAttribute`() {
        val stateUp = createStateUp(verse = 5)

        stateUp.append("5 ", SpanStyle(), BibleTextCategory.VERSE_LABEL)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = result.length,
            )
        assertEquals(1, annotations.size)
        assertEquals(annotationString(verse = 5), annotations.first().item)
    }

    @Test
    fun `append with verse zero does not add BibleReferenceAttribute`() {
        val stateUp = createStateUp(verse = 0)

        stateUp.append("Some text", SpanStyle(), BibleTextCategory.SCRIPTURE)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = result.length,
            )
        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `header text after verse does not inherit verse BibleReferenceAttribute`() {
        val stateUp = createStateUp(verse = 3)

        stateUp.append("verse three text", SpanStyle(), BibleTextCategory.SCRIPTURE)
        stateUp.append("Section Heading", SpanStyle(), BibleTextCategory.HEADER)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = result.length,
            )

        assertEquals(1, annotations.size)
        assertEquals(0, annotations.first().start)
        assertEquals("verse three text".length, annotations.first().end)
    }

    @Test
    fun `multiple scripture appends for same verse produce separate annotations`() {
        val stateUp = createStateUp(verse = 2)

        stateUp.append("2 ", SpanStyle(), BibleTextCategory.VERSE_LABEL)
        stateUp.append("And the earth was", SpanStyle(), BibleTextCategory.SCRIPTURE)

        val result = stateUp.textBuilder.toAnnotatedString()
        val annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = result.length,
            )

        assertEquals(2, annotations.size)
        assertTrue(annotations.all { it.item == annotationString(verse = 2) })
    }

    @Test
    fun `consecutive verses produce distinct annotations`() {
        val stateUp = createStateUp(verse = 1)

        stateUp.append("In the beginning", SpanStyle(), BibleTextCategory.SCRIPTURE)
        stateUp.verse = 2
        stateUp.append("And the earth was", SpanStyle(), BibleTextCategory.SCRIPTURE)

        val result = stateUp.textBuilder.toAnnotatedString()
        val verse1Annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = "In the beginning".length,
            )
        val verse2Annotations =
            result.getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = "In the beginning".length,
                end = result.length,
            )

        assertEquals(annotationString(verse = 1), verse1Annotations.first().item)
        assertEquals(annotationString(verse = 2), verse2Annotations.first().item)
    }
}
