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
    val accountId: String? = null,
    val isRequestedWhileSignedIn: Boolean = true,
)

/**
 * Responsible for fetching and managing data related to the Bible
 * Reader. Note that versions are used by the Bible Reader, but those
 * are managed by the BibleVersionRepository.
 *
 * Which session the reader is in is the pair [isSignedIn] and [currentAccountId], not the account alone: a host app
 * that supplies its own tokens can leave the reader signed in on an access token with no ID token to name the account,
 * so a missing account means either signed out or signed in as someone this SDK cannot name. Those two are treated
 * differently everywhere below, and reading only the account would collapse them into the permissive one.
 *
 * The highlight request is cleared automatically when the reader leaves the session that made it, so a highlight one
 * user asked for cannot be applied to the account that signs in next. [accountIdChanges] is observed for that, and only
 * a departure from a session clears it — signing out, or switching to another user. The session those emissions are
 * compared against starts as the one in place when this repository is constructed, read there rather than from the
 * first emission: it can change between construction and the collector starting, and taking the first emission as the
 * baseline would adopt the new session instead of noticing the departure from the old one.
 *
 * Two kinds of emission deliberately leave it alone. Signing in, because a request held while signed out exists
 * precisely to be applied once sign-in completes. And a repeat of the same session, which the config state emits
 * whenever a permission grant lands — the very moment a highlight request is waiting for.
 *
 * Observing changes only covers the account switches that happen while this repository exists, and the request outlives
 * it. So each stored request is also stamped with the account that asked for it, and one whose stamp does not match the
 * signed-in account is never handed out: it is discarded when loaded from storage, and reads as absent until the clear
 * catches up. The account can change across process death, or before the reader is ever opened, and neither is a
 * transition anything here saw.
 *
 * So a request records the whole session that made it, and is handed back only to that session. One made while signed
 * out is applied by whoever signs in next, which is what it exists for. One made while signed in under an account this
 * SDK cannot name stays applicable only while that same nameless session lasts: it is refused once anyone nameable
 * signs in, and refused once the reader signs out.
 */
class BibleReaderRepository internal constructor(
    private val storage: Storage,
    private val bibleVersionRepository: BibleVersionRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val currentAccountId: () -> String? = { YouVersionApi.users.currentUserId },
    private val isSignedIn: () -> Boolean = { YouVersionApi.isSignedIn },
    accountIdChanges: Flow<String?> =
        YouVersionPlatformConfiguration.configState.map { currentAccountId() },
) {
    companion object {
        private const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
        private const val KEY_HIGHLIGHT_REQUEST = "bible-reader-view--highlight-request"
    }

    private val highlightRequestState = MutableStateFlow(storedHighlightRequest())

    init {
        var previousAccountId = currentAccountId()
        var wasSignedIn = isSignedIn()
        scope.launch {
            accountIdChanges.collect { accountId ->
                val isSignedInNow = isSignedIn()
                when {
                    wasSignedIn && (previousAccountId != accountId || !isSignedInNow) -> highlightRequest = null
                    !wasSignedIn && isSignedInNow -> bindHeldRequestToCurrentSession()
                }
                previousAccountId = accountId
                wasSignedIn = isSignedInNow
            }
        }
    }

    /**
     * Re-stamps a held request with the session that just signed in, so "applied by whoever signs in next" means that
     * one sign-in and not every later one. Without this a request made while signed out stays unstamped after being
     * answered, and the account that signs in after *that* could claim it too.
     */
    private fun bindHeldRequestToCurrentSession() {
        highlightRequest?.let { highlightRequest = it }
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
     * the reader being recreated during the browser grant flow. Stamped on the way in with the account that is signed
     * in when it is set, which is the account that asked for it.
     *
     * A request another account asked for reads as absent, so a reader constructed between the account changing and
     * the clear that change triggers cannot restore it. Correctness does not depend on that clear having run.
     */
    internal var highlightRequest: HighlightRequest?
        get() = highlightRequestState.value?.takeIf { it.belongsToCurrentSession() }
        set(value) {
            val isRequestedWhileSignedIn = isSignedIn()
            val stamped =
                value?.copy(
                    accountId = if (isRequestedWhileSignedIn) currentAccountId() else null,
                    isRequestedWhileSignedIn = isRequestedWhileSignedIn,
                )
            storage.putString(KEY_HIGHLIGHT_REQUEST, stamped?.let { Json.encodeToString(it) })
            highlightRequestState.value = stamped
        }

    /**
     * Emits the current [highlightRequest] and every later change to it. Collect this instead of holding a copy: it is
     * cleared when the signed-in account changes, and a separately held copy would go stale and could be applied under
     * the account that signed in next.
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
        if (isRequestedWhileSignedIn) {
            isSignedIn() && accountId == currentAccountId()
        } else {
            accountId == null
        }

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
