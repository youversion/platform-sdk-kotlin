package com.youversion.platform.core.languages.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LanguageTests {
    @Test
    fun `test reflexive equality`() {
        val language = Language(id = "en", language = "English")
        assertTrue(language.equals(language))
    }

    @Test
    fun `test same id with different fields are equal`() {
        val first = Language(id = "en", language = "English", script = "Latn")
        val second = Language(id = "en", language = "Spanish", script = "Arab")
        assertEquals(first, second)
    }

    @Test
    fun `test different ids are not equal`() {
        val first = Language(id = "en", language = "English")
        val second = Language(id = "es", language = "Spanish")
        assertNotEquals(first, second)
    }

    @Test
    fun `test comparing with non-Language returns false`() {
        val language = Language(id = "en", language = "English")
        assertFalse(language.equals("en"))
        assertFalse(language.equals(42))
        assertFalse(language.equals(null))
    }

    @Test
    fun `test hashCode is consistent for same id`() {
        val first = Language(id = "en", language = "English")
        val second = Language(id = "en", language = "Spanish")
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `test both null ids are equal`() {
        val first = Language(id = null, language = "English")
        val second = Language(id = null, language = "Spanish")
        assertEquals(first, second)
    }

    @Test
    fun `test null id and non-null id are not equal`() {
        val withNull = Language(id = null, language = "English")
        val withValue = Language(id = "en", language = "English")
        assertNotEquals(withNull, withValue)
        assertNotEquals(withValue, withNull)
    }

    @Test
    fun `test hashCode with null id does not throw`() {
        val language = Language(id = null)
        language.hashCode()
    }

    @Test
    fun `test same id collapses in HashSet`() {
        val first = Language(id = "en", language = "English")
        val second = Language(id = "en", language = "Different")
        val set = hashSetOf(first, second)
        assertEquals(1, set.size)
    }

    @Test
    fun `test different ids remain distinct in HashSet`() {
        val first = Language(id = "en", language = "English")
        val second = Language(id = "es", language = "Spanish")
        val set = hashSetOf(first, second)
        assertEquals(2, set.size)
    }
}
