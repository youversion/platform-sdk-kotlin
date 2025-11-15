package com.youversion.platform.core.utilities

import kotlin.test.Test
import kotlin.test.assertEquals

class AbbreviationSplittingTests {
    val testSplitter = object : AbbreviationSplitting {}

    @Test
    fun `test split abbreviation ESV`() {
        val result = testSplitter.splitAbbreviation("ESV")
        assertEquals("ESV", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation NIV1984`() {
        val result = testSplitter.splitAbbreviation("NIV1984")
        assertEquals("NIV", result.letters)
        assertEquals("1984", result.numbers)
    }

    @Test
    fun `test split abbreviation KJV21`() {
        val result = testSplitter.splitAbbreviation("KJV21")
        assertEquals("KJV", result.letters)
        assertEquals("21", result.numbers)
    }

    @Test
    fun `test split abbreviation NKJV`() {
        val result = testSplitter.splitAbbreviation("NKJV")
        assertEquals("NKJV", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation MSG`() {
        val result = testSplitter.splitAbbreviation("MSG")
        assertEquals("MSG", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation NLT`() {
        val result = testSplitter.splitAbbreviation("NLT")
        assertEquals("NLT", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation NASB1995`() {
        val result = testSplitter.splitAbbreviation("NASB1995")
        assertEquals("NASB", result.letters)
        assertEquals("1995", result.numbers)
    }

    @Test
    fun `test split abbreviation 123`() {
        val result = testSplitter.splitAbbreviation("123")
        assertEquals("", result.letters)
        assertEquals("123", result.numbers)
    }

    @Test
    fun `test split abbreviation ABC123`() {
        val result = testSplitter.splitAbbreviation("ABC123")
        assertEquals("ABC", result.letters)
        assertEquals("123", result.numbers)
    }

    @Test
    fun `test split abbreviation empty string`() {
        val result = testSplitter.splitAbbreviation("")
        assertEquals("", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation 1`() {
        val result = testSplitter.splitAbbreviation("1")
        assertEquals("", result.letters)
        assertEquals("1", result.numbers)
    }

    @Test
    fun `test split abbreviation A1B2`() {
        val result = testSplitter.splitAbbreviation("A1B2")
        assertEquals("A1B", result.letters)
        assertEquals("2", result.numbers)
    }

    @Test
    fun `test split abbreviation version2023`() {
        val result = testSplitter.splitAbbreviation("version2023")
        assertEquals("version", result.letters)
        assertEquals("2023", result.numbers)
    }

    @Test
    fun `test split abbreviation returns original text when no numbers`() {
        val result = testSplitter.splitAbbreviation("NODIGITS")
        assertEquals("NODIGITS", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation handles only numbers`() {
        val result = testSplitter.splitAbbreviation("2023")
        assertEquals("", result.letters)
        assertEquals("2023", result.numbers)
    }

    @Test
    fun `test split abbreviation handles empty string`() {
        val result = testSplitter.splitAbbreviation("")
        assertEquals("", result.letters)
        assertEquals("", result.numbers)
    }

    @Test
    fun `test split abbreviation handles numbers in middle`() {
        val result = testSplitter.splitAbbreviation("AB12CD34")
        assertEquals("AB12CD", result.letters)
        assertEquals("34", result.numbers)
    }
}
