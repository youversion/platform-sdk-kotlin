package com.youversion.platform.reader.screens.bible

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.Config
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleIntroRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.users.api.UsersApi
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVersionRepository = mockk<BibleVersionRepository>()
    private val mockChapterRepository = mockk<BibleChapterRepository>()
    private val mockIntroRepository = mockk<BibleIntroRepository>()

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private val genesisBook =
        BibleBook(
            id = "GEN",
            title = "Genesis",
            fullTitle = null,
            abbreviation = null,
            canon = null,
            chapters = null,
        )

    private val testVersion =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            books = listOf(genesisBook),
            copyright = "Public Domain",
        )

    private val stateFlow =
        MutableStateFlow(
            BibleReaderViewModel.State(bibleReference = defaultReference),
        )

    private val mockViewModel =
        mockk<BibleReaderViewModel>(relaxed = true) {
            every { state } returns stateFlow
        }

    @Before
    fun setUp() {
        mockkObject(BibleVersionRendering)
        PlatformKoinGraph.start(
            listOf(
                module {
                    single { mockVersionRepository }
                    single { mockChapterRepository }
                    single { mockIntroRepository }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
        unmockkObject(BibleVersionRendering)
        BibleReaderTheme.selectedColorScheme.value = null
    }

    private fun stubSuccessfulTextLoad() {
        coEvery { mockVersionRepository.version(any()) } returns testVersion
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf(mockk<BibleTextBlock>(relaxed = true))
    }

    private fun stubFailedTextLoad() {
        coEvery { mockVersionRepository.version(any()) } throws RuntimeException("Network error")
    }

    private fun stubNotPermittedTextLoad() {
        coEvery { mockVersionRepository.version(any()) } returns testVersion
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } throws BibleVersionApiException(BibleVersionApiException.Reason.NOT_PERMITTED)
    }

    // region Book Name & Chapter Display

    @Test
    fun `displays book name when version is available`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
    }

    @Test
    fun `displays chapter number`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `does not display book name or chapter number when version is null`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = null,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("Genesis").assertDoesNotExist()
        composeTestRule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun `displays intro label when viewing intro`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                introBookUSFM = "GEN",
                introPassageId = "intro-1",
            )
        coEvery { mockVersionRepository.version(any()) } returns testVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""
        coEvery {
            BibleVersionRendering.introTextBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("Intro").assertIsDisplayed()
    }

    @Test
    fun `displays passage selection with book and chapter`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("Genesis 1").assertIsDisplayed()
    }

    // endregion

    // region Copyright

    @Test
    fun `displays copyright when loading succeeds`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Public Domain").assertIsDisplayed()
    }

    @Test
    fun `displays promotional content when copyright is null`() {
        val versionWithPromo =
            BibleVersion(
                id = 1,
                abbreviation = "KJV",
                books = listOf(genesisBook),
                copyright = null,
                promotionalContent = "Promotional Info",
            )
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = versionWithPromo,
            )
        coEvery { mockVersionRepository.version(any()) } returns versionWithPromo
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf(mockk<BibleTextBlock>(relaxed = true))

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Promotional Info").assertIsDisplayed()
    }

    @Test
    fun `does not display copyright when loading fails`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubFailedTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Public Domain").assertDoesNotExist()
    }

    // endregion

    // region Banners

    @Test
    fun `shows offline banner when loading fails`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubFailedTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "You've lost your internet connection.",
                substring = true,
            )[0]
            .assertIsDisplayed()
    }

    @Test
    fun `shows version unavailable banner when not permitted`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubNotPermittedTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "Your previously selected Bible version is unavailable.",
                substring = true,
            )[0]
            .assertIsDisplayed()
    }

    @Test
    fun `does not show banner when loading succeeds`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection.",
                substring = true,
            ).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable.",
                substring = true,
            ).assertDoesNotExist()
    }

    @Test
    fun `banner dismisses when dismiss button is tapped`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubFailedTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()

        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithContentDescription("Dismiss")
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    // endregion

    // region Sheets

    @Test
    fun `shows font settings sheet when showingFontList is true`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                showingFontList = true,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Font").assertIsDisplayed()
    }

    @Test
    fun `tapping font button dispatches CloseFontSettings and invokes onFontsClick`() {
        var fontsClicked = false
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                showingFontList = true,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = { fontsClicked = true },
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Untitled Serif").performClick()
        composeTestRule.waitForIdle()

        verify { mockViewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings) }
        assertTrue(fontsClicked)
    }

    @Test
    fun `tapping theme dispatches SetReaderTheme action`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                showingFontList = true,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_2").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockViewModel.onAction(match { it is BibleReaderViewModel.Action.SetReaderTheme })
        }
    }

    @Test
    fun `tapping footnote dispatches OpenFootnotes and sheet displays`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )

        val footnoteText =
            buildAnnotatedString {
                append("This is a footnote")
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "1:GEN:1:1",
                    start = 0,
                    end = 18,
                )
            }
        val block =
            BibleTextBlock(
                text =
                    buildAnnotatedString {
                        append("※")
                        addStringAnnotation(
                            tag = BibleReferenceAttribute.NAME,
                            annotation = "1:GEN:1:1",
                            start = 0,
                            end = 1,
                        )
                        addStringAnnotation(
                            tag = BibleTextCategoryAttribute.NAME,
                            annotation = BibleTextCategory.FOOTNOTE_MARKER.name,
                            start = 0,
                            end = 1,
                        )
                    },
                chapter = 1,
                headIndent = 0.sp,
                marginTop = 8.dp,
                alignment = TextAlign.Start,
                footnotes = listOf(footnoteText),
            )
        coEvery { mockVersionRepository.version(any()) } returns testVersion
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf(block)

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("※").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockViewModel.onAction(match { it is BibleReaderViewModel.Action.OpenFootnotes })
        }

        stateFlow.value =
            stateFlow.value.copy(
                showingFootnotes = true,
                footnotes = listOf(AnnotatedString("Test footnote")),
                footnotesReference = defaultReference,
            )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test footnote").assertIsDisplayed()
    }

    @Test
    fun `tapping intro footnote dispatches OpenIntroFootnotes and sheet displays`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                introBookUSFM = "GEN",
                introPassageId = "intro-1",
            )

        val footnoteText =
            buildAnnotatedString {
                append("Intro footnote text")
            }
        val block =
            BibleTextBlock(
                text =
                    buildAnnotatedString {
                        append("※")
                        addStringAnnotation(
                            tag = BibleTextCategoryAttribute.NAME,
                            annotation = BibleTextCategory.FOOTNOTE_IMAGE.name,
                            start = 0,
                            end = 1,
                        )
                    },
                chapter = 1,
                headIndent = 0.sp,
                marginTop = 8.dp,
                alignment = TextAlign.Start,
                footnotes = listOf(footnoteText),
            )
        coEvery { mockVersionRepository.version(any()) } returns testVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""
        coEvery {
            BibleVersionRendering.introTextBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf(block)

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("※").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockViewModel.onAction(match { it is BibleReaderViewModel.Action.OpenIntroFootnotes })
        }

        stateFlow.value =
            stateFlow.value.copy(
                showingIntroFootnotes = true,
                introFootnotes = listOf(AnnotatedString("Test intro footnote")),
            )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test intro footnote").assertIsDisplayed()
    }

    // endregion

    // region Verse Tap Sign-In Gate

    private fun stubVersionWithTappableVerse() {
        val block =
            BibleTextBlock(
                text =
                    buildAnnotatedString {
                        append("In the beginning")
                        addStringAnnotation(
                            tag = BibleReferenceAttribute.NAME,
                            annotation = "1:GEN:1:1",
                            start = 0,
                            end = 16,
                        )
                    },
                chapter = 1,
                headIndent = 0.sp,
                marginTop = 8.dp,
                alignment = TextAlign.Start,
                footnotes = emptyList(),
            )
        coEvery { mockVersionRepository.version(any()) } returns testVersion
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf(block)
    }

    @Test
    fun `verse tap dispatches OnVerseTap when signed in`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubVersionWithTappableVerse()

        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        val configStateFlow =
            MutableStateFlow(
                Config(
                    appKey = "test",
                    authCallback = "",
                    apiHost = "",
                    hostEnv = null,
                    installId = null,
                    accessToken = "token",
                    refreshToken = null,
                    idToken = null,
                    expiryDate = null,
                ),
            )
        every { YouVersionPlatformConfiguration.configState } returns configStateFlow
        val mockUsersApi = mockk<UsersApi>(relaxed = true)
        every { YouVersionApi.users } returns mockUsersApi

        try {
            composeTestRule.setContent {
                BibleScreen(
                    viewModel = mockViewModel,
                    appName = "Test App",
                    appSignInMessage = "Sign in",
                    onReferencesClick = {},
                    onVersionsClick = {},
                    onFontsClick = {},
                )
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("In the beginning").performClick()
            composeTestRule.waitForIdle()

            verify {
                mockViewModel.onAction(match { it is BibleReaderViewModel.Action.OnVerseTap })
            }
        } finally {
            unmockkObject(YouVersionPlatformConfiguration)
            unmockkObject(YouVersionApi)
        }
    }

    @Test
    fun `verse tap does not dispatch OnVerseTap when not signed in`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubVersionWithTappableVerse()

        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        val configStateFlow =
            MutableStateFlow(
                Config(
                    appKey = "test",
                    authCallback = "",
                    apiHost = "",
                    hostEnv = null,
                    installId = null,
                    accessToken = null,
                    refreshToken = null,
                    idToken = null,
                    expiryDate = null,
                ),
            )
        every { YouVersionPlatformConfiguration.configState } returns configStateFlow
        val mockUsersApi = mockk<UsersApi>(relaxed = true)
        every { YouVersionApi.users } returns mockUsersApi

        try {
            composeTestRule.setContent {
                BibleScreen(
                    viewModel = mockViewModel,
                    appName = "Test App",
                    appSignInMessage = "Sign in",
                    onReferencesClick = {},
                    onVersionsClick = {},
                    onFontsClick = {},
                )
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("In the beginning").performClick()
            composeTestRule.waitForIdle()

            verify(exactly = 0) {
                mockViewModel.onAction(match { it is BibleReaderViewModel.Action.OnVerseTap })
            }
        } finally {
            unmockkObject(YouVersionPlatformConfiguration)
            unmockkObject(YouVersionApi)
        }
    }

    // endregion

    // region Sign-Out Confirmation

    private lateinit var mockUsersApi: UsersApi

    private fun stubSignedInConfig() {
        mockkObject(YouVersionPlatformConfiguration)
        mockkObject(YouVersionApi)
        val configStateFlow =
            MutableStateFlow(
                Config(
                    appKey = "test",
                    authCallback = "",
                    apiHost = "",
                    hostEnv = null,
                    installId = null,
                    accessToken = "token",
                    refreshToken = null,
                    idToken = null,
                    expiryDate = null,
                ),
            )
        every { YouVersionPlatformConfiguration.configState } returns configStateFlow
        every { YouVersionPlatformConfiguration.clearAuthData() } returns Unit
        coEvery { YouVersionApi.hasValidToken() } returns true
        mockUsersApi = mockk(relaxed = true)
        every { YouVersionApi.users } returns mockUsersApi
    }

    private fun cleanUpSignedInConfig() {
        unmockkObject(YouVersionPlatformConfiguration)
        unmockkObject(YouVersionApi)
    }

    @Test
    fun `sign out shows confirmation alert and cancel dismisses it`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()
        stubSignedInConfig()

        try {
            composeTestRule.setContent {
                BibleScreen(
                    viewModel = mockViewModel,
                    appName = "Test App",
                    appSignInMessage = "Sign in",
                    onReferencesClick = {},
                    onVersionsClick = {},
                    onFontsClick = {},
                )
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodesWithText("Sign Out")[0].performClick()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText("Are you sure you want to sign out", substring = true)
                .assertIsDisplayed()

            composeTestRule.onNodeWithText("Cancel").performClick()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText("Are you sure you want to sign out", substring = true)
                .assertDoesNotExist()
            verify(exactly = 0) { mockUsersApi.signOut() }
        } finally {
            cleanUpSignedInConfig()
        }
    }

    @Test
    fun `sign out confirmation triggers sign out`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()
        stubSignedInConfig()

        try {
            composeTestRule.setContent {
                BibleScreen(
                    viewModel = mockViewModel,
                    appName = "Test App",
                    appSignInMessage = "Sign in",
                    onReferencesClick = {},
                    onVersionsClick = {},
                    onFontsClick = {},
                )
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Fonts & Settings").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodesWithText("Sign Out")[0].performClick()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText("Are you sure you want to sign out", substring = true)
                .assertIsDisplayed()

            composeTestRule.onAllNodesWithText("Sign Out")[1].performClick()
            composeTestRule.waitForIdle()

            verify { mockUsersApi.signOut() }
        } finally {
            cleanUpSignedInConfig()
        }
    }

    // endregion

    // region Verse Action Sheet

    @Test
    fun `verse action sheet shows when showVerseActionSheet is true`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                showVerseActionSheet = true,
                selectedVerses = setOf(defaultReference.copy(verseStart = 1, verseEnd = 1)),
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithContentDescription("Copy")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Share").assertIsDisplayed()
    }

    @Test
    fun `verse action sheet is hidden when showVerseActionSheet is false`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                showVerseActionSheet = false,
                selectedVerses = emptySet(),
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Copy").assertIsNotDisplayed()
    }

    @Test
    fun `clears verse selection when bottom sheet is dismissed`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
                showVerseActionSheet = true,
                selectedVerses = setOf(defaultReference.copy(verseStart = 1, verseEnd = 1)),
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithContentDescription("Copy")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Copy").assertIsDisplayed()

        composeTestRule.onNodeWithTag("verse_action_sheet").performTouchInput {
            swipeDown(startY = 0f, endY = height.toFloat())
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Copy").assertIsNotDisplayed()

        verify { mockViewModel.onAction(BibleReaderViewModel.Action.ClearVerseSelection) }
    }

    // endregion

    // region Bottom Bar

    @Test
    fun `renders bottom bar when provided`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                bottomBar = { Text("Bottom Bar Content") },
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("Bottom Bar Content").assertIsDisplayed()
    }

    @Test
    fun `does not render bottom bar when null`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                bottomBar = null,
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithText("Bottom Bar Content").assertDoesNotExist()
    }

    // endregion

    // region Passage Selection

    @Test
    fun `previous chapter button dispatches GoToPreviousChapter action`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Previous Chapter").performClick()

        verify { mockViewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter) }
    }

    @Test
    fun `next chapter button dispatches GoToNextChapter action`() {
        stateFlow.value =
            BibleReaderViewModel.State(
                bibleReference = defaultReference,
                bibleVersion = testVersion,
            )
        stubSuccessfulTextLoad()

        composeTestRule.setContent {
            BibleScreen(
                viewModel = mockViewModel,
                appName = "Test App",
                appSignInMessage = "Sign in",
                onReferencesClick = {},
                onVersionsClick = {},
                onFontsClick = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Next Chapter").performClick()

        verify { mockViewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter) }
    }

    // endregion
}
