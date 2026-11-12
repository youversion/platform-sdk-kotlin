package com.youversion.platform.core.highlights.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HighlightTests {
    private fun highlight(
        id: String? = "id",
        bibleId: Int = 1,
        passageId: String = "passage",
        color: String = "color",
        userId: String? = "user",
        createTime: String? = "created",
        updateTime: String? = "updated",
    ) = Highlight(
        id = id,
        bibleId = bibleId,
        passageId = passageId,
        color = color,
        userId = userId,
        createTime = createTime,
        updateTime = updateTime,
    )

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(highlight(), highlight())
        assertEquals(highlight().hashCode(), highlight().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            highlight(id = null),
            highlight(bibleId = 2),
            highlight(passageId = "other"),
            highlight(color = "other"),
            highlight(userId = null),
            highlight(createTime = null),
            highlight(updateTime = null),
        ).forEach {
            assertNotEquals(highlight(), it)
            assertNotEquals(highlight().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "Highlight(id=id, bibleId=1, passageId=passage, color=color, userId=user, " +
                "createTime=created, updateTime=updated)",
            highlight().toString(),
        )
    }
}
