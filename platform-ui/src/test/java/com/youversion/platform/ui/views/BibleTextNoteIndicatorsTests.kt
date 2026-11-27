package com.youversion.platform.ui.views

import androidx.compose.ui.test.junit4.createComposeRule
import com.youversion.platform.core.Config
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.core.notes.api.NoteChanges
import com.youversion.platform.core.notes.api.NotesApi
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.notes.models.NoteCreateRequest
import com.youversion.platform.core.notes.models.NoteStatus
import com.youversion.platform.core.notes.models.NoteUpdate
import com.youversion.platform.core.users.model.YouVersionPermissions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleTextNoteIndicatorsTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val chapter = BibleReference(111, "JHN", 3)

    private fun verse(number: Int) = BibleReference(111, "JHN", 3, number)

    private fun note(
        id: String,
        vararg references: BibleReference,
    ) = Note(
        id = id,
        title = null,
        content = "",
        color = null,
        references = references.toList().takeIf { it.isNotEmpty() },
        languageTag = null,
        tags = null,
        status = NoteStatus.PRIVATE,
        createdTime = null,
        updatedTime = null,
    )

    private class FakeNotesApi(
        var page: suspend (pageToken: String?) -> PaginatedResponse<Note>,
    ) : NotesApi {
        val requestedTokens = mutableListOf<String?>()

        override suspend fun notes(
            references: List<BibleReference>,
            isAnyVersion: Boolean,
            pageSize: Int?,
            pageToken: String?,
        ): PaginatedResponse<Note> {
            requestedTokens += pageToken
            return page(pageToken)
        }

        override suspend fun notes(
            versionId: Int?,
            pageSize: Int?,
            pageToken: String?,
        ): PaginatedResponse<Note> = error("unused")

        override suspend fun note(id: String): Note = error("unused")

        override suspend fun createNote(request: NoteCreateRequest): Note = error("unused")

        override suspend fun updateNote(
            id: String,
            update: NoteUpdate,
        ): Note = error("unused")

        override suspend fun deleteNote(id: String) = error("unused")
    }

    private val highlights = MutableStateFlow<List<BibleHighlight>>(emptyList())
    private val highlightsRepository = mockk<BibleHighlightsRepository>(relaxed = true)
    private val configState = MutableStateFlow<Config?>(null)

    private fun signIn(
        refreshToken: String = "user:a",
        permissions: Set<String> = setOf(YouVersionPermissions.READ_NOTES, YouVersionPermissions.HIGHLIGHTS),
    ) {
        configState.value =
            Config(
                appKey = "test",
                authCallback = "",
                apiHost = "",
                hostEnv = null,
                installId = null,
                accessToken = "token-$refreshToken",
                refreshToken = refreshToken,
                idToken = null,
                expiryDate = null,
                grantedPermissionValues = permissions,
            )
    }

    @Before
    fun setUp() {
        every { highlightsRepository.highlights } returns highlights
        PlatformKoinGraph.start(listOf(module { single { highlightsRepository } }))
        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        every { YouVersionPlatformConfiguration.configState } returns configState
        coEvery { YouVersionApi.hasValidToken() } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(YouVersionApi)
        unmockkObject(YouVersionPlatformConfiguration)
        PlatformKoinGraph.stop()
    }

    private fun render(notesApi: NotesApi): () -> BibleNoteIndicators {
        var indicators: BibleNoteIndicators? = null
        composeTestRule.setContent {
            indicators = rememberBibleNoteIndicators(reference = chapter, notesApi = notesApi)
        }
        composeTestRule.waitForIdle()
        return { composeTestRule.waitForIdle().let { assertNotNull(indicators) } }
    }

    @Test
    fun `chapterNotes follows every page and keeps each note in the chapter once`() =
        runTest {
            val inChapter = note("a", verse(1))
            val api =
                FakeNotesApi { token ->
                    when (token) {
                        null -> {
                            PaginatedResponse(
                                listOf(inChapter, note("other", BibleReference(111, "JHN", 4, 1))),
                                "p2",
                            )
                        }

                        "p2" -> {
                            PaginatedResponse(emptyList(), "p3")
                        }

                        else -> {
                            PaginatedResponse(listOf(inChapter, note("loose"), note("b", verse(7))))
                        }
                    }
                }

            assertEquals(listOf("a", "b"), api.chapterNotes(chapter).map { it.id })
            assertEquals(listOf(null, "p2", "p3"), api.requestedTokens)
        }

    @Test
    fun `chapterNotes fails rather than loop on a repeated page token`() =
        runTest {
            val api = FakeNotesApi { PaginatedResponse(emptyList(), "same") }

            assertFailsWith<IllegalStateException> { api.chapterNotes(chapter) }
        }

    @Test
    fun `marks every verse a range or gapped note covers and lists all notes on a verse`() {
        signIn()
        val gapped = note("gapped", BibleReference(111, "JHN", 3, 1, 2), verse(7))
        val single = note("single", verse(2))
        val indicators = render(FakeNotesApi { PaginatedResponse(listOf(gapped, single)) })

        assertNotNull(indicators().style(verse(1)))
        assertEquals(listOf("gapped", "single"), indicators().notes(verse(2)).map { it.id })
        assertNull(indicators().style(verse(3)))
        assertEquals(listOf("gapped"), indicators().notes(verse(7)).map { it.id })
    }

    @Test
    fun `a highlighted verse gets a different marker color`() {
        signIn()
        highlights.value = listOf(BibleHighlight(verse(2), "#ffec5b"))
        val indicators = render(FakeNotesApi { PaginatedResponse(listOf(note("n", verse(1), verse(2)))) })

        assertNotEquals(
            assertNotNull(indicators().style(verse(1))).backgroundColor,
            assertNotNull(indicators().style(verse(2))).backgroundColor,
        )
    }

    @Test
    fun `loads nothing without the read notes permission`() {
        signIn(permissions = setOf(YouVersionPermissions.HIGHLIGHTS))
        val api = FakeNotesApi { PaginatedResponse(listOf(note("n", verse(1)))) }
        val indicators = render(api)

        assertNull(indicators().style(verse(1)))
        assertTrue(api.requestedTokens.isEmpty())
    }

    @Test
    fun `a failed refresh keeps the markers and a refused one clears them`() {
        signIn()
        val api = FakeNotesApi { PaginatedResponse(listOf(note("n", verse(1)))) }
        val indicators = render(api)
        assertNotNull(indicators().style(verse(1)))

        api.page = { throw YouVersionNetworkException(YouVersionNetworkException.Reason.SERVER_ERROR) }
        NoteChanges.notifyChanged()
        assertNotNull(indicators().style(verse(1)))

        api.page = { throw YouVersionNetworkException(YouVersionNetworkException.Reason.NOT_PERMITTED) }
        NoteChanges.notifyChanged()
        assertNull(indicators().style(verse(1)))
    }

    @Test
    fun `another account signing in never sees the previous account's notes`() {
        signIn("user:a")
        val api = FakeNotesApi { PaginatedResponse(listOf(note("a", verse(1)))) }
        val indicators = render(api)
        assertNotNull(indicators().style(verse(1)))

        val accountBNotes = CompletableDeferred<PaginatedResponse<Note>>()
        api.page = { accountBNotes.await() }
        signIn("user:b")
        assertNull(indicators().style(verse(1)))

        accountBNotes.complete(PaginatedResponse(listOf(note("b", verse(5)))))
        assertEquals(listOf("b"), indicators().notes(verse(5)).map { it.id })

        configState.value = null
        assertTrue(indicators().notes(verse(5)).isEmpty())
    }

    @Test
    fun `a note change reloads the notes and forces a highlights reload for the chapter`() {
        signIn()
        val api = FakeNotesApi { PaginatedResponse(listOf(note("a", verse(1)))) }
        val indicators = render(api)
        verify(exactly = 0) { highlightsRepository.ensureHighlightsForChapterLoaded(any(), forceReload = true) }

        api.page = { PaginatedResponse(listOf(note("b", verse(4)))) }
        NoteChanges.notifyChanged()

        assertNull(indicators().style(verse(1)))
        assertNotNull(indicators().style(verse(4)))
        verify(exactly = 1) { highlightsRepository.ensureHighlightsForChapterLoaded(chapter, forceReload = true) }
    }
}
