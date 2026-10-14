package com.youversion.platform.core.verseimages.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.verseimages.models.VerseImage

interface VerseImagesApi {
    /**
     * Retrieves a page of verse art, optionally narrowed to a passage, a language, or both.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * A filter that matches nothing returns an empty page rather than failing.
     *
     * @param passageId A single verse or verse range, such as `JHN.3.16` or `1CO.13.4-7`, to return only the art
     *     illustrating it. A whole chapter such as `JHN.3` is not accepted and fails the request.
     * @param languageRanges BCP 47 language ranges in preference order, to return only art in those languages. `*`
     *     matches every language. Empty applies no language filter.
     * @param pageSize The number of images to return. When supplied, it must be between 1 and 99.
     * @param pageToken A [PaginatedResponse.nextPageToken] from an earlier call, to continue where it left off.
     * @return The matching [VerseImage]s. [PaginatedResponse.totalSize] is known only when [passageId] is supplied,
     *     and is `null` otherwise.
     * @throws IllegalArgumentException if [pageSize] is outside the range stated above.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun verseImages(
        passageId: String? = null,
        languageRanges: List<String> = emptyList(),
        pageSize: Int? = null,
        pageToken: String? = null,
    ): PaginatedResponse<VerseImage>

    /**
     * Retrieves the verse art identified by [imageId].
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set for the request to succeed.
     *
     * @param imageId The opaque identifier of the image, such as a [VerseImage.id] from an earlier call. It must not
     *     be empty.
     * @return The [VerseImage] the identifier names.
     * @throws IllegalArgumentException if [imageId] is empty.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun verseImage(imageId: String): VerseImage
}
