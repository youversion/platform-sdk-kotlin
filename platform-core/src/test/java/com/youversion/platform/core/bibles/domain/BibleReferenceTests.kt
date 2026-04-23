package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleReferenceTests {
    @Test
    fun `test init single verse`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 3)

        assertEquals(1, ref.versionId)
        assertEquals("GEN", ref.bookUSFM)
        assertEquals(2, ref.chapter)
        assertEquals(3, ref.verseStart)
        assertEquals(3, ref.verseEnd)
    }

    @Test
    fun `test init verse range`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 23, verseStart = 1, verseEnd = 2)

        assertEquals(1, ref.versionId)
        assertEquals("PSA", ref.bookUSFM)
        assertEquals(23, ref.chapter)
        assertEquals(1, ref.verseStart)
        assertEquals(2, ref.verseEnd)
    }

    // ----- Test isRange
    @Test
    fun `test isRange single verse`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 3, verse = 14)

        assertFalse(ref.isRange)
    }

    @Test
    fun `test isRange verse range`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 3, verseStart = 14, verseEnd = 15)

        assertTrue(ref.isRange)
    }

    // ----- Test compare function
    @Test
    fun `test compare same reference`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

        assertEquals(ref1, ref2)
    }

    @Test
    fun `test compare different books`() {
        // Note: Comparing different books is not reliable without knowing the canonical book order
        // This test just verifies the function doesn't crash with different book usfms
        val genRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val exoRef = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1, verse = 1)

        // Just verify the function can be called with different books
        // The actual comparison result is not validated
        BibleReference.compare(genRef, exoRef)
        BibleReference.compare(exoRef, genRef)

        // This test passes as long as the above lines don't crash
    }

    @Test
    fun `test compare same book different chapters`() {
        val gen1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val gen2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)

        assertTrue(gen1 < gen2)
        assertEquals(-1, BibleReference.compare(gen1, gen2)) // GEN 1 < GEN 2
        assertEquals(1, BibleReference.compare(gen2, gen1)) // GEN 2 > GEN 1
    }

    @Test
    fun `test compare same chapter different verses`() {
        val gen1_1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
        val gen1_2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 2)

        assertTrue(gen1_1 < gen1_2)
        assertTrue(gen1_2 > gen1_1)
    }

    @Test
    fun `test compare ranges`() {
        val gen1_1to3 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val gen1_2to4 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 2, verseEnd = 4)

        // Compare based on starting verse first
        assertTrue(gen1_1to3 < gen1_2to4)

        // Same starting verse, compare end verse
        val gen1_1to3_2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val gen1_1to4 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 4)

        assertTrue(gen1_1to3_2 < gen1_1to4) // 1-3 < 1-4
    }

    @Test
    fun `test sorting`() {
        val refs =
            listOf(
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 4, verse = 3),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3, verse = 2),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 2),
            )

        val sorted = refs.sorted()

        assertEquals("GEN", sorted[0].bookUSFM)
        assertEquals(1, sorted[0].chapter)
        assertEquals(1, sorted[0].verseStart)

        assertEquals("GEN", sorted[1].bookUSFM)
        assertEquals(1, sorted[1].chapter)
        assertEquals(2, sorted[1].verseStart)

        assertEquals("GEN", sorted[2].bookUSFM)
        assertEquals(2, sorted[2].chapter)
        assertEquals(1, sorted[2].verseStart)

        assertEquals("GEN", sorted[3].bookUSFM)
        assertEquals(3, sorted[3].chapter)
        assertEquals(2, sorted[3].verseStart)

        assertEquals("GEN", sorted[4].bookUSFM)
        assertEquals(4, sorted[4].chapter)
        assertEquals(3, sorted[4].verseStart)
    }

    // ----- Test chapterUSFM
    @Test
    fun `test chapterUSFM`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verse = 1)

        assertEquals("GEN.1", ref.chapterUSFM)
    }

    // ----- Test asUSFM
    @Test
    fun `test asUSFM`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1)
        assertEquals("GEN.1", ref1.asUSFM)

        val ref2 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verse = 1)
        assertEquals("GEN.1.1", ref2.asUSFM)

        val ref3 = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1, verseStart = 1, verseEnd = 5)
        assertEquals("GEN.1.1-GEN.1.5", ref3.asUSFM)
    }

    @Test
    fun `test asUSFM when verseStart is null and verseEnd is set`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = null, verseEnd = 5)

        assertEquals("GEN.1", ref.asUSFM)
    }

    @Test
    fun `test asUSFM when verseEnd is null and verseStart is set`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = null)

        assertEquals("GEN.1.3", ref.asUSFM)
    }

    @Test
    fun `test asUSFM when verseStart equals verseEnd`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 3)

        assertEquals("GEN.1.3", ref.asUSFM)
    }

    // ----- Test merging references
    @Test
    fun `test merge adjacent references`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 4, verseEnd = 6)

        val merged1To2 = BibleReference.referenceByMerging(ref1, ref2)
        assertEquals(1, merged1To2.versionId)
        assertEquals("GEN", merged1To2.bookUSFM)
        assertEquals(1, merged1To2.chapter)
        assertEquals(1, merged1To2.verseStart)
        assertEquals(6, merged1To2.verseEnd)

        // show that order does not matter
        val merged2To1 = BibleReference.referenceByMerging(ref2, ref1)
        assertEquals(1, merged2To1.versionId)
        assertEquals("GEN", merged2To1.bookUSFM)
        assertEquals(1, merged2To1.chapter)
        assertEquals(1, merged2To1.verseStart)
        assertEquals(6, merged2To1.verseEnd)
    }

    @Test
    fun `test merge overlapping references`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 7)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 9)

        val merged1To2 = BibleReference.referenceByMerging(ref1, ref2)
        assertEquals(1, merged1To2.versionId)
        assertEquals("GEN", merged1To2.bookUSFM)
        assertEquals(1, merged1To2.chapter)
        assertEquals(3, merged1To2.verseStart)
        assertEquals(9, merged1To2.verseEnd)

        // show that order does not matter
        val merged2To1 = BibleReference.referenceByMerging(ref2, ref1)
        assertEquals(1, merged2To1.versionId)
        assertEquals("GEN", merged2To1.bookUSFM)
        assertEquals(1, merged2To1.chapter)
        assertEquals(3, merged2To1.verseStart)
        assertEquals(9, merged2To1.verseEnd)
    }

    @Test
    fun `test merge one reference contains another`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 2, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 7)

        val merged1To2 = BibleReference.referenceByMerging(ref1, ref2)
        assertEquals(1, merged1To2.versionId)
        assertEquals("GEN", merged1To2.bookUSFM)
        assertEquals(1, merged1To2.chapter)
        assertEquals(2, merged1To2.verseStart)
        assertEquals(10, merged1To2.verseEnd)

        // show that order does not matter
        val merged2To1 = BibleReference.referenceByMerging(ref2, ref1)
        assertEquals(1, merged2To1.versionId)
        assertEquals("GEN", merged2To1.bookUSFM)
        assertEquals(1, merged2To1.chapter)
        assertEquals(2, merged2To1.verseStart)
        assertEquals(10, merged2To1.verseEnd)
    }

    @Test
    fun `test merge multiple groups of references`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 4, verseStart = 2, verseEnd = 10)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 2, verseStart = 5, verseEnd = 7)
        val ref3 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 4, verseStart = 5, verseEnd = 12)
        val ref4 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 2, verseStart = 8, verseEnd = 11)
        val ref5 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 1, verseStart = 5, verseEnd = 7)

        val result = BibleReference.referencesByMerging(listOf(ref1, ref2, ref3, ref4, ref5))
        assertEquals(3, result.size)

        val singleChapter1 = result.first { it.chapter == 1 }
        assertEquals(1, singleChapter1.chapter)
        assertEquals(5, singleChapter1.verseStart)
        assertEquals(7, singleChapter1.verseEnd)

        val mergedChapter2 = result.first { it.chapter == 2 }
        assertEquals(2, mergedChapter2.chapter)
        assertEquals(5, mergedChapter2.verseStart)
        assertEquals(11, mergedChapter2.verseEnd)

        val mergedChapter4 = result.first { it.chapter == 4 }
        assertEquals(4, mergedChapter4.chapter)
        assertEquals(2, mergedChapter4.verseStart)
        assertEquals(12, mergedChapter4.verseEnd)
    }

    // ----- Test init validation
    @Test
    fun `test init throws when chapter is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 0, verse = 1)
        }
    }

    @Test
    fun `test init throws when verseStart is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 0, verseEnd = 3)
        }
    }

    @Test
    fun `test init throws when verseEnd is less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 0)
        }
    }

    @Test
    fun `test init throws when verseStart is greater than verseEnd`() {
        assertFailsWith<IllegalArgumentException> {
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 3)
        }
    }

    @Test
    fun `test init allows verseEnd without verseStart`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = null, verseEnd = 5)

        assertNull(ref.verseStart)
        assertEquals(5, ref.verseEnd)
    }

    // ----- Test isRange additional
    @Test
    fun `test isRange returns true when verseEnd is not null and verseStart does not equal verseEnd`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = null, verseEnd = 5)

        assertTrue(ref.isRange)
    }

    @Test
    fun `test isRange returns false when verseEnd is null`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = null)

        assertFalse(ref.isRange)
    }

    @Test
    fun `test isRange returns false when verseStart equals verseEnd`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 3)

        assertFalse(ref.isRange)
    }

    // ----- Test toString
    @Test
    fun `test toString with verseStart but no verseEnd`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = null)

        assertEquals("bible1__GEN.1.3", ref.toString())
    }

    @Test
    fun `test toString with no verseStart or verseEnd`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        assertEquals("bible1__GEN.1", ref.toString())
    }

    @Test
    fun `test toString when verseStart equals verseEnd`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 3)

        assertEquals("bible1__GEN.1.3", ref.toString())
    }

    @Test
    fun `test toString when verseStart is null but verseEnd is set`() {
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = null, verseEnd = 5)

        assertEquals("bible1__GEN.1", ref.toString())
    }

    // ----- Test compare additional
    @Test
    fun `test compare a verseEnd null b verseEnd set`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = null)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)

        assertEquals(1, BibleReference.compare(a, b))
    }

    @Test
    fun `test compare a verseEnd set b verseEnd null`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = null)

        assertEquals(-1, BibleReference.compare(a, b))
    }

    @Test
    fun `test compare both verseEnd null`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = null)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = null)

        assertEquals(0, BibleReference.compare(a, b))
    }

    // ----- Test referenceByMerging additional
    @Test
    fun `test referenceByMerging throws when not adjacent or overlapping`() {
        val ref1 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 3)
        val ref2 = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 5, verseEnd = 7)

        assertFailsWith<IllegalArgumentException> {
            BibleReference.referenceByMerging(ref1, ref2)
        }
    }

    @Test
    fun `test referenceByMerging returns a when a verseStart is null`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 5)

        val merged = BibleReference.referenceByMerging(a, b)

        assertEquals(a, merged)
    }

    @Test
    fun `test referenceByMerging returns b when b verseStart is null`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 5)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        val merged = BibleReference.referenceByMerging(a, b)

        assertEquals(b, merged)
    }

    @Test
    fun `test referenceByMerging when a verseEnd is null`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = null)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 4, verseEnd = 6)

        val merged = BibleReference.referenceByMerging(a, b)

        assertEquals(3, merged.verseStart)
        assertEquals(6, merged.verseEnd)
    }

    @Test
    fun `test referenceByMerging when b verseEnd is null`() {
        val a = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)
        val b = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 6, verseEnd = null)

        val merged = BibleReference.referenceByMerging(a, b)

        assertEquals(3, merged.verseStart)
        assertEquals(6, merged.verseEnd)
    }

    // ----- Test existsIn

    @Test
    fun `test existsIn returns false when book not in version`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("EXO", listOf(chapter(id = "1")))),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        assertFalse(ref.existsIn(version))
    }

    @Test
    fun `test existsIn returns false when version has null books`() {
        val version = BibleVersion(id = 1, books = null)
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        assertFalse(ref.existsIn(version))
    }

    @Test
    fun `test existsIn returns false when version has empty books`() {
        val version = BibleVersion(id = 1, books = emptyList())
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        assertFalse(ref.existsIn(version))
    }

    @Test
    fun `test existsIn matches book USFM case-insensitively`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", listOf(chapter(id = "1")))),
            )
        assertTrue(BibleReference(versionId = 1, bookUSFM = "gen", chapter = 1).existsIn(version))
        assertTrue(BibleReference(versionId = 1, bookUSFM = "GeN", chapter = 1).existsIn(version))
    }

    @Test
    fun `test existsIn returns true optimistically when book lookup fails despite USFM match`() {
        // bookUSFMs does a case-insensitive contains check, but version.book() does an exact match.
        // When stored USFM case differs, book() returns null and existsIn short-circuits to true.
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("gen", listOf(chapter(id = "1")))),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 99)
        assertTrue(ref.existsIn(version))
    }

    @Test
    fun `test existsIn returns true optimistically when book has null chapters`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", null)),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 999)
        assertTrue(ref.existsIn(version))
    }

    @Test
    fun `test existsIn matches chapter by id`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", listOf(chapter(id = "3")))),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3)
        assertTrue(ref.existsIn(version))
    }

    @Test
    fun `test existsIn matches chapter by title`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", listOf(chapter(title = "3")))),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3)
        assertTrue(ref.existsIn(version))
    }

    @Test
    fun `test existsIn matches chapter by passageId using chapterUSFM`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", listOf(chapter(passageId = "GEN.3")))),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 3)
        assertTrue(ref.existsIn(version))
    }

    @Test
    fun `test existsIn matches chapter by passageId when reference book is lowercase`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", listOf(chapter(passageId = "GEN.3")))),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "gen", chapter = 3)
        assertTrue(ref.existsIn(version))
    }

    @Test
    fun `test existsIn returns false when chapter not found`() {
        val version =
            BibleVersion(
                id = 1,
                books =
                    listOf(
                        bookWithChapters(
                            "GEN",
                            listOf(
                                chapter(id = "1", passageId = "GEN.1", title = "1"),
                                chapter(id = "2", passageId = "GEN.2", title = "2"),
                            ),
                        ),
                    ),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 999)
        assertFalse(ref.existsIn(version))
    }

    @Test
    fun `test existsIn returns false when book has empty chapters`() {
        val version =
            BibleVersion(
                id = 1,
                books = listOf(bookWithChapters("GEN", emptyList())),
            )
        val ref = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)
        assertFalse(ref.existsIn(version))
    }

    @Test
    fun `test existsIn ignores verseStart and verseEnd`() {
        val version =
            BibleVersion(
                id = 1,
                books =
                    listOf(
                        bookWithChapters(
                            "GEN",
                            listOf(chapter(id = "1", passageId = "GEN.1", title = "1")),
                        ),
                    ),
            )
        assertTrue(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1).existsIn(version))
        assertTrue(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 500).existsIn(version))
        assertTrue(
            BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 1, verseEnd = 999).existsIn(version),
        )
    }

    @Test
    fun `test existsIn with multiple books only matches correct chapters`() {
        val version =
            BibleVersion(
                id = 1,
                books =
                    listOf(
                        bookWithChapters(
                            "GEN",
                            listOf(
                                chapter(id = "1", passageId = "GEN.1", title = "1"),
                                chapter(id = "2", passageId = "GEN.2", title = "2"),
                            ),
                        ),
                        bookWithChapters(
                            "EXO",
                            listOf(chapter(id = "1", passageId = "EXO.1", title = "1")),
                        ),
                    ),
            )
        assertTrue(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2).existsIn(version))
        assertTrue(BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1).existsIn(version))
        assertFalse(BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 2).existsIn(version))
    }

    private fun chapter(
        id: String? = null,
        passageId: String? = null,
        title: String? = null,
    ): BibleChapter =
        BibleChapter(
            id = id,
            passageId = passageId,
            title = title,
            verses = null,
        )

    private fun bookWithChapters(
        usfm: String?,
        chapters: List<BibleChapter>?,
    ): BibleBook =
        BibleBook(
            id = usfm,
            title = usfm,
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = chapters,
        )
}
