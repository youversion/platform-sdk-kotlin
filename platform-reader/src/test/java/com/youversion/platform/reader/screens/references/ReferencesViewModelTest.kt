package com.youversion.platform.reader.screens.references

import androidx.lifecycle.ViewModelProvider
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleBookIntro
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReferencesViewModelTest {
    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private fun createChapters(count: Int): List<BibleChapter> =
        (1..count).map {
            BibleChapter(
                id = "ch$it",
                passageId = "p$it",
                title = "Chapter $it",
                verses = listOf(BibleVerse(id = "v1", passageId = "p1", title = "1")),
            )
        }

    private val genesisBook =
        BibleBook(
            id = "GEN",
            title = "Genesis",
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = createChapters(50),
        )

    private val exodusBook =
        BibleBook(
            id = "EXO",
            title = "Exodus",
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = createChapters(40),
        )

    private val leviticusBook =
        BibleBook(
            id = "LEV",
            title = "Leviticus",
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = createChapters(27),
            intro =
                BibleBookIntro(
                    id = "LEV_INTRO",
                    passageId = "LEV.INTRO1",
                    title = "Introduction to Leviticus",
                ),
        )

    private val testVersion =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            bookCodes = listOf("GEN", "EXO", "LEV"),
            books = listOf(genesisBook, exodusBook, leviticusBook),
        )

    // ----- init

    @Test
    fun `init maps bookCodes to ReferenceRow list with bookName and chapters`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)
        val rows = viewModel.state.value.referenceRows

        assertEquals(3, rows.size)

        assertEquals("GEN", rows[0].bookCode)
        assertEquals("Genesis", rows[0].bookName)
        assertEquals(50, rows[0].chapters.size)
        assertNull(rows[0].introPassageId)

        assertEquals("EXO", rows[1].bookCode)
        assertEquals("Exodus", rows[1].bookName)
        assertEquals(40, rows[1].chapters.size)
        assertNull(rows[1].introPassageId)

        assertEquals("LEV", rows[2].bookCode)
        assertEquals("Leviticus", rows[2].bookName)
        assertEquals(27, rows[2].chapters.size)
        assertEquals("LEV.INTRO1", rows[2].introPassageId)
    }

    @Test
    fun `init sets expandedBookCode to current bibleReference bookUSFM`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        assertEquals("GEN", viewModel.state.value.expandedBookCode)
    }

    @Test
    fun `init handles null bookCodes with empty rows list`() {
        val versionWithNullBookCodes = BibleVersion(id = 1, abbreviation = "KJV", bookCodes = null)
        val viewModel = ReferencesViewModel(versionWithNullBookCodes, defaultReference)

        assertEquals(0, viewModel.state.value.referenceRows.size)
    }

    // ----- expandBook

    @Test
    fun `expandBook expands a book when it is not currently expanded`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)
        assertEquals("GEN", viewModel.state.value.expandedBookCode)

        viewModel.expandBook("EXO")

        assertEquals("EXO", viewModel.state.value.expandedBookCode)
    }

    @Test
    fun `expandBook collapses the book when the same book is tapped again`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)
        assertEquals("GEN", viewModel.state.value.expandedBookCode)

        viewModel.expandBook("GEN")

        assertNull(viewModel.state.value.expandedBookCode)
    }

    @Test
    fun `expandBook expands a book when no book is currently expanded`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)
        viewModel.expandBook("GEN")
        assertNull(viewModel.state.value.expandedBookCode)

        viewModel.expandBook("GEN")

        assertEquals("GEN", viewModel.state.value.expandedBookCode)
    }

    // ----- onSearchQueryChange

    @Test
    fun `onSearchQueryChange updates searchQuery in state`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        viewModel.onSearchQueryChange("exodus")

        assertEquals("exodus", viewModel.state.value.searchQuery)
    }

    // ----- isSearchActive

    @Test
    fun `isSearchActive returns true when searchQuery is not blank`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        viewModel.onSearchQueryChange("gen")

        assertTrue(viewModel.state.value.isSearchActive)
    }

    @Test
    fun `isSearchActive returns false when searchQuery is blank`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        assertFalse(viewModel.state.value.isSearchActive)
    }

    @Test
    fun `isSearchActive returns false when searchQuery is only whitespace`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        viewModel.onSearchQueryChange("   ")

        assertFalse(viewModel.state.value.isSearchActive)
    }

    // ----- filteredReferenceRows

    @Test
    fun `filteredReferenceRows returns all rows when search query is blank`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        assertEquals(3, viewModel.state.value.filteredReferenceRows.size)
    }

    @Test
    fun `filteredReferenceRows filters rows by bookName case insensitively`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        viewModel.onSearchQueryChange("exo")

        val filtered = viewModel.state.value.filteredReferenceRows
        assertEquals(1, filtered.size)
        assertEquals("EXO", filtered[0].bookCode)
    }

    @Test
    fun `filteredReferenceRows returns multiple matches`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        viewModel.onSearchQueryChange("e")

        val filtered = viewModel.state.value.filteredReferenceRows
        assertEquals(3, filtered.size)
        assertEquals("GEN", filtered[0].bookCode)
        assertEquals("EXO", filtered[1].bookCode)
        assertEquals("LEV", filtered[2].bookCode)
    }

    @Test
    fun `filteredReferenceRows returns empty list when no books match`() {
        val viewModel = ReferencesViewModel(testVersion, defaultReference)

        viewModel.onSearchQueryChange("zzz")

        assertEquals(0, viewModel.state.value.filteredReferenceRows.size)
    }

    @Test
    fun `filteredReferenceRows excludes rows with null bookName when search is active`() {
        val versionWithNullBookName =
            BibleVersion(
                id = 1,
                abbreviation = "KJV",
                bookCodes = listOf("GEN", "UNKNOWN"),
                books = listOf(genesisBook),
            )
        val viewModel = ReferencesViewModel(versionWithNullBookName, defaultReference)
        assertEquals(2, viewModel.state.value.referenceRows.size)

        viewModel.onSearchQueryChange("gen")

        val filtered = viewModel.state.value.filteredReferenceRows
        assertEquals(1, filtered.size)
        assertEquals("GEN", filtered[0].bookCode)
    }

    // ----- factory

    @Test
    fun `factory creates a ViewModelProvider Factory`() {
        val factory = ReferencesViewModel.factory(testVersion, defaultReference)

        assertTrue(factory is ViewModelProvider.Factory)
    }
}
