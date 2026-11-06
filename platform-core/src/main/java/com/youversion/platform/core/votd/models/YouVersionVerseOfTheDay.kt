package com.youversion.platform.core.votd.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A day's verse of the day.
 *
 * @property day The day of the year, starting at 1.
 * @property passageUsfm The passage for the day, as a USFM passage id.
 * @property imageId An opaque id for the day's verse image, resolved with
 *     [com.youversion.platform.core.verseimages.api.VerseImagesApi.verseImage]. `null` when the day has no image.
 */
@Serializable
public class YouVersionVerseOfTheDay
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.DAY) public val day: Int,
        @SerialName(CodingKey.PASSAGE_ID) public val passageUsfm: String,
        @SerialName(CodingKey.IMAGE_ID) public val imageId: String? = null,
    ) {
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

        override fun toString(): String =
            "YouVersionVerseOfTheDay(day=$day, passageUsfm=$passageUsfm, imageId=$imageId)"

        public object CodingKey {
            public const val DAY: String = "day"
            public const val PASSAGE_ID: String = "passage_id"
            public const val IMAGE_ID: String = "image_id"
        }

        public companion object {
            public val preview: YouVersionVerseOfTheDay =
                YouVersionVerseOfTheDay(
                    day = 1,
                    passageUsfm = "ISA.43.19",
                )
        }
    }
