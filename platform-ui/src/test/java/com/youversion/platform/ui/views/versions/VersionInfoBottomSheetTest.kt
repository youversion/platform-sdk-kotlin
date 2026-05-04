package com.youversion.platform.ui.views.versions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VersionInfoBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSheet(
        bibleVersion: BibleVersion = BibleVersion.preview,
        organization: Organization? = null,
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                VersionInfoBottomSheet(
                    bibleVersion = bibleVersion,
                    organization = organization,
                    onDismissRequest = onDismissRequest,
                )
            }
        }
    }

    // ----- VersionHeader

    @Test
    fun `displays localized abbreviation in header`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                ),
        )

        composeTestRule.onNodeWithText("KJV").assertIsDisplayed()
    }

    @Test
    fun `displays localized title in header`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                ),
        )

        composeTestRule.onNodeWithText("King James Version").assertIsDisplayed()
    }

    @Test
    fun `displays publisher name when organization is non-null`() {
        renderSheet(
            bibleVersion = BibleVersion.preview,
            organization = Organization.preview,
        )

        composeTestRule.onNodeWithText("Biblica").assertIsDisplayed()
    }

    @Test
    fun `does not display publisher name when organization is null`() {
        renderSheet(
            bibleVersion = BibleVersion.preview,
            organization = null,
        )

        composeTestRule.onNodeWithText("Biblica").assertDoesNotExist()
    }

    // ----- OfflineAgreement

    @Ignore("TODO: Re-enable when offline downloads UI is uncommented in VersionInfoBottomSheet")
    @Test
    fun `displays offline agreement text`() {
        renderSheet()

        composeTestRule
            .onNodeWithText(
                "Our agreement with the publisher",
                substring = true,
            ).assertIsDisplayed()
    }

    @Ignore("TODO: Re-enable when offline downloads UI is uncommented in VersionInfoBottomSheet")
    @Test
    fun `displays offline tagline`() {
        renderSheet()

        composeTestRule
            .onNodeWithText(
                "Read it anytime, anywhere",
                substring = true,
            ).assertIsDisplayed()
    }

    // ----- VersionCopyright

    @Test
    fun `displays promotionalContent when available`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                    promotionalContent = "Promotional text here",
                    copyright = "Copyright text",
                ),
        )

        composeTestRule.onNodeWithText("Promotional text here").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copyright text").assertDoesNotExist()
    }

    @Test
    fun `displays copyright when promotionalContent is null`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                    promotionalContent = null,
                    copyright = "Copyright 2024 Publisher",
                ),
        )

        composeTestRule.onNodeWithText("Copyright 2024 Publisher").assertIsDisplayed()
    }

    @Test
    fun `does not display copyright text when both promotionalContent and copyright are null`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                    promotionalContent = null,
                    copyright = null,
                ),
        )

        composeTestRule.onNodeWithText("Promotional text here").assertDoesNotExist()
        composeTestRule.onNodeWithText("Copyright 2024 Publisher").assertDoesNotExist()
    }

    // ----- VersionWebsite

    @Test
    fun `displays readerFooterUrl when non-null`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                    readerFooterUrl = "https://www.biblesociety.org.uk",
                ),
        )

        composeTestRule
            .onNodeWithText("https://www.biblesociety.org.uk")
            .assertIsDisplayed()
    }

    @Test
    fun `does not display website when readerFooterUrl is null`() {
        renderSheet(
            bibleVersion =
                BibleVersion(
                    id = 1,
                    localizedAbbreviation = "KJV",
                    localizedTitle = "King James Version",
                    readerFooterUrl = null,
                ),
        )

        composeTestRule
            .onNodeWithText("https://www.biblesociety.org.uk")
            .assertDoesNotExist()
    }

    // ----- PrimaryActions

    @Ignore("TODO: Re-enable when offline downloads UI is uncommented in VersionInfoBottomSheet")
    @Test
    fun `displays agree and download button`() {
        renderSheet()

        composeTestRule.onNodeWithText("Agree and Download").assertIsDisplayed()
    }

    @Ignore("TODO: Re-enable when offline downloads UI is uncommented in VersionInfoBottomSheet")
    @Test
    fun `displays maybe later button`() {
        renderSheet()

        composeTestRule.onNodeWithText("Maybe Later").assertIsDisplayed()
    }

    @Ignore("TODO: Re-enable when offline downloads UI is uncommented in VersionInfoBottomSheet")
    @Test
    fun `tapping maybe later calls onDismissRequest`() {
        var isDismissed = false

        renderSheet(onDismissRequest = { isDismissed = true })

        composeTestRule.onNodeWithText("Maybe Later").performClick()
        composeTestRule.waitForIdle()
        assertTrue(isDismissed)
    }
}
