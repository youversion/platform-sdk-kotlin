package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Widens a verse so the range constructor delegates to the primary constructor rather than to itself. */
private fun optionalVerse(verse: Int): Int? = verse

// The primary constructor is private so a reference can only be built as one of the three shapes below.
@Serializable(with = BibleReferenceSerializer::class)
public class BibleReference private constructor(
    public val versionId: Int,
    public val bookUSFM: String,
    public val chapter: Int,
    public val verseStart: Int?,
    public val verseEnd: Int?,
) : Comparable<BibleReference> {
    init {
        require(chapter >= 1) { "Chapter must be greater than or equal to 1." }
        verseStart?.let {
            require(it >= 1) { "Verse must be greater than or equal to 1." }
        }
        verseEnd?.let {
            require(it >= 1) { "Ending verse must be greater than or equal to 1." }
        }
        if (verseStart != null && verseEnd != null) {
            require(verseEnd >= verseStart) { "Ending verse must be equal to or after starting verse." }
        }
    }

    /** The whole chapter. */
    public constructor(versionId: Int, bookUSFM: String, chapter: Int) : this(versionId, bookUSFM, chapter, null, null)

    /** A single verse. */
    public constructor(versionId: Int, bookUSFM: String, chapter: Int, verse: Int) :
        this(versionId, bookUSFM, chapter, optionalVerse(verse), optionalVerse(verse))

    /** A verse range, ending on or after it starts. */
    public constructor(versionId: Int, bookUSFM: String, chapter: Int, verseStart: Int, verseEnd: Int) :
        this(versionId, bookUSFM, chapter, optionalVerse(verseStart), optionalVerse(verseEnd))

    public val chapterUSFM: String
        get() = "${bookUSFM.uppercase()}.$chapter"

    public val isRange: Boolean
        get() = verseStart != verseEnd

    /**
     * The verses this reference covers; a whole chapter covers all of them, so its range is unbounded. Compare or
     * intersect endpoints rather than iterating: iterating a whole-chapter range walks to [Int.MAX_VALUE].
     */
    internal val verseRange: IntRange
        get() {
            val start = verseStart
            val end = verseEnd
            return if (start != null && end != null) start..end else 1..Int.MAX_VALUE
        }

    public val asUSFM: String
        get() {
            val upperBookUSFM = bookUSFM.uppercase()
            return when {
                verseStart == null -> "$upperBookUSFM.$chapter"
                isRange -> "$upperBookUSFM.$chapter.$verseStart-$upperBookUSFM.$chapter.$verseEnd"
                else -> "$upperBookUSFM.$chapter.$verseStart"
            }
        }

    /** This same chapter, verse or range, in the version identified by [versionId]. */
    @PlatformInternalApi
    public fun withVersionId(versionId: Int): BibleReference =
        BibleReference(versionId, bookUSFM, chapter, verseStart, verseEnd)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BibleReference) return false
        return versionId == other.versionId &&
            bookUSFM == other.bookUSFM &&
            chapter == other.chapter &&
            verseStart == other.verseStart &&
            verseEnd == other.verseEnd
    }

    override fun hashCode(): Int {
        var result = versionId.hashCode()
        result = 31 * result + bookUSFM.hashCode()
        result = 31 * result + chapter.hashCode()
        result = 31 * result + verseStart.hashCode()
        result = 31 * result + verseEnd.hashCode()
        return result
    }

    override fun toString(): String {
        val prefix = "bible${versionId}__$bookUSFM.$chapter"
        return when {
            verseStart == null -> prefix
            isRange -> "$prefix.$verseStart-$verseEnd"
            else -> "$prefix.$verseStart"
        }
    }

    override fun compareTo(other: BibleReference): Int = compare(this, other)

    /**
     * Returns whether this reference exists in [version].
     *
     * If [version] has not been fully populated (its books list, book metadata, or chapter
     * metadata is absent), this function optimistically returns `true` rather than reporting a
     * false negative. A version whose books list is explicitly empty is treated as not containing
     * any reference.
     */
    public fun existsIn(version: BibleVersion): Boolean {
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

    /**
     * Whether this reference and [otherReference] share at least one verse. A whole chapter overlaps any
     * reference within the same chapter.
     */
    public fun overlaps(otherReference: BibleReference): Boolean {
        if (versionId != otherReference.versionId || bookUSFM != otherReference.bookUSFM) {
            return false
        }

        val a = minOf(this, otherReference)
        val b = maxOf(this, otherReference)

        if (a.chapter != b.chapter) {
            return false // TODO change this when we support cross-chapter ranges
        }

        val aVerses = a.verseRange
        val bVerses = b.verseRange

        return aVerses.last >= bVerses.first && bVerses.last >= aVerses.first
    }

    /**
     * Whether this reference covers every verse of [otherReference]. A whole chapter contains any reference
     * within the same chapter.
     */
    public fun contains(otherReference: BibleReference): Boolean {
        if (versionId != otherReference.versionId || bookUSFM != otherReference.bookUSFM) {
            return false
        }

        if (chapter != otherReference.chapter) {
            return false // TODO change this when we support cross-chapter ranges
        }

        val outerVerses = verseRange
        val innerVerses = otherReference.verseRange

        return outerVerses.first <= innerVerses.first && outerVerses.last >= innerVerses.last
    }

    public fun isAdjacentOrOverlapping(otherReference: BibleReference): Boolean {
        if (versionId != otherReference.versionId ||
            bookUSFM != otherReference.bookUSFM ||
            chapter != otherReference.chapter
        ) {
            return false
        }

        val a = minOf(this, otherReference)
        val b = maxOf(this, otherReference)

        return a.verseRange.last >= b.verseRange.first - 1
    }

    public companion object {
        // Static function equivalent
        public fun compare(
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

            if (a.verseStart == null || b.verseStart == null) {
                return when {
                    a.verseStart == b.verseStart -> 0
                    a.verseStart == null -> -1
                    else -> 1
                }
            }

            val aVerses = a.verseRange
            val bVerses = b.verseRange
            return when {
                aVerses.first != bVerses.first -> if (aVerses.first < bVerses.first) -1 else 1
                aVerses.last != bVerses.last -> if (aVerses.last < bVerses.last) -1 else 1
                else -> 0
            }
        }

        public fun referencesByMerging(references: List<BibleReference>): List<BibleReference> {
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

        public fun referenceByMerging(
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

            return BibleReference(
                versionId = minReference.versionId,
                bookUSFM = minReference.bookUSFM,
                chapter = minReference.chapter,
                verseStart = minOf(a.verseRange.first, b.verseRange.first),
                verseEnd = maxOf(a.verseRange.last, b.verseRange.last),
            )
        }

        public fun unvalidatedReference(
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
                val (bookText, chapterText, verseText, endChapterText, endVerseText) = match.destructured
                val chapter = chapterText.toIntOrNull()
                val endChapter = endChapterText.toIntOrNull()
                val verse = verseText.toIntOrNull()
                val endVerse = endVerseText.toIntOrNull()
                if (chapter != null && endChapter != null && verse != null && endVerse != null) {
                    if (chapter != endChapter) {
                        return null
                    }
                    return reference(bookText.uppercase(), chapter, verse, endVerse)
                }
                return null
            }

            // GEN.1.3-GEN.1.5
            val patBCVBCV = Regex("""(\w{3})\.(\d+)\.(\d+)-(\w{3})\.(\d+)\.(\d+)""")
            patBCVBCV.matchEntire(usfm)?.let { match ->
                val (bookText, chapterText, verseText, endBookText, endChapterText, endVerseText) =
                    match.destructured
                val chapter = chapterText.toIntOrNull()
                val endChapter = endChapterText.toIntOrNull()
                val verse = verseText.toIntOrNull()
                val endVerse = endVerseText.toIntOrNull()
                if (chapter != null && endChapter != null && verse != null && endVerse != null) {
                    if (bookText != endBookText || chapter != endChapter) {
                        return null
                    }
                    return reference(bookText.uppercase(), chapter, verse, endVerse)
                }
                return null
            }

            // GEN.1.3-5
            val patBCVV = Regex("""(\w{3})\.(\d+)\.(\d+)-(\d+)""")
            patBCVV.matchEntire(usfm)?.let { match ->
                val (bookText, chapterText, verseText, endVerseText) = match.destructured
                val chapter = chapterText.toIntOrNull()
                val verse = verseText.toIntOrNull()
                val endVerse = endVerseText.toIntOrNull()
                if (chapter != null && verse != null && endVerse != null) {
                    return reference(bookText.uppercase(), chapter, verse, endVerse)
                }
                return null
            }

            // GEN.1.3
            val patBCV = Regex("""(\w{3})\.(\d+)\.(\d+)""")
            patBCV.matchEntire(usfm)?.let { match ->
                val (bookText, chapterText, verseText) = match.destructured
                val chapter = chapterText.toIntOrNull()
                val verse = verseText.toIntOrNull()
                if (chapter != null && verse != null) {
                    return reference(bookText.uppercase(), chapter, verse, verse)
                }
                return null
            }

            // GEN.1
            val patBC = Regex("""(\w{3})\.(\d+)""")
            patBC.matchEntire(usfm)?.let { match ->
                val (bookText, chapterText) = match.destructured
                val chapter = chapterText.toIntOrNull()
                if (chapter != null) {
                    return BibleReference(versionId = versionId, bookUSFM = bookText.uppercase(), chapter = chapter)
                }
                return null
            }

            // GEN.1-2
            val patBCC = Regex("""(\w{3})\.(\d+)-(\d+)""")
            patBCC.matchEntire(usfm)?.let { match ->
                val (bookText, chapterText, _) = match.destructured
                val chapter = chapterText.toIntOrNull()
                if (chapter != null) {
                    return BibleReference(versionId = versionId, bookUSFM = bookText.uppercase(), chapter = chapter)
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
            verseStart == null -> BibleReference(stored.versionId, stored.bookUSFM, stored.chapter)
            verseEnd == null -> BibleReference(stored.versionId, stored.bookUSFM, stored.chapter, verse = verseStart)
            else -> BibleReference(stored.versionId, stored.bookUSFM, stored.chapter, verseStart, verseEnd)
        }
    }
}
