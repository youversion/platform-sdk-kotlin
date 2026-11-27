package com.youversion.platform.core.notes.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.api.buildYouVersionUrlString
import com.youversion.platform.core.api.failureException
import com.youversion.platform.core.api.pageToken
import com.youversion.platform.core.api.parameter
import com.youversion.platform.core.api.parseApiBody
import com.youversion.platform.core.api.requireValidPageSize
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.notes.models.NoteCreateRequest
import com.youversion.platform.core.notes.models.NoteStatus
import com.youversion.platform.core.notes.models.NoteUpdate
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object NotesEndpoints : NotesApi {
    private val httpClient: HttpClient
        get() = PlatformCoreKoinComponent.httpClient

    /** The notes collection URL, filtered and paged by the arguments that are not `null`. */
    fun notesUrl(
        versionId: Int? = null,
        passageId: String? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): String =
        buildYouVersionUrlString {
            path("/v1/notes")
            parameter("bible_id", versionId)
            parameter("passage_id", passageId)
            parameter("page_size", pageSize)
            pageToken(pageToken)
        }

    /** The URL of one note; [id] is encoded as a single path segment. */
    fun noteUrl(id: String): String =
        buildYouVersionUrlString {
            path("/v1/notes")
            appendPathSegments(listOf(id), encodeSlash = true)
        }

    override suspend fun notes(
        versionId: Int?,
        pageSize: Int?,
        pageToken: String?,
    ): PaginatedResponse<Note> = notesPage(notesUrl(versionId, null, pageSize, pageToken), pageSize)

    override suspend fun notes(
        references: List<BibleReference>,
        isAnyVersion: Boolean,
        pageSize: Int?,
        pageToken: String?,
    ): PaginatedResponse<Note> {
        val normalized =
            requireNotNull(normalizedNoteReferences(references, allowsChapter = true)) {
                "references must be nonempty and in one version, book and chapter"
            }
        val versionId = normalized.first().versionId.takeUnless { isAnyVersion }
        return notesPage(notesUrl(versionId, notePassageId(normalized), pageSize, pageToken), pageSize)
    }

    override suspend fun note(id: String): Note {
        require(id.isNotEmpty()) { "id must not be empty" }
        return parseApiBody<NoteBody>(send { httpClient.get(noteUrl(id)) }).toNote()
    }

    override suspend fun createNote(request: NoteCreateRequest): Note {
        require(request.content.isNotEmpty() || request.references != null) {
            "a note needs content or references"
        }
        val body =
            buildJsonObject {
                put("request_id", request.requestId)
                put("content", request.content)
                put("status", request.status.rawValue)
                request.title?.let { put("title", it) }
                request.color?.let { put("color", it) }
                request.references?.let { put("passage", notePassage(it)) }
                request.languageTag?.let { put("language_tag", it) }
                request.tags?.let { put("tags", JsonArray(it.map(::JsonPrimitive))) }
            }
        return parseApiBody<NoteBody>(send { httpClient.post(notesUrl()) { jsonBody(body) } }).toNote()
    }

    override suspend fun updateNote(
        id: String,
        update: NoteUpdate,
    ): Note {
        require(id.isNotEmpty()) { "id must not be empty" }
        require(update.content != "") { "content must not be empty" }
        val body =
            buildJsonObject {
                putField("title", update.title, ::JsonPrimitive)
                update.content?.let { put("content", it) }
                putField("color", update.color, ::JsonPrimitive)
                putField("passage", update.references, ::notePassage)
                putField("language_tag", update.languageTag, ::JsonPrimitive)
                putField("tags", update.tags) { tags -> JsonArray(tags.map(::JsonPrimitive)) }
                update.status?.let { put("status", it.rawValue) }
            }
        require(body.isNotEmpty()) { "update must change at least one field" }
        return parseApiBody<NoteBody>(send { httpClient.patch(noteUrl(id)) { jsonBody(body) } }).toNote()
    }

    override suspend fun deleteNote(id: String) {
        require(id.isNotEmpty()) { "id must not be empty" }
        send { httpClient.delete(noteUrl(id)) }
    }

    private suspend fun notesPage(
        url: String,
        pageSize: Int?,
    ): PaginatedResponse<Note> {
        requireValidPageSize(pageSize)
        val response = send { httpClient.get(url) }
        if (response.status == HttpStatusCode.NoContent) {
            return PaginatedResponse(emptyList())
        }
        val page = parseApiBody<PaginatedResponse<NoteBody>>(response)
        return PaginatedResponse(page.data.map { it.toNote() }, page.nextPageToken)
    }

    /**
     * Sends [request] for the signed-in user and returns its successful response.
     *
     * Notes authenticate the user with a bearer token, so a `401` means they are signed out or their access token
     * expired and is reported as
     * [MISSING_AUTHENTICATION][YouVersionNetworkException.Reason.MISSING_AUTHENTICATION], which a sign-in or token
     * refresh may resolve, rather than as the app-key failure the shared parsing reports. A `403` means the user has
     * not granted the notes permission the call needs and is
     * [NOT_PERMITTED][YouVersionNetworkException.Reason.NOT_PERMITTED].
     */
    private suspend fun send(request: suspend () -> HttpResponse): HttpResponse {
        if (YouVersionPlatformConfiguration.accessToken == null) {
            throw YouVersionNetworkException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)
        }
        val response = request()
        when {
            response.status == HttpStatusCode.Unauthorized -> {
                throw response.failureException(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION)
            }

            response.status == HttpStatusCode.Forbidden -> {
                throw response.failureException(YouVersionNetworkException.Reason.NOT_PERMITTED)
            }

            !response.status.isSuccess() -> {
                throw response.failureException()
            }
        }
        return response
    }

    private fun HttpRequestBuilder.jsonBody(body: JsonObject) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    /** Leaves out a `null` [field], sends JSON `null` for [NoteUpdate.Field.Clear], and encodes a replacement. */
    private fun <T> JsonObjectBuilder.putField(
        key: String,
        field: NoteUpdate.Field<T>?,
        encode: (T) -> JsonElement,
    ) {
        when (field) {
            null -> Unit
            NoteUpdate.Field.Clear -> put(key, JsonNull)
            is NoteUpdate.Field.Value -> put(key, encode(field.value))
        }
    }
}

/** A note as the API sends it; its passage stays raw so a malformed one does not fail the whole response. */
@Serializable
internal class NoteBody(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String,
    @SerialName("color") val color: String? = null,
    @SerialName("passage") val passage: JsonElement? = null,
    @SerialName("language_tag") val languageTag: String? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("status") val status: String,
    @SerialName("created_dt") val createdTime: String? = null,
    @SerialName("updated_dt") val updatedTime: String? = null,
) {
    /** This note as the public model, with an unreadable passage as `null` references. */
    fun toNote(): Note =
        Note(
            id = id,
            title = title,
            content = content,
            color = color,
            references = notePassageReferences(passage),
            languageTag = languageTag,
            tags = tags,
            status = NoteStatus(status),
            createdTime = createdTime,
            updatedTime = updatedTime,
        )
}
