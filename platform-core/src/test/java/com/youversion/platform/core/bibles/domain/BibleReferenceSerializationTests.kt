package com.youversion.platform.core.bibles.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibleReferenceSerializationTests {
    // ----- Repairing stored half-references

    @Test
    fun `test decoding keeps a reference that stored both verses`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":3,"verseEnd":5}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertEquals(3, reference.verseStart)
        assertEquals(5, reference.verseEnd)
    }

    @Test
    fun `test decoding a stored starting verse alone repairs it to a single verse`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":3,"verseEnd":null}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertEquals(3, reference.verseStart)
        assertEquals(3, reference.verseEnd)
    }

    @Test
    fun `test decoding a stored ending verse alone repairs it to the whole chapter`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":null,"verseEnd":5}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertNull(reference.verseStart)
        assertNull(reference.verseEnd)
    }

    @Test
    fun `test decoding a stored reference with neither verse is the whole chapter`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":null,"verseEnd":null}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertNull(reference.verseStart)
        assertNull(reference.verseEnd)
    }

    // ----- Absent verse fields, not just null ones

    @Test
    fun `test decoding tolerates an absent ending verse`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":3}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertEquals(3, reference.verseStart)
        assertEquals(3, reference.verseEnd)
    }

    @Test
    fun `test decoding tolerates an absent starting verse`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseEnd":5}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertNull(reference.verseStart)
        assertNull(reference.verseEnd)
    }

    @Test
    fun `test decoding tolerates both verse fields being absent`() {
        val json = """{"versionId":1,"bookUSFM":"GEN","chapter":1}"""

        val reference = Json.decodeFromString<BibleReference>(json)

        assertNull(reference.verseStart)
        assertNull(reference.verseEnd)
    }

    // ----- The stored format itself does not change

    @Test
    fun `test encoding a whole chapter writes both verses as null`() {
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        val json = Json.encodeToString(reference)

        assertEquals("""{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":null,"verseEnd":null}""", json)
    }

    @Test
    fun `test encoding a single verse writes the same verse twice`() {
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3)

        val json = Json.encodeToString(reference)

        assertEquals("""{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":3,"verseEnd":3}""", json)
    }

    @Test
    fun `test encoding a verse range writes both verses`() {
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5)

        val json = Json.encodeToString(reference)

        assertEquals("""{"versionId":1,"bookUSFM":"GEN","chapter":1,"verseStart":3,"verseEnd":5}""", json)
    }

    // ----- Round tripping the three legal shapes

    @Test
    fun `test round tripping the three legal shapes returns an equal reference`() {
        val shapes =
            listOf(
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 3),
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verseStart = 3, verseEnd = 5),
            )

        for (reference in shapes) {
            assertEquals(reference, Json.decodeFromString<BibleReference>(Json.encodeToString(reference)))
        }
    }
}
