package com.youversion.platform.core.search.models

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VerseSearchResultsTests {
    private fun results(
        references: List<BibleReference> = emptyList(),
        userIntent: SearchUserIntent? = SearchUserIntent("raw"),
        didYouMean: List<String> = listOf("a"),
        searchInsteadFor: String? = "instead",
        nextPageToken: String? = "token",
    ) = VerseSearchResults(
        references = references,
        userIntent = userIntent,
        didYouMean = didYouMean,
        searchInsteadFor = searchInsteadFor,
        nextPageToken = nextPageToken,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(results(), results())
        assertEquals(results().hashCode(), results().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            results(references = listOf(BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3))),
            results(userIntent = null),
            results(didYouMean = emptyList()),
            results(searchInsteadFor = null),
            results(nextPageToken = null),
        ).forEach {
            assertNotEquals(results(), it)
            assertNotEquals(results().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "VerseSearchResults(references=[], userIntent=SearchUserIntent(rawValue=raw), " +
                "didYouMean=[a], searchInsteadFor=instead, nextPageToken=token)",
            results().toString(),
        )
    }
}
