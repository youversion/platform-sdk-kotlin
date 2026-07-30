package com.youversion.platform.reader.domain

import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A highlight change the reader asked for but that is waiting on the highlights permission. Persisted so it survives
 * the reader being recreated while the permission grant happens in the browser, and applied once the grant lands.
 *
 * Named a request rather than a pending highlight because it is not a highlight yet and is not waiting on the server:
 * a highlight queued for the server is a `PendingHighlightOperation`, and the two have different lifetimes.
 */
@Serializable
internal data class HighlightRequest(
    val references: List<BibleReference>,
    val hexColor: String,
    val isRemoval: Boolean,
    val sessionId: String? = null,
)

/**
 * Responsible for fetching and managing data related to the Bible
 * Reader. Note that versions are used by the Bible Reader, but those
 * are managed by the BibleVersionRepository.
 *
 * A highlight request is bound to the session that made it via [YouVersionApi.currentSessionId], so it can never be
 * applied under another user. One made while signed out carries no stamp and is applied by whoever signs in next,
 * which is what it exists for; it is stamped with that session as soon as one is seen, so it stops there rather than
 * staying open to every session after it.
 *
 * Both a stamp and an observer are needed. [sessionIdChanges] clears the request when the reader leaves a session, but
 * only covers switches happening while this repository exists; the stamp also covers a switch across process death.
 * Emissions are compared against the session read at construction rather than the first emission, which can already be
 * the new session.
 */
class BibleReaderRepository internal constructor(
    private val storage: Storage,
    private val bibleVersionRepository: BibleVersionRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val currentSessionId: () -> String? = { YouVersionApi.currentSessionId },
    sessionIdChanges: Flow<String?> =
        YouVersionPlatformConfiguration.configState.map { currentSessionId() },
) {
    companion object {
        private const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
        private const val KEY_HIGHLIGHT_REQUEST = "bible-reader-view--highlight-request"
    }

    private val highlightRequestState = MutableStateFlow(storedHighlightRequest())

    init {
        bindHeldRequestToCurrentSession()
        var previousSessionId = currentSessionId()
        scope.launch {
            sessionIdChanges.collect { sessionId ->
                if (sessionId != previousSessionId) {
                    if (previousSessionId == null) {
                        bindHeldRequestToCurrentSession()
                    } else {
                        highlightRequest = null
                    }
                    previousSessionId = sessionId
                }
            }
        }
    }

    /**
     * Stamps a held request with the session signed in now, so "applied by whoever signs in next" means that one
     * sign-in and not every later one. Run when a sign-in is observed, and again at construction because the sign-in
     * that should have stamped a request can land while nothing is collecting — including after the process that held
     * it is gone.
     */
    private fun bindHeldRequestToCurrentSession() {
        highlightRequest
            ?.takeIf { it.sessionId != currentSessionId() }
            ?.let { highlightRequest = it }
    }

    /**
     * Returns the last Bible reference that the Reader was viewing.
     */
    var lastBibleReference: BibleReference?
        get() =
            storage
                .getStringOrNull(KEY_BIBLE_READER_REFERENCE)
                ?.let { Json.decodeFromString(it) }
        set(value) =
            storage
                .putString(KEY_BIBLE_READER_REFERENCE, value?.let { Json.encodeToString(it) })

    /**
     * A highlight change the reader requested that is waiting on the highlights permission, persisted so it outlives
     * the reader being recreated during the browser grant flow. Stamped on the way in with the session that asked for
     * it, and reads as absent under any other, so correctness does not depend on the clear having run.
     */
    internal var highlightRequest: HighlightRequest?
        get() = highlightRequestState.value?.takeIf { it.belongsToCurrentSession() }
        set(value) {
            val stamped = value?.copy(sessionId = currentSessionId())
            storage.putString(KEY_HIGHLIGHT_REQUEST, stamped?.let { Json.encodeToString(it) })
            highlightRequestState.value = stamped
        }

    /**
     * Emits the current [highlightRequest] and every later change to it. Collect this instead of holding a copy: it is
     * cleared when the session changes, and a separately held copy would go stale.
     */
    internal val highlightRequestChanges: StateFlow<HighlightRequest?> = highlightRequestState.asStateFlow()

    private fun storedHighlightRequest(): HighlightRequest? {
        val stored =
            storage
                .getStringOrNull(KEY_HIGHLIGHT_REQUEST)
                ?.let { Json.decodeFromString<HighlightRequest>(it) }
                ?: return null
        if (!stored.belongsToCurrentSession()) {
            storage.putString(KEY_HIGHLIGHT_REQUEST, null)
            return null
        }
        return stored
    }

    private fun HighlightRequest.belongsToCurrentSession(): Boolean =
        sessionId == null || sessionId == currentSessionId()

    /**
     * Always produces a valid BibleReference based on what is available.
     */
    fun produceBibleReference(bibleReference: BibleReference?): BibleReference =
        bibleReference // Always use the provided reference if available
            ?: lastBibleReference // If no provided reference, use the last saved reference
            ?: run {
                // Fallback to John 1. Attempt to use the first downloaded version.
                // If no versions have been downloaded, use BSB.
                val downloadedVersions = bibleVersionRepository.downloadedVersions
                val versionId = downloadedVersions.firstOrNull() ?: BibleDefaults.VERSION_ID
                BibleReference(
                    versionId = versionId,
                    bookUSFM = "JHN",
                    chapter = 1,
                )
            }

    fun previousChapter(
        version: BibleVersion?,
        bibleReference: BibleReference,
    ): BibleReference? {
        val books = version?.books ?: emptyList()
        val previousBookIndex =
            books.indexOfFirst { it.id == bibleReference.bookUSFM }
        return when {
            bibleReference.chapter > 1 -> {
                // We're navigating to a previous chapter inside the same book
                bibleReference.copy(chapter = bibleReference.chapter - 1)
            }

            previousBookIndex > 0 -> {
                // We're navigating to the last chapter in the previous book
                val previousBook = books[previousBookIndex - 1]
                val chapters = previousBook.chapters ?: return null
                val lastChapter = chapters.count()
                if (lastChapter < 1) {
                    return null
                }
                bibleReference.copy(
                    bookUSFM = previousBook.id ?: "",
                    chapter = lastChapter,
                )
            }

            else -> {
                // We're at the first chapter, intro, etc of the first book (e.g. Genesis 1)
                null
            }
        }
    }

    fun nextChapter(
        version: BibleVersion?,
        bibleReference: BibleReference,
    ): BibleReference? {
        val books = version?.books ?: emptyList()
        val currentBookIndex = books.indexOfFirst { it.id == bibleReference.bookUSFM }
        val currentBook = books.getOrNull(currentBookIndex)
        val lastChapter = currentBook?.chapters?.count() ?: 0

        return when {
            bibleReference.chapter < lastChapter -> {
                // We're navigating to the next chapter in the same book
                bibleReference.copy(chapter = bibleReference.chapter + 1)
            }

            currentBookIndex < books.count() - 1 -> {
                // We're navigating to the first chapter of the next book
                val nextBook = books.getOrNull(currentBookIndex + 1)
                bibleReference.copy(
                    bookUSFM = nextBook?.id ?: "",
                    chapter = 1,
                )
            }

            else -> {
                // We're at the end of the last book
                null
            }
        }
    }
}
