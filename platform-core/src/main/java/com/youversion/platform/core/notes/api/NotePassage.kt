package com.youversion.platform.core.notes.api

import com.youversion.platform.core.bibles.domain.BibleReference
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

private val bookRegex = Regex("[A-Z0-9]{3}")
private val verseIdRegex = Regex("([A-Z0-9]{3})\\.([1-9][0-9]*)\\.([1-9][0-9]*)(?:-([1-9][0-9]*))?")

/**
 * [references] with uppercase book codes, sorted with adjacent or overlapping verses merged and gaps kept, or `null`
 * when they are empty, span more than one version, book or chapter, or name a whole chapter without [allowsChapter].
 * A whole chapter, when allowed, replaces every verse in it.
 */
internal fun normalizedNoteReferences(
    references: List<BibleReference>,
    allowsChapter: Boolean = false,
): List<BibleReference>? {
    val uppercased =
        references.map {
            val book = it.bookUSFM.uppercase()
            val start = it.verseStart
            val end = it.verseEnd
            if (start == null || end == null) {
                BibleReference(it.versionId, book, it.chapter)
            } else {
                BibleReference(it.versionId, book, it.chapter, start, end)
            }
        }
    val first = uppercased.firstOrNull() ?: return null
    val isOneChapter =
        bookRegex.matches(first.bookUSFM) &&
            uppercased.all {
                it.versionId == first.versionId && it.bookUSFM == first.bookUSFM && it.chapter == first.chapter
            }
    return when {
        !isOneChapter -> null
        uppercased.none { it.verseStart == null } -> BibleReference.referencesByMerging(uppercased)
        allowsChapter -> listOf(BibleReference(first.versionId, first.bookUSFM, first.chapter))
        else -> null
    }
}

/** The API's compact passage id for normalized [references], such as `JHN.3.1-3+JHN.3.7`. */
internal fun notePassageId(references: List<BibleReference>): String =
    references.joinToString("+") {
        when (it.verseStart) {
            null -> it.chapterUSFM
            it.verseEnd -> "${it.chapterUSFM}.${it.verseStart}"
            else -> "${it.chapterUSFM}.${it.verseStart}-${it.verseEnd}"
        }
    }

/** The API's `passage` object for an attachment; throws [IllegalArgumentException] for an invalid selection. */
internal fun notePassage(references: List<BibleReference>): JsonObject {
    val normalized =
        requireNotNull(normalizedNoteReferences(references)) {
            "references must be nonempty verses in one version, book and chapter"
        }
    return buildJsonObject {
        put("bible_id", normalized.first().versionId)
        put("passage_id", notePassageId(normalized))
    }
}

/** The references of an API `passage` object, or `null` when it is absent or malformed. */
internal fun notePassageReferences(passage: JsonElement?): List<BibleReference>? {
    val passageObject = passage as? JsonObject ?: return null
    val versionId = (passageObject["bible_id"] as? JsonPrimitive)?.intOrNull ?: return null
    val passageId = (passageObject["passage_id"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
    val references =
        passageId.split("+").map { verseId ->
            val (book, chapterText, startText, endText) = verseIdRegex.matchEntire(verseId)?.destructured ?: return null
            val chapter = chapterText.toIntOrNull() ?: return null
            val start = startText.toIntOrNull() ?: return null
            val end = endText.ifEmpty { startText }.toIntOrNull()?.takeIf { it >= start } ?: return null
            BibleReference(versionId, book, chapter, start, end)
        }
    return normalizedNoteReferences(references)
}
