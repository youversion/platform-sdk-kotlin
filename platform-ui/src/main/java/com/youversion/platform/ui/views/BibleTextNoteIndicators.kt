package com.youversion.platform.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.core.notes.api.NoteChanges
import com.youversion.platform.core.notes.api.NotesApi
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.users.model.YouVersionPermissions
import com.youversion.platform.ui.R
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import kotlinx.coroutines.CancellationException

private const val NOTES_PAGE_SIZE = 99
private val NoteIndicatorColor = Color(red = 0.35f, green = 0.62f, blue = 0.86f, alpha = 0.55f)
private val HighlightedNoteIndicatorColor = Color(0xFF8E8E93).copy(alpha = 0.25f)

/**
 * Renders the chapter, or the passage within it, named by [reference] as [BibleText] does, and marks the number of
 * every verse the signed-in user has a note on in [reference]'s Bible version.
 *
 * Notes load only while a user is signed in and has granted
 * [READ_NOTES][com.youversion.platform.core.users.model.YouVersionPermissions.READ_NOTES], so request it when signing
 * in. Every page of the chapter's notes is loaded and the markers appear once they all have. A refresh keeps the markers
 * shown until it succeeds; a failed one leaves them as they were. Signing out, another account signing in, or the
 * server refusing access removes them. A note created, changed or deleted through
 * [YouVersionApi.notes][com.youversion.platform.core.api.YouVersionApi.notes] reloads the markers and the chapter's
 * highlights, since a note's color can set them.
 *
 * A marker is blue on a verse without a highlight and a translucent gray over one. Every other parameter behaves as in
 * [BibleText].
 *
 * @param onNoteIndicatorTap Callback invoked when a marker is tapped, with its verse and every note attached to that
 * verse, newest first. The tap does not reach [onVerseTap].
 */
@Composable
public fun BibleTextWithNoteIndicators(
    reference: BibleReference,
    textOptions: BibleTextOptions = BibleTextOptions(),
    selectedVerses: Set<BibleReference> = emptySet(),
    onVerseSelectedChange: (Set<BibleReference>) -> Unit = {},
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)? = null,
    onFootnoteTap: ((reference: BibleReference, footNotes: List<AnnotatedString>) -> Unit)? = null,
    placeholder: @Composable (BibleTextLoadingPhase) -> Unit = { StandardPlaceholder(it) },
    onStateChange: (BibleTextLoadingPhase) -> Unit = {},
    onBlocksChange: (List<BibleTextBlock>) -> Unit = {},
    onNoteIndicatorTap: (reference: BibleReference, notes: List<Note>) -> Unit,
) {
    val noteIndicators = rememberBibleNoteIndicators(reference = reference, textOptions = textOptions)
    BibleText(
        reference = reference,
        textOptions = textOptions,
        selectedVerses = selectedVerses,
        onVerseSelectedChange = onVerseSelectedChange,
        onVerseTap = onVerseTap,
        onFootnoteTap = onFootnoteTap,
        placeholder = placeholder,
        onStateChange = onStateChange,
        onBlocksChange = onBlocksChange,
        verseIndicatorStyle = noteIndicators::style,
        onVerseIndicatorTap = { verse ->
            noteIndicators.notes(verse).takeIf { it.isNotEmpty() }?.let { onNoteIndicatorTap(verse, it) }
        },
    )
}

/**
 * The signed-in user's notes in one chapter and the verse indicators that mark them. Build one with
 * [rememberBibleNoteIndicators] and pass [style] as a verse indicator style.
 */
@PlatformInternalApi
@Immutable
public class BibleNoteIndicators internal constructor(
    private val chapterNotes: List<Note>,
    private val highlights: List<BibleHighlight>,
    private val numberColor: Color,
    private val accessibilityLabelFormat: String,
) {
    /** The notes attached to a verse [reference] covers, newest first. */
    public fun notes(reference: BibleReference): List<Note> =
        chapterNotes.filter { note -> note.references.orEmpty().any { it.overlaps(reference) } }

    /** The indicator for [reference]'s number, or null when no note is attached to it. */
    public fun style(reference: BibleReference): BibleVerseIndicatorStyle? {
        if (notes(reference).isEmpty()) return null
        val isHighlighted = highlights.any { it.bibleReference.overlaps(reference) }
        return BibleVerseIndicatorStyle(
            backgroundColor = if (isHighlighted) HighlightedNoteIndicatorColor else NoteIndicatorColor,
            foregroundColor = numberColor,
            contentDescription = accessibilityLabelFormat.format(reference.verseStart ?: reference.asUSFM),
        )
    }
}

/**
 * Loads the signed-in user's notes in the chapter holding [reference], as [BibleTextWithNoteIndicators] describes,
 * and keeps them for as long as it is composed. A note change reported by [NoteChanges] also reloads the chapter's
 * highlights.
 */
@PlatformInternalApi
@Composable
public fun rememberBibleNoteIndicators(
    reference: BibleReference,
    textOptions: BibleTextOptions = BibleTextOptions(),
    notesApi: NotesApi = YouVersionApi.notes,
): BibleNoteIndicators {
    val chapter = BibleReference(reference.versionId, reference.bookUSFM, reference.chapter)
    val config by YouVersionPlatformConfiguration.configState.collectAsStateWithLifecycle()
    val sessionId = remember(config) { YouVersionApi.currentSessionId }
    val grantedPermissionValues = config?.grantedPermissionValues.orEmpty()
    val isSignedIn = config?.isSignedIn == true
    val hasReadAccess = isSignedIn && YouVersionPermissions.READ_NOTES in grantedPermissionValues
    val hasHighlightsAccess = isSignedIn && YouVersionPermissions.HIGHLIGHTS in grantedPermissionValues
    val changeCount by NoteChanges.changeCount.collectAsStateWithLifecycle()
    val highlightsRepository: BibleHighlightsRepository = PlatformKoinGraph.koinApplication.koin.get()
    val cachedHighlights by highlightsRepository.highlights.collectAsStateWithLifecycle()
    var loaded by remember { mutableStateOf<LoadedChapterNotes?>(null) }

    LaunchedEffect(chapter, sessionId, hasReadAccess, changeCount) {
        if (!hasReadAccess) {
            loaded = null
            return@LaunchedEffect
        }
        try {
            YouVersionApi.hasValidToken()
            loaded = LoadedChapterNotes(chapter, sessionId, notesApi.chapterNotes(chapter))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w("Failed to load notes for $chapter", e)
            val reason = (e as? YouVersionNetworkException)?.reason
            if (reason == YouVersionNetworkException.Reason.NOT_PERMITTED ||
                reason == YouVersionNetworkException.Reason.MISSING_AUTHENTICATION
            ) {
                loaded = null
            }
        }
    }

    var handledChangeCount by remember { mutableIntStateOf(changeCount) }
    LaunchedEffect(changeCount) {
        if (changeCount != handledChangeCount && hasHighlightsAccess) {
            highlightsRepository.ensureHighlightsForChapterLoaded(chapter, forceReload = true)
        }
        handledChangeCount = changeCount
    }

    val notes =
        loaded
            ?.takeIf { hasReadAccess && it.chapter == chapter && it.sessionId == sessionId }
            ?.notes
            .orEmpty()
    val highlights =
        remember(cachedHighlights, chapter, hasHighlightsAccess) {
            if (hasHighlightsAccess) cachedHighlights.filter { it.bibleReference.overlaps(chapter) } else emptyList()
        }
    val numberColor = textOptions.resolvedTextColor()
    val accessibilityLabelFormat = stringResource(R.string.notes_verse_indicator_accessibility_label)
    return remember(notes, highlights, numberColor, accessibilityLabelFormat) {
        BibleNoteIndicators(notes, highlights, numberColor, accessibilityLabelFormat)
    }
}

/** A chapter's notes as loaded for one session, so they are never shown for another chapter or account. */
private class LoadedChapterNotes(
    val chapter: BibleReference,
    val sessionId: String?,
    val notes: List<Note>,
)

/**
 * Every page of the notes attached to [chapter] in its Bible version, each note once, keeping only those whose
 * attachment is in [chapter]. Throws when the server repeats a page token rather than looping on it.
 */
internal suspend fun NotesApi.chapterNotes(chapter: BibleReference): List<Note> {
    val notes = mutableListOf<Note>()
    val seenPageTokens = mutableSetOf<String>()
    var pageToken: String? = null
    do {
        val page = notes(references = listOf(chapter), pageSize = NOTES_PAGE_SIZE, pageToken = pageToken)
        notes += page.data
        pageToken = page.nextPageToken
        check(pageToken == null || seenPageTokens.add(pageToken)) { "The server repeated a notes page token" }
    } while (pageToken != null)
    return notes.distinctBy { it.id }.filter { note -> note.references.orEmpty().any { it.overlaps(chapter) } }
}
