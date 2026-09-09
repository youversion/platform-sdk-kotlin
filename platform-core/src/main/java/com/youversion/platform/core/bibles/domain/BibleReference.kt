package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Widens a verse so the range constructor delegates to the primary constructor rather than to itself. */
private fun nullable(verse: Int): Int? = verse

// The primary constructor is private so a reference can only be built as one of the three shapes below. Copying to
// another version, book or chapter must keep working, so the generated copy stays public and init asserts the shape.
@ExposedCopyVisibility
@Serializable(with = BibleReferenceSerializer::class)
data class BibleReference private constructor(
    val versionId: Int,
    val bookUSFM: String,
    val chapter: Int,
    val verseStart: Int?,
    val verseEnd: Int?,
) : Comparable<BibleReference> {
    init {
        require(chapter >= 1) { "Chapter must be greater than or equal to 1." }
        verseStart?.let {
            require(it >= 1) { "Verse must be greater than or equal to 1." }
        }
        verseEnd?.let {
            require(it >= 1) { "Ending verse must be greater than or equal to 1." }
        }
        require((verseStart == null) == (verseEnd == null)) { LEGAL_SHAPES }
        if (verseStart != null && verseEnd != null) {
            require(verseEnd >= verseStart) { "Ending verse must be equal to or after starting verse." }
        }
    }

    /** The whole chapter. */
    constructor(versionId: Int, bookUSFM: String, chapter: Int) : this(versionId, bookUSFM, chapter, null, null)

    /** A single verse. */
    constructor(versionId: Int, bookUSFM: String, chapter: Int, verse: Int) :
        this(versionId, bookUSFM, chapter, nullable(verse), nullable(verse))

    /** A verse range, ending on or after it starts. */
    constructor(versionId: Int, bookUSFM: String, chapter: Int, verseStart: Int, verseEnd: Int) :
        this(versionId, bookUSFM, chapter, nullable(verseStart), nullable(verseEnd))

    val chapterUSFM: String
        get() = "${bookUSFM.uppercase()}.$chapter"

    val isRange: Boolean
        get() = verseEnd != null && verseStart != verseEnd

    override fun toString(): String {
        val prefix = "bible${versionId}__$bookUSFM.$chapter"
        return when {
            verseStart != null && verseEnd != null && verseStart != verseEnd ->
                "$prefix.$verseStart-$verseEnd"

            verseStart != null ->
                "$prefix.$verseStart"

            else ->
                prefix
        }
    }

    override fun compareTo(other: BibleReference): Int = compare(this, other)

    val asUSFM: String
        get() {
            val upperBookUSFM = bookUSFM.uppercase()
            return when {
                verseStart != null && verseEnd != null && verseStart != verseEnd ->
                    "$upperBookUSFM.$chapter.$verseStart-$upperBookUSFM.$chapter.$verseEnd"

                verseStart != null ->
                    "$upperBookUSFM.$chapter.$verseStart"

                else ->
                    "$upperBookUSFM.$chapter"
            }
        }

    /**
     * Returns whether this reference exists in [version].
     *
     * If [version] has not been fully populated (its books list, book metadata, or chapter
     * metadata is absent), this function optimistically returns `true` rather than reporting a
     * false negative. A version whose books list is explicitly empty is treated as not containing
     * any reference.
     */
    fun existsIn(version: BibleVersion): Boolean {
        if (version.books == null) return true

        val normalizedBookUSFM = bookUSFM.uppercase()
        if (!version.bookUSFMs.any { it.uppercase() == normalizedBookUSFM }) return false

        val book = version.book(normalizedBookUSFM) ?: return true
        val chapters = book.chapters ?: return true

        val chapterText = chapter.toString()
        return chapters.any { chapterMetadata ->
            chapterMetadata.id == chapterText ||
                chapterMetadata.title == chapterText ||
                chapterMetadata.passageId == chapterUSFM
        }
    }

    fun overlaps(otherReference: BibleReference): Boolean {
        if (versionId != otherReference.versionId || bookUSFM != otherReference.bookUSFM) {
            return false
        }

        val a = minOf(this, otherReference)
        val b = maxOf(this, otherReference)

        if (a.chapter != b.chapter) {
            return false // TODO change this when we support cross-chapter ranges
        }

        // Treat a reference with both verseStart and verseEnd nil as a whole chapter reference.
        // Whole chapter overlaps any reference within the same chapter.
        val aStart: Int
        val aEnd: Int
        if (a.verseStart == null && a.verseEnd == null) {
            aStart = 1
            aEnd = Int.MAX_VALUE
        } else {
            aStart = a.verseStart ?: 1
            aEnd = a.verseEnd ?: aStart
        }

        val bStart: Int
        val bEnd: Int
        if (b.verseStart == null && b.verseEnd == null) {
            bStart = 1
            bEnd = Int.MAX_VALUE
        } else {
            bStart = b.verseStart ?: 1
            bEnd = b.verseEnd ?: bStart
        }

        return aEnd >= bStart && bEnd >= aStart
    }

    fun contains(otherReference: BibleReference): Boolean {
        if (versionId != otherReference.versionId || bookUSFM != otherReference.bookUSFM) {
            return false
        }

        if (chapter != otherReference.chapter) {
            return false // TODO change this when we support cross-chapter ranges
        }

        // Treat a reference with both verseStart and verseEnd nil as a whole chapter reference.
        // Whole chapter contains any reference within the same chapter.
        val aStart: Int
        val aEnd: Int
        if (verseStart == null && verseEnd == null) {
            aStart = 1
            aEnd = Int.MAX_VALUE
        } else {
            aStart = verseStart ?: 1
            aEnd = verseEnd ?: aStart
        }

        val bStart: Int
        val bEnd: Int
        if (otherReference.verseStart == null && otherReference.verseEnd == null) {
            bStart = 1
            bEnd = Int.MAX_VALUE
        } else {
            bStart = otherReference.verseStart ?: 1
            bEnd = otherReference.verseEnd ?: bStart
        }

        return aStart <= bStart && aEnd >= bEnd
    }

    fun isAdjacentOrOverlapping(otherReference: BibleReference): Boolean {
        if (versionId != otherReference.versionId ||
            bookUSFM != otherReference.bookUSFM ||
            chapter != otherReference.chapter
        ) {
            return false
        }

        val a = minOf(this, otherReference)
        val b = maxOf(this, otherReference)

        val lastVerseOfA = a.verseEnd ?: a.verseStart
        return if (lastVerseOfA != null) {
            val firstVerseOfB = b.verseStart ?: 1
            lastVerseOfA + 1 >= firstVerseOfB
        } else {
            true
        }
    }

    companion object {
        private const val LEGAL_SHAPES =
            "A reference must be a whole chapter (neither verse), a single verse (both verses the same) or a " +
                "verse range (both verses set); it cannot have only one of verseStart and verseEnd."

        // Static function equivalent
        fun compare(
            a: BibleReference,
            b: BibleReference,
        ): Int {
            // returns -1, 0, or 1
            if (a.bookUSFM != b.bookUSFM) {
                return if (a.bookUSFM < b.bookUSFM) -1 else 1
            }

            if (a.chapter != b.chapter) {
                return if (a.chapter < b.chapter) -1 else 1
            }

            return when {
                a.verseStart != null && b.verseStart != null -> {
                    val lhs = a.verseStart
                    val rhs = b.verseStart
                    if (lhs == rhs) {
                        when {
                            a.verseEnd != null && b.verseEnd != null -> {
                                val lhsEnd = a.verseEnd
                                val rhsEnd = b.verseEnd
                                if (lhsEnd == rhsEnd) {
                                    0
                                } else if (lhsEnd < rhsEnd) {
                                    -1
                                } else {
                                    1
                                }
                            }

                            a.verseEnd == null && b.verseEnd == null -> 0
                            a.verseEnd == null -> 1
                            else -> -1
                        }
                    } else {
                        if (lhs < rhs) -1 else 1
                    }
                }

                a.verseStart == null && b.verseStart == null -> 0
                a.verseStart == null -> -1
                else -> 1
            }
        }

        fun referencesByMerging(references: List<BibleReference>): List<BibleReference> {
            val tmp = references.toMutableList()
            tmp.sort()
            var i = 1
            while (i < tmp.size) {
                val previousReference = tmp[i - 1]
                val currentReference = tmp[i]
                if (previousReference.isAdjacentOrOverlapping(currentReference)) {
                    tmp[i - 1] = referenceByMerging(previousReference, currentReference)
                    tmp.removeAt(i)
                } else {
                    i++
                }
            }
            return tmp
        }

        fun referenceByMerging(
            a: BibleReference,
            b: BibleReference,
        ): BibleReference {
            require(a.isAdjacentOrOverlapping(b)) {
                "This function requires the two references to be adjacent or overlapping."
            }

            if (a.verseStart == null) {
                return a // chapter reference
            }

            if (b.verseStart == null) {
                return b // chapter reference
            }

            val minReference = minOf(a, b)

            val lastVerseOfA = a.verseEnd ?: a.verseStart
            val lastVerseOfB = b.verseEnd ?: b.verseStart
            val firstVerse = minOf(a.verseStart, b.verseStart)
            val lastVerse = maxOf(lastVerseOfA, lastVerseOfB)

            return BibleReference(
                versionId = minReference.versionId,
                bookUSFM = minReference.bookUSFM,
                chapter = minReference.chapter,
                verseStart = firstVerse,
                verseEnd = lastVerse,
            )
        }

        fun unvalidatedReference(
            usfm: String,
            versionId: Int,
        ): BibleReference? {
            fun reference(
                bookUSFM: String,
                chapter: Int,
                verseStart: Int,
                verseEnd: Int,
            ): BibleReference? {
                if (verseStart > verseEnd) {
                    return null
                }
                return BibleReference(
                    versionId = versionId,
                    bookUSFM = bookUSFM,
                    chapter = chapter,
                    verseStart = verseStart,
                    verseEnd = verseEnd,
                )
            }

            // GEN.1.3-1.5
            val patBCVCV = Regex("""(\w{3})\.(\d+)\.(\d+)-(\d+)\.(\d+)""")
            patBCVCV.matchEntire(usfm)?.let { match ->
                val (bText, cText, vText, _, v2Text) = match.destructured
                val c = cText.toIntOrNull()
                val v = vText.toIntOrNull()
                val v2 = v2Text.toIntOrNull()
                if (c != null && v != null && v2 != null) {
                    return reference(bText.uppercase(), c, v, v2)
                }
                return null
            }

            // GEN.1.3-GEN.1.5
            val patBCVBCV = Regex("""(\w{3})\.(\d+)\.(\d+)-(\w{3})\.(\d+)\.(\d+)""")
            patBCVBCV.matchEntire(usfm)?.let { match ->
                val (bText, cText, vText, b2Text, _, v2Text) = match.destructured
                val c = cText.toIntOrNull()
                val v = vText.toIntOrNull()
                val v2 = v2Text.toIntOrNull()
                if (c != null && v != null && v2 != null) {
                    if (bText != b2Text) {
                        return null
                    }
                    return reference(bText.uppercase(), c, v, v2)
                }
                return null
            }

            // GEN.1.3-5
            val patBCVV = Regex("""(\w{3})\.(\d+)\.(\d+)-(\d+)""")
            patBCVV.matchEntire(usfm)?.let { match ->
                val (bText, cText, vText, v2Text) = match.destructured
                val c = cText.toIntOrNull()
                val v = vText.toIntOrNull()
                val v2 = v2Text.toIntOrNull()
                if (c != null && v != null && v2 != null) {
                    return reference(bText.uppercase(), c, v, v2)
                }
                return null
            }

            // GEN.1.3
            val patBCV = Regex("""(\w{3})\.(\d+)\.(\d+)""")
            patBCV.matchEntire(usfm)?.let { match ->
                val (bText, cText, vText) = match.destructured
                val c = cText.toIntOrNull()
                val v = vText.toIntOrNull()
                if (c != null && v != null) {
                    return reference(bText.uppercase(), c, v, v)
                }
                return null
            }

            // GEN.1
            val patBC = Regex("""(\w{3})\.(\d+)""")
            patBC.matchEntire(usfm)?.let { match ->
                val (bText, cText) = match.destructured
                val c = cText.toIntOrNull()
                if (c != null) {
                    return reference(bText.uppercase(), c, 1, 1)
                }
                return null
            }

            // GEN.1-2
            val patBCC = Regex("""(\w{3})\.(\d+)-(\d+)""")
            patBCC.matchEntire(usfm)?.let { match ->
                val (bText, cText, _) = match.destructured
                val c = cText.toIntOrNull()
                if (c != null) {
                    return reference(bText.uppercase(), c, 1, 1)
                }
                return null
            }

            return null
        }
    }
}

/**
 * Reads references saved before a reference was required to be one of the three shapes the SDK means, repairing the
 * half-specified ones on the way in: a starting verse alone becomes that single verse, and an ending verse alone
 * becomes the whole chapter. Encoding is unchanged — [Stored] mirrors the saved shape field for field.
 */
private object BibleReferenceSerializer : KSerializer<BibleReference> {
    @Serializable
    @SerialName("com.youversion.platform.core.bibles.domain.BibleReference")
    @OptIn(ExperimentalSerializationApi::class)
    private data class Stored(
        val versionId: Int,
        val bookUSFM: String,
        val chapter: Int,
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) val verseStart: Int? = null,
        @EncodeDefault(EncodeDefault.Mode.ALWAYS) val verseEnd: Int? = null,
    )

    override val descriptor: SerialDescriptor = Stored.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: BibleReference,
    ) {
        val stored = Stored(value.versionId, value.bookUSFM, value.chapter, value.verseStart, value.verseEnd)
        encoder.encodeSerializableValue(Stored.serializer(), stored)
    }

    override fun deserialize(decoder: Decoder): BibleReference {
        val stored = decoder.decodeSerializableValue(Stored.serializer())
        val verseStart = stored.verseStart
        val verseEnd = stored.verseEnd

        return when {
            // Whole chapter, whether nothing was saved or only an ending verse was
            verseStart == null -> BibleReference(stored.versionId, stored.bookUSFM, stored.chapter)

            // Single verse
            verseEnd == null -> BibleReference(stored.versionId, stored.bookUSFM, stored.chapter, verse = verseStart)

            else -> BibleReference(stored.versionId, stored.bookUSFM, stored.chapter, verseStart, verseEnd)
        }
    }
}
