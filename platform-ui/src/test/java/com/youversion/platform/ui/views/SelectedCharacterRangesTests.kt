package com.youversion.platform.ui.views

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectedCharacterRangesTests {
    private fun buildAnnotatedText(
        text: String,
        versionId: Int = 1,
        bookUSFM: String = "GEN",
        chapter: Int = 1,
        verse: Int,
    ): AnnotatedString =
        buildAnnotatedString {
            append(text)
            addStringAnnotation(
                tag = BibleReferenceAttribute.NAME,
                annotation = "$versionId:$bookUSFM:$chapter:$verse",
                start = 0,
                end = text.length,
            )
        }

    private fun createReference(
        versionId: Int = 1,
        bookUSFM: String = "GEN",
        chapter: Int = 1,
        verse: Int,
    ): BibleReference =
        BibleReference(
            versionId = versionId,
            bookUSFM = bookUSFM,
            chapter = chapter,
            verse = verse,
        )

    @Test
    fun `returns empty list when selectedVerses is empty`() {
        val text = buildAnnotatedText("In the beginning", verse = 1)

        val result = text.selectedCharacterRanges(emptySet())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when no annotations match selectedVerses`() {
        val text = buildAnnotatedText("In the beginning", verse = 1)
        val selectedVerses = setOf(createReference(verse = 2))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns range when annotation matches selected verse`() {
        val text = buildAnnotatedText("In the beginning", verse = 1)
        val selectedVerses = setOf(createReference(verse = 1))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertEquals(1, result.size)
        assertEquals(0 until 16, result.first())
    }

    @Test
    fun `merges multiple annotations for same verse into single range`() {
        val text =
            buildAnnotatedString {
                append("3 ")
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "1:EZR:2:3",
                    start = 0,
                    end = 2,
                )
                append("the descendants of Parosh")
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "1:EZR:2:3",
                    start = 2,
                    end = 27,
                )
            }
        val selectedVerses = setOf(createReference(bookUSFM = "EZR", chapter = 2, verse = 3))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertEquals(1, result.size)
        assertEquals(0 until 27, result.first())
    }

    @Test
    fun `does not match when versionId differs`() {
        val text = buildAnnotatedText("In the beginning", versionId = 1, verse = 1)
        val selectedVerses = setOf(createReference(versionId = 2, verse = 1))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `does not match when bookUSFM differs`() {
        val text = buildAnnotatedText("In the beginning", bookUSFM = "GEN", verse = 1)
        val selectedVerses = setOf(createReference(bookUSFM = "EXO", verse = 1))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `does not match when chapter differs`() {
        val text = buildAnnotatedText("In the beginning", chapter = 1, verse = 1)
        val selectedVerses = setOf(createReference(chapter = 2, verse = 1))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns range for table cell with single annotation`() {
        val text = buildAnnotatedText("2,172", versionId = 111, bookUSFM = "EZR", chapter = 2, verse = 3)
        val selectedVerses = setOf(createReference(versionId = 111, bookUSFM = "EZR", chapter = 2, verse = 3))

        val result = text.selectedCharacterRanges(selectedVerses)

        assertEquals(1, result.size)
        assertEquals(0 until 5, result.first())
    }
}
