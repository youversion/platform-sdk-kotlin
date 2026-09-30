package com.youversion.platform.reader.domain

import com.youversion.platform.core.domain.Storage
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchRepositoryTest {
    private val saved = mutableMapOf<String, String?>()
    private val storage =
        mockk<Storage> {
            every { getStringOrNull(any()) } answers { saved[firstArg()] }
            every { putString(any(), any()) } answers { saved[firstArg()] = secondArg() }
        }
    private val repository = SearchRepository(storage)

    @Test
    fun `nothing saved means no recent searches`() {
        assertEquals(emptyList(), repository.recentSearches)
    }

    @Test
    fun `an unreadable saved value means no recent searches`() {
        saved["bible-reader-search--recent-queries"] = "not json"

        assertEquals(emptyList(), repository.recentSearches)
    }

    @Test
    fun `the newest search comes first`() {
        repository.recordRecentSearch("love")
        repository.recordRecentSearch("peace")

        assertEquals(listOf("peace", "love"), repository.recentSearches)
    }

    @Test
    fun `a repeated search moves to the front rather than appearing twice`() {
        repository.recordRecentSearch("love")
        repository.recordRecentSearch("peace")
        repository.recordRecentSearch("Love")

        assertEquals(listOf("Love", "peace"), repository.recentSearches)
    }

    @Test
    fun `only the three newest searches are kept`() {
        repository.recordRecentSearch("love")
        repository.recordRecentSearch("peace")
        repository.recordRecentSearch("joy")
        repository.recordRecentSearch("hope")

        assertEquals(listOf("hope", "joy", "peace"), repository.recentSearches)
    }

    @Test
    fun `a search is saved trimmed and a blank one is not saved`() {
        repository.recordRecentSearch("  love  ")
        repository.recordRecentSearch("   ")

        assertEquals(listOf("love"), repository.recentSearches)
    }
}
