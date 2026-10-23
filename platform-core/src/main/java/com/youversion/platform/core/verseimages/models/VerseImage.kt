package com.youversion.platform.core.verseimages.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A piece of verse art. This is metadata only: the image itself is served from [imageUrl].
 *
 * @property id The opaque identifier, used to fetch this image again.
 * @property imageUrl Where the image is served. Append `?size=thumb`, `?size=card` or `?size=full` to choose a
 *     resized rendition; leave it off for the full-size image.
 * @property passageId The verse or verse range the art illustrates, such as `JHN.3.16` or `1CO.13.4-5`.
 * @property attribution The credit line to show with the image.
 * @property languageTag The BCP 47 language tag of the text in the art.
 * @property width The native width in pixels.
 * @property height The native height in pixels.
 * @property aspectRatio [width] divided by [height], rounded to three places, for laying out space before the
 *     image loads.
 */
@Serializable
data class VerseImage(
    @SerialName("id") val id: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("passage_id") val passageId: String,
    @SerialName("attribution") val attribution: String,
    @SerialName("language_tag") val languageTag: String,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int,
    @SerialName("aspect_ratio") val aspectRatio: Double,
)
