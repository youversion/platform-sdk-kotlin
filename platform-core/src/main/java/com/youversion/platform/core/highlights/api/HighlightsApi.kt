package com.youversion.platform.core.highlights.api

import com.youversion.platform.core.highlights.models.Highlight

interface HighlightsApi {
    /**
     * Creates a new Bible highlight on YouVersion.
     *
     * This function creates a highlight for the specified passage using the provided parameters.
     *
     * @param versionId: The ID of the Bible version.
     * @param passageId: The passage identifier (e.g., "JHN.5.1").
     * @param color: The hex color code for the highlight (e.g., "eeeeff").
     * @return: A boolean indicating whether the highlight was successfully created.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun createHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean

    /**
     * Retrieves highlights for a specific Bible chapter from YouVersion.
     *
     * This function fetches highlights for the chapter identified by the provided `passageId` and `versionId`
     * for the authenticated user.
     *
     * A valid `YouVersionPlatformConfiguration.appKey` must be set before calling this function.
     *
     * @param versionId: The ID of the Bible version to fetch highlights for.
     * @param passageId: The passage identifier (e.g., "JHN.5").
     * @returns: An array of highlight data representing the user's highlights in the specified chapter.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun highlights(
        versionId: Int,
        passageId: String,
    ): List<Highlight>

    /**
     * Updates an existing Bible highlight on YouVersion.
     *
     * This function updates the color of an existing highlight for the specified passage.
     *
     * @param versionId: The ID of the Bible version.
     * @param passageId: The passage identifier (e.g., "JHN.5.1").
     * @param color: The new hex color code for the highlight (e.g., "eeeeff").
     * @returns: A boolean indicating whether the highlight was successfully updated.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun updateHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean

    /**
     * Deletes a Bible highlight from YouVersion.
     *
     * This function removes the highlight for the specified passage.
     * A valid `YouVersionPlatformConfiguration.appKey` must be set before calling this function.
     *
     * @param versionId: The ID of the Bible version.
     * @param passageId: The passage identifier (e.g., "JHN.5.1").
     * @returns: A boolean indicating whether the highlight was successfully deleted.
     * @throws [com.youversion.platform.core.api.YouVersionNetworkException] for any invalid request or response.
     */
    suspend fun deleteHighlight(
        versionId: Int,
        passageId: String,
    ): Boolean
}
