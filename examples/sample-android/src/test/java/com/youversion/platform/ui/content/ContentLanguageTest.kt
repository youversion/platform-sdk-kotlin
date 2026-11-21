package com.youversion.platform.ui.content

import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(PlatformInternalApi::class)
class ContentLanguageTest {
    private fun version(
        id: Int,
        languageTag: String,
        bookCodes: List<String>? = null,
    ) = BibleVersion(id = id, languageTag = languageTag, bookCodes = bookCodes)

    @Test
    fun `English prefers the default version`() =
        runTest {
            val versions = { _: String -> listOf(version(1, "en"), version(3034, "en")) }

            assertEquals(3034, contentBibleId("en", versions = { versions(it) }))
        }

    @Test
    fun `Taiwanese Chinese falls back to traditional script, then to Chinese`() =
        runTest {
            val requested = mutableListOf<String>()
            val id =
                contentBibleId("zh-TW") { tag ->
                    requested += tag
                    if (tag == "zh") listOf(version(9, "zh")) else emptyList()
                }

            assertEquals(listOf("zh-TW", "zh-Hant", "zh"), requested)
            assertEquals(9, id)
        }

    @Test
    fun `a passage picks a Bible that contains its book`() =
        runTest {
            val versions = listOf(version(1, "es", listOf("MAT")), version(2, "es", listOf("GEN", "JHN")))

            assertEquals(2, contentBibleId("es", "JHN.3.16") { versions })
            assertFailsWith<IllegalStateException> { contentBibleId("es", "REV.1.1") { versions } }
        }
}
