package com.youversion.platform.core.votd.models

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
    fun `test copy drops image id`() {
        val day = YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1")
        val copied = day.copy()
        assertNull(copied.imageId)
        assertNotEquals(day, copied)
    }

    @Test
    fun `test two argument constructor leaves image id null`() {
        assertNull(YouVersionVerseOfTheDay(1, "ISA.43.19").imageId)
    }

    @Test
    fun `test three argument constructor sets image id`() {
        assertEquals("img-1", YouVersionVerseOfTheDay(1, "ISA.43.19", "img-1").imageId)
    }
}
