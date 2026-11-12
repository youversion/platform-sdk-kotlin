package com.youversion.platform.core.votd.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouVersionVerseOfTheDayTests {
    @Test
    fun `test days with equal image ids are equal`() {
        val first = YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1")
        val second = YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1")
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `test days differing only in image id are not equal`() {
        assertNotEquals(
            YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1"),
            YouVersionVerseOfTheDay(1, "ISA.43.19", "img-2"),
        )
        assertNotEquals(
            YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1"),
            YouVersionVerseOfTheDay(1, "ISA.43.19"),
        )
    }

    @Test
    fun `test to string includes image id`() {
        val text = YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1").toString()
        assertTrue(text.contains("imageId=img-1"))
    }

    @Test
    fun `test decoding reads the image id`() {
        val day =
            Json.decodeFromString<YouVersionVerseOfTheDay>(
                """{"day":1,"passage_id":"ISA.43.19","image_id":"img-1"}""",
            )
        assertEquals(YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1"), day)
    }

    @Test
    fun `test decoding without an image id leaves it null`() {
        val day = Json.decodeFromString<YouVersionVerseOfTheDay>("""{"day":1,"passage_id":"ISA.43.19"}""")
        assertNull(day.imageId)
    }

    @Test
    fun `test encoding writes the image id under its key`() {
        assertEquals(
            """{"day":1,"passage_id":"ISA.43.19","image_id":"img-1"}""",
            Json.encodeToString(YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1")),
        )
    }

    @Test
    fun `test encoding omits a null image id`() {
        assertEquals(
            """{"day":1,"passage_id":"ISA.43.19"}""",
            Json.encodeToString(YouVersionVerseOfTheDay(1, "ISA.43.19")),
        )
    }

    @Test
    fun `test image id defaults to null`() {
        assertNull(YouVersionVerseOfTheDay(1, "ISA.43.19").imageId)
    }

    @Test
    fun `test image id is kept`() {
        assertEquals("img-1", YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1").imageId)
    }
}
