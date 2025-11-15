package com.youversion.platform.helpers

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.utilities.dependencies.DateSerializer
import com.youversion.platform.core.utilities.dependencies.Store
import com.youversion.platform.core.utilities.koin.YouVersionPlatformTools
import com.youversion.platform.core.utilities.koin.startYouVersionPlatform
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.util.Date

interface YouVersionPlatformTest : KoinTest {
    override fun getKoin(): Koin = YouVersionPlatformTools.defaultContext().get()
}

internal fun startYouVersionPlatformTest(engine: HttpClientEngine = MockEngine.create { addHandler { respondOk() } }) =
    startYouVersionPlatform(
        listOf(
            module {
                single<HttpClientEngine> { engine }
                single<Store> { TestStore() }
            },
        ),
    )

internal fun stopYouVersionPlatformTest() = YouVersionPlatformTools.defaultContext().stopKoin()

/**
 * An in-memory [Store] implementation.
 */
class TestStore : Store {
    private val prefs: MutableMap<String, String> = mutableMapOf()

    override var installId: String?
        get() = prefs[Store.KEY_INSTALL_ID]
        set(value) {
            value?.let { prefs[Store.KEY_INSTALL_ID] = it }
                ?: prefs.remove(Store.KEY_INSTALL_ID)
        }
    override var accessToken: String?
        get() = prefs[Store.KEY_ACCESS_TOKEN]
        set(value) {
            value?.let { prefs[Store.KEY_ACCESS_TOKEN] = it }
                ?: prefs.remove(Store.KEY_ACCESS_TOKEN)
        }

    override var refreshToken: String?
        get() = prefs[Store.KEY_REFRESH_TOKEN]
        set(value) {
            value?.let { prefs[Store.KEY_REFRESH_TOKEN] = it }
                ?: prefs.remove(Store.KEY_REFRESH_TOKEN)
        }

    override var expiryDate: Date?
        get() = prefs[Store.KEY_EXPIRY_DATE]?.let { Json.decodeFromString(DateSerializer, it) }
        set(value) {
            value?.let { prefs[Store.KEY_EXPIRY_DATE] = Json.encodeToString(DateSerializer, it) }
                ?: prefs.remove(Store.KEY_EXPIRY_DATE)
        }

    override var bibleReference: BibleReference?
        get() = prefs[Store.KEY_BIBLE_READER_REFERENCE]?.let { Json.decodeFromString(it) }
        set(value) {
            value?.let { prefs[Store.KEY_BIBLE_READER_REFERENCE] = Json.encodeToString(it) }
                ?: prefs.remove(Store.KEY_BIBLE_READER_REFERENCE)
        }
    override var myVersionIds: Set<Int>?
        get() = prefs[Store.KEY_BIBLE_READER_MY_VERSIONS]?.split(",")?.map { it.toInt() }?.toSet()
        set(value) {
            value?.let { prefs[Store.KEY_BIBLE_READER_MY_VERSIONS] = it.joinToString(",") { i -> i.toString() } }
                ?: prefs.remove(Store.KEY_BIBLE_READER_MY_VERSIONS)
        }
}
