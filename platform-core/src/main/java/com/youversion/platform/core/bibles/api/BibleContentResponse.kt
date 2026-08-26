package com.youversion.platform.core.bibles.api

import com.youversion.platform.core.api.parseApiBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

/** A parsed API payload together with the caching policy from the response's HTTP headers. */
internal data class BibleContentResponse<T>(
    val value: T,
    val expiresAt: Long,
    val isCacheable: Boolean,
)

internal const val DEFAULT_CACHE_DURATION_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

internal suspend inline fun <reified T> parseBibleContentResponse(
    response: HttpResponse,
    now: Long = System.currentTimeMillis(),
): BibleContentResponse<T> =
    BibleContentResponse(
        value = parseApiBody(response),
        expiresAt = response.cacheExpiresAt(now),
        isCacheable = response.allowsCaching,
    )

internal val HttpResponse.allowsCaching: Boolean
    get() =
        cacheControlDirectives.none {
            val name = it.substringBefore('=').lowercase()
            name == "no-cache" || name == "no-store"
        }

internal fun HttpResponse.cacheExpiresAt(now: Long): Long {
    val maxAgeSeconds =
        cacheControlDirectives
            .firstOrNull { it.lowercase().startsWith("max-age=") }
            ?.substringAfter('=')
            ?.trim('"')
            ?.toDoubleOrNull()
            ?.takeIf { it >= 0 }
            ?: (DEFAULT_CACHE_DURATION_MILLIS / 1000.0)
    val ageSeconds = headers[HttpHeaders.Age]?.toDoubleOrNull() ?: 0.0
    val remainingSeconds = maxOf(0.0, maxAgeSeconds - maxOf(0.0, ageSeconds))
    return now + (remainingSeconds * 1000).toLong()
}

private val HttpResponse.cacheControlDirectives: List<String>
    get() = headers[HttpHeaders.CacheControl]?.split(',')?.map { it.trim() } ?: emptyList()
