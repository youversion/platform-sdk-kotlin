package com.youversion.platform.core.languages.domain

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import java.util.Locale
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageRepositoryTest : YouVersionPlatformTest {
    private lateinit var memoryCache: BibleVersionCache
    private lateinit var temporaryCache: BibleVersionCache
    private lateinit var persistentCache: BibleVersionCache
    private lateinit var bibleVersionRepository: BibleVersionRepository
    private lateinit var repository: LanguageRepository
    private lateinit var savedLocale: Locale

    @BeforeTest
    fun setup() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        memoryCache = BibleVersionMemoryCache()
        temporaryCache = BibleVersionMemoryCache()
        persistentCache = BibleVersionMemoryCache()
        bibleVersionRepository =
            BibleVersionRepository(
                memoryCache = memoryCache,
                temporaryCache = temporaryCache,
                persistentCache = persistentCache,
            )
        repository = LanguageRepository(bibleVersionRepository)
    }

    @AfterTest
    fun teardown() {
        Locale.setDefault(savedLocale)
        stopYouVersionPlatformTest()
    }

    // ----- allPermittedLanguageTags

    @Test
    fun `test allPermittedLanguageTags returns distinct tags from permittedVersions`() =
        runTest {
            MockEngine { _ ->
                respondJson(
                    """
                    {
                        "data": [
                            {"id": 1, "language_tag": "en"},
                            {"id": 2, "language_tag": "en"},
                            {"id": 3, "language_tag": "fr"}
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(listOf("en", "fr"), repository.allPermittedLanguageTags())
        }

    // ----- suggestedLanguageTags

    @Test
    fun `test suggestedLanguageTags falls back to en and es when API returns empty`() =
        runTest {
            MockEngine { _ ->
                respondJson("""{"data": []}""")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(listOf("en", "es"), repository.suggestedLanguageTags())
        }

    @Test
    fun `test suggestedLanguageTags filters codes by permitted versions`() =
        runTest {
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/bibles" -> respondJson("""{"data": [{"id": 1, "language_tag": "en"}]}""")
                    "/v1/languages" ->
                        respondJson(
                            """
                            {
                                "data": [
                                    {"id": "en", "language": "en"},
                                    {"id": "de", "language": "de"}
                                ]
                            }
                            """.trimIndent(),
                        )
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            bibleVersionRepository.permittedVersionsListing()

            assertEquals(listOf("en"), repository.suggestedLanguageTags())
        }

    @Test
    fun `test suggestedLanguageTags returns all codes when permittedVersions is empty`() =
        runTest {
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/bibles" -> respondJson("""{"data": []}""")
                    "/v1/languages" ->
                        respondJson(
                            """
                            {
                                "data": [
                                    {"id": "en", "language": "en"},
                                    {"id": "de", "language": "de"}
                                ]
                            }
                            """.trimIndent(),
                        )
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            bibleVersionRepository.permittedVersionsListing()

            assertEquals(listOf("en", "de"), repository.suggestedLanguageTags())
        }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `test suggestedLanguageTags is cached on second call`() =
        runTest {
            val languagesRequestCount = AtomicInt(0)
            val biblesRequestCount = AtomicInt(0)
            MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/languages" -> {
                        languagesRequestCount.incrementAndFetch()
                        respondJson("""{"data": [{"id": "en", "language": "en"}]}""")
                    }
                    "/v1/bibles" -> {
                        biblesRequestCount.incrementAndFetch()
                        respondJson("""{"data": [{"id": 1, "language_tag": "en"}]}""")
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val first = repository.suggestedLanguageTags()
            val second = repository.suggestedLanguageTags()

            assertEquals(first, second)
            assertEquals(1, languagesRequestCount.load())
            assertEquals(1, biblesRequestCount.load())
        }

    // ----- languageName display-name preference

    @Test
    fun `test languageName prefers locale language code in displayNames`() =
        runTest {
            MockEngine { _ ->
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "de",
                                "language": "de",
                                "display_names": {"en": "German (en)", "de": "German (de)"}
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            repository.loadLanguageNames(null)

            assertEquals("German (en)", repository.languageName("de"))
        }

    @Test
    fun `test languageName prefers version languageTag when locale does not match`() =
        runTest {
            Locale.setDefault(Locale.FRANCE)

            MockEngine { _ ->
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "de",
                                "language": "de",
                                "display_names": {"xx": "Wrong", "de": "Deutsch"}
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            repository.loadLanguageNames(BibleVersion(id = 1, languageTag = "de"))

            assertEquals("Deutsch", repository.languageName("de"))
        }

    @Test
    fun `test languageName falls back to en when locale and version tag miss`() =
        runTest {
            Locale.setDefault(Locale.FRANCE)

            MockEngine { _ ->
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "de",
                                "language": "de",
                                "display_names": {"xx": "Wrong", "en": "German"}
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            repository.loadLanguageNames(BibleVersion(id = 1, languageTag = "yy"))

            assertEquals("German", repository.languageName("de"))
        }

    @Test
    fun `test languageName falls back to first displayNames entry when no preferred key matches`() =
        runTest {
            Locale.setDefault(Locale.FRANCE)

            MockEngine { _ ->
                respondJson(
                    """
                    {
                        "data": [
                            {
                                "id": "de",
                                "language": "de",
                                "display_names": {"aa": "First", "bb": "Second"}
                            }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            repository.loadLanguageNames(BibleVersion(id = 1, languageTag = "yy"))

            assertEquals("First", repository.languageName("de"))
        }

    @Test
    fun `test languageName falls back to JVM display language when not cached`() {
        assertEquals("French", repository.languageName("fr"))
    }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `test loadLanguageNames is no-op when already populated`() =
        runTest {
            val requestCount = AtomicInt(0)
            MockEngine { _ ->
                requestCount.incrementAndFetch()
                respondJson(
                    """
                    {
                        "data": [
                            {"id": "en", "language": "en", "display_names": {"en": "English"}}
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            repository.loadLanguageNames(null)
            repository.loadLanguageNames(null)

            assertEquals(1, requestCount.load())
        }
}
