package com.youversion.platform.core.votd.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A day's verse of the day.
 *
 * @property day The day of the year, starting at 1.
 * @property passageUsfm The passage for the day, as a USFM passage id.
 * @property imageId An opaque id for the day's verse image, resolved with
 *     [com.youversion.platform.core.verseimages.api.VerseImagesApi.verseImage]. `null` when the day has no image.
 *     [copy] does not carry it over, so a copy of a day with an image is not equal to the original.
 */
@Serializable
data class YouVersionVerseOfTheDay(
    @SerialName(CodingKey.DAY) val day: Int,
    @SerialName(CodingKey.PASSAGE_ID) val passageUsfm: String,
) {
    @SerialName(CodingKey.IMAGE_ID)
    var imageId: String? = null
        private set

    /**
     * Creates a verse of the day with [imageId], the only way to set it; the two-argument constructor leaves it
     * `null`.
     */
    constructor(day: Int, passageUsfm: String, imageId: String?) : this(day, passageUsfm) {
        this.imageId = imageId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is YouVersionVerseOfTheDay) return false
        return day == other.day &&
            passageUsfm == other.passageUsfm &&
            imageId == other.imageId
    }

    override fun hashCode(): Int {
        var result = day
        result = 31 * result + passageUsfm.hashCode()
        result = 31 * result + (imageId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "YouVersionVerseOfTheDay(day=$day, passageUsfm=$passageUsfm, imageId=$imageId)"

    object CodingKey {
        const val DAY = "day"
        const val PASSAGE_ID = "passage_id"
        const val IMAGE_ID = "image_id"
    }

    companion object {
        val preview =
            YouVersionVerseOfTheDay(
                day = 1,
                passageUsfm = "ISA.43.19",
            )
    }
}
