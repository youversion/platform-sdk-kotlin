package com.youversion.platform.core.bibles.model

import com.youversion.platform.core.bibles.models.BibleBookIntro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BibleBookIntroTests {
    private fun intro(
        id: String = "id",
        passageId: String = "passage",
        title: String = "title",
    ) = BibleBookIntro(
        id = id,
        passageId = passageId,
        title = title,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(intro(), intro())
        assertEquals(intro().hashCode(), intro().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            intro(id = "other"),
            intro(passageId = "other"),
            intro(title = "other"),
        ).forEach {
            assertNotEquals(intro(), it)
            assertNotEquals(intro().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals("BibleBookIntro(id=id, passageId=passage, title=title)", intro().toString())
    }
}
