package com.youversion.platform.ui.content

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** The content language the library starts in. */
internal const val DEFAULT_CONTENT_LANGUAGE_TAG = "en"

/**
 * The languages the content language picker offers. YouVersion Platform plans an API that lists the available
 * content languages; until then, these are the languages that have verse images.
 */
internal val CONTENT_LANGUAGE_TAGS =
    listOf(
        "en",
        "es",
        "fr",
        "de",
        "it",
        "ja",
        "zh",
        "ru",
        "pt",
        "ko",
        "hi",
        "ar",
        "tr",
        "nl",
        "sv",
        "zh-CN",
        "zh-TW",
        "zh-HK",
    )

private const val VERSION_PAGE_SIZE = 99
private const val LANGUAGE_TAG_KEY = "contentLanguageTag"

private val versionsByLanguageTag = ConcurrentHashMap<String, List<BibleVersion>>()

/**
 * The Content tab's one content language, kept in [savedStateHandle] so it survives process death. Both libraries
 * read [languageTag].
 */
internal class ContentLanguageViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val languageTag: StateFlow<String> = savedStateHandle.getStateFlow(LANGUAGE_TAG_KEY, DEFAULT_CONTENT_LANGUAGE_TAG)

    /** Makes [languageTag] the content language. */
    fun select(languageTag: String) {
        savedStateHandle[LANGUAGE_TAG_KEY] = languageTag
    }
}

/** [languageTag]'s name in the device's language, such as "Spanish" for `es`, or the tag itself when unknown. */
internal fun contentLanguageName(languageTag: String): String =
    Locale.forLanguageTag(languageTag).getDisplayName(Locale.getDefault()).ifEmpty { languageTag }

/**
 * The id of a Bible in [languageTag] to title passages and search images with, or one that contains [passageId] when
 * it is given. English prefers the default version. Chinese regions fall back to their script, then to `zh`.
 *
 * @param versions The Bibles in a language; by default fetched once per language and kept for the process.
 * @throws IllegalStateException when no Bible in the language, or none with the passage, is available.
 */
internal suspend fun contentBibleId(
    languageTag: String,
    passageId: String? = null,
    versions: suspend (languageTag: String) -> List<BibleVersion> = ::cachedVersions,
): Int {
    val languageTags =
        when (languageTag) {
            "zh-CN" -> listOf(languageTag, "zh")
            "zh-TW", "zh-HK" -> listOf(languageTag, "zh-Hant", "zh")
            else -> listOf(languageTag)
        }
    for (tag in languageTags) {
        val candidates =
            versions(tag)
                .filter { version ->
                    if (passageId == null) return@filter true
                    val reference = BibleReference.unvalidatedReference(passageId, version.id) ?: return@filter false
                    version.bookCodes?.contains(reference.bookUSFM) ?: true
                }.sortedBy { it.id }
        if (tag == "en" && candidates.any { it.id == BibleDefaults.VERSION_ID }) return BibleDefaults.VERSION_ID
        val version =
            candidates.firstOrNull { it.languageTag.equals(tag, ignoreCase = true) } ?: candidates.firstOrNull()
        if (version != null) return version.id
    }
    val message =
        if (passageId == null) {
            "No Bible is available for the selected language."
        } else {
            "This passage is not available in the selected language."
        }
    error(message)
}

private suspend fun cachedVersions(languageTag: String): List<BibleVersion> =
    versionsByLanguageTag[languageTag] ?: fetchVersions(languageTag).also { versionsByLanguageTag[languageTag] = it }

private suspend fun fetchVersions(languageTag: String): List<BibleVersion> {
    val versions = mutableListOf<BibleVersion>()
    val visitedTokens = mutableSetOf<String>()
    var pageToken: String? = null
    do {
        val page = YouVersionApi.bible.versions(languageTag, pageSize = VERSION_PAGE_SIZE, pageToken = pageToken)
        versions += page.data
        pageToken = page.nextPageToken?.also { check(visitedTokens.add(it)) { "The versions repeated a page." } }
    } while (pageToken != null)
    return versions
}
