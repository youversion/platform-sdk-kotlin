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
 */
@Serializable
internal data class PendingHighlight(
    val references: List<BibleReference>,
    val hexColor: String,
    val isRemoval: Boolean,
    val accountId: String? = null,
)

/**
 * Responsible for fetching and managing data related to the Bible
 * Reader. Note that versions are used by the Bible Reader, but those
 * are managed by the BibleVersionRepository.
 *
 * The pending highlight is cleared automatically when the reader leaves a signed-in account, so a highlight one user
 * asked for cannot be applied to the account that signs in next. [accountIdChanges] is observed for that, and only a
 * departure from an account clears it — signing out, or switching to another user.
 *
 * Three kinds of emission deliberately leave it alone. Signing in, because a request held while signed out exists
 * precisely to be applied once sign-in completes. The first value observed, which reports who is already signed in
 * rather than any change. And a repeat of the same account, which the config state emits whenever a permission grant
 * lands — the very moment a pending highlight is waiting for.
 *
 * Observing changes only covers the account switches that happen while this repository exists, and the request outlives
 * it. So each stored request is also stamped with the account that asked for it, and one whose stamp does not match the
 * signed-in account is discarded when it is loaded: the account can change across process death, or before the reader
 * is ever opened, and neither is a transition anything here saw. A request stamped with no account was made while
 * signed out, and exists to be applied by whoever signs in next.
 */
class BibleReaderRepository internal constructor(
    private val storage: Storage,
    private val bibleVersionRepository: BibleVersionRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val currentAccountId: () -> String? = { YouVersionApi.users.currentUserId },
    accountIdChanges: Flow<String?> =
        YouVersionPlatformConfiguration.configState.map { currentAccountId() },
) {
    companion object {
        private const val KEY_BIBLE_READER_REFERENCE = "bible-reader-view--reference"
        private const val KEY_PENDING_HIGHLIGHT = "bible-reader-view--pending-highlight"
    }

    private val pendingHighlightState = MutableStateFlow(storedPendingHighlight())

    init {
        scope.launch {
            var previousAccountId: String? = null
            accountIdChanges.collect { accountId ->
                if (previousAccountId != null && previousAccountId != accountId) {
                    pendingHighlight = null
                }
                previousAccountId = accountId
            }
        }
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
     */
    internal var pendingHighlight: PendingHighlight?
        get() = pendingHighlightState.value
        set(value) {
            val stamped = value?.copy(accountId = currentAccountId())
            storage.putString(KEY_PENDING_HIGHLIGHT, stamped?.let { Json.encodeToString(it) })
            pendingHighlightState.value = stamped
        }

    /**
     * Emits the current [pendingHighlight] and every later change to it. Collect this instead of holding a copy: it is
     * cleared when the signed-in account changes, and a separately held copy would go stale and could be applied under
     * the account that signed in next.
     */
    internal val pendingHighlightChanges: StateFlow<PendingHighlight?> = pendingHighlightState.asStateFlow()

    private fun storedPendingHighlight(): PendingHighlight? {
        val stored =
            storage
                .getStringOrNull(KEY_PENDING_HIGHLIGHT)
                ?.let { Json.decodeFromString<PendingHighlight>(it) }
                ?: return null
        if (stored.accountId != null && stored.accountId != currentAccountId()) {
            storage.putString(KEY_PENDING_HIGHLIGHT, null)
            return null
        }
        return stored
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
