package com.youversion.platform.core.notes.api

import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.notes.models.NoteCreateRequest
import com.youversion.platform.core.notes.models.NoteUpdate

/**
 * The signed-in user's notes.
 *
 * Every call needs a valid `YouVersionPlatformConfiguration.appKey` and a signed-in user. Reading needs the user to
 * have granted [READ_NOTES][com.youversion.platform.core.users.model.SignInWithYouVersionPermission.READ_NOTES], and
 * creating, updating and deleting need
 * [WRITE_NOTES][com.youversion.platform.core.users.model.SignInWithYouVersionPermission.WRITE_NOTES]. Request both
 * when signing in. Call `YouVersionApi.hasValidToken()` first to refresh an access token that is about to expire.
 *
 * Every call throws [com.youversion.platform.core.api.YouVersionNetworkException] with reason
 * [MISSING_AUTHENTICATION][com.youversion.platform.core.api.YouVersionNetworkException.Reason.MISSING_AUTHENTICATION]
 * when no user is signed in or the server answers `401`, which a sign-in or token refresh may resolve; an invalid app
 * key also answers `401`. It throws with reason
 * [NOT_PERMITTED][com.youversion.platform.core.api.YouVersionNetworkException.Reason.NOT_PERMITTED] when the server
 * answers `403` because the user has not granted the notes permission the call needs, which a retry does not change;
 * and for any other invalid request or response.
 */
public interface NotesApi {
    /**
     * Fetches one page of the user's notes, newest first.
     *
     * Follow [PaginatedResponse.nextPageToken] until it is `null`, even past an empty page, and keep [versionId] the
     * same on every page; [pageSize] may change. A response with no content is an empty last page.
     *
     * @param versionId Only notes attached to this Bible version. `null` returns every note, including notes with no
     *     attachment.
     * @param pageSize The number of notes to return. When supplied, it must be between 1 and 99. `null` uses the
     *     platform default.
     * @param pageToken The [PaginatedResponse.nextPageToken] of the previous page.
     * @return The page of [Note]s. [PaginatedResponse.totalSize] is always `null`.
     * @throws IllegalArgumentException if [pageSize] is outside the range stated above.
     */
    public suspend fun notes(
        versionId: Int? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): PaginatedResponse<Note>

    /**
     * Fetches one page of the user's notes attached to any of [references], newest first.
     *
     * Paging works as for the other [notes]; keep [references] and [isAnyVersion] the same on every page.
     *
     * @param references Verses, verse ranges or one whole chapter, all in one version, book and chapter. Gaps between
     *     them are kept.
     * @param isAnyVersion Whether to match these verses in every Bible version rather than only in the references'
     *     version.
     * @param pageSize The number of notes to return. When supplied, it must be between 1 and 99. `null` uses the
     *     platform default.
     * @param pageToken The [PaginatedResponse.nextPageToken] of the previous page.
     * @return The page of [Note]s. [PaginatedResponse.totalSize] is always `null`.
     * @throws IllegalArgumentException if [references] is empty or spans more than one version, book or chapter, or
     *     [pageSize] is outside the range stated above.
     */
    public suspend fun notes(
        references: List<BibleReference>,
        isAnyVersion: Boolean = false,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): PaginatedResponse<Note>

    /**
     * Fetches one of the user's notes.
     *
     * @param id The note's id. Opaque; it is encoded as given.
     * @return The [Note].
     * @throws IllegalArgumentException if [id] is empty.
     */
    public suspend fun note(id: String): Note

    /**
     * Creates a note. Retry a failed create with the same [request] so the server creates it at most once.
     *
     * @param request The note to create.
     * @return The created [Note].
     * @throws IllegalArgumentException if [NoteCreateRequest.content] is empty, or [NoteCreateRequest.references] is
     *     empty, spans more than one version, book or chapter, or names a whole chapter.
     */
    public suspend fun createNote(request: NoteCreateRequest): Note

    /**
     * Changes the fields [update] names and leaves the others unchanged.
     *
     * @param id The note's id. Opaque; it is encoded as given.
     * @param update The fields to change.
     * @return The updated [Note].
     * @throws IllegalArgumentException if [id] is empty, [update] changes nothing, [NoteUpdate.content] is empty, or
     *     [NoteUpdate.references] replaces the attachment with references that are empty, span more than one
     *     version, book or chapter, or name a whole chapter.
     */
    public suspend fun updateNote(
        id: String,
        update: NoteUpdate,
    ): Note

    /**
     * Deletes one of the user's notes. A note that does not exist throws with
     * [statusCode][com.youversion.platform.core.api.YouVersionNetworkException.statusCode] `404`.
     *
     * @param id The note's id. Opaque; it is encoded as given.
     * @throws IllegalArgumentException if [id] is empty.
     */
    public suspend fun deleteNote(id: String)
}
