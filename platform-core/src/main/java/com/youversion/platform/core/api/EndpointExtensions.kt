package com.youversion.platform.core.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import java.util.Locale

internal fun buildYouVersionUrlString(block: URLBuilder.() -> Unit): String =
    HttpRequestBuilder()
        .apply {
            url {
                protocol = URLProtocol.HTTPS
                host = YouVersionPlatformConfiguration.apiHost

                block()
            }
        }.build()
        .url
        .toString()

internal fun URLBuilder.parameter(
    key: String,
    value: Any?,
): Unit = value?.let { parameters.append(key, it.toString()) } ?: Unit

internal fun URLBuilder.pageSize(
    pageSize: Int?,
    fields: List<String>? = null,
) {
    val pageSizeParam = if (fields?.size in 1..3) "*" else pageSize?.toString()
    parameter("page_size", pageSizeParam)
}

internal fun URLBuilder.fields(fields: List<String>? = null) {
    fields?.forEach { field ->
        parameter("fields[]", field)
    }
}

/** Appends each range as its own `language_ranges[]` parameter, in order. Empty appends nothing. */
internal fun URLBuilder.languageRanges(languageRanges: List<String>) {
    languageRanges.forEach { languageRange -> parameters.append("language_ranges[]", languageRange) }
}

internal fun URLBuilder.pageToken(pageToken: String?): Unit = parameter("page_token", pageToken)

/** Throws [IllegalArgumentException] unless [pageSize] is `null` or between 1 and 99. */
internal fun requireValidPageSize(pageSize: Int?) {
    pageSize?.let { require(it in 1..99) { "pageSize must be between 1 and 99" } }
}

/** An `Accept-Language` value weighting [languageRanges] in order, or `null` when none is usable. */
internal fun acceptLanguage(languageRanges: List<String>): String? =
    languageRanges
        .filter { it.isNotBlank() }
        .take(MAX_RANGES)
        .mapIndexed { index, range ->
            when (index) {
                0 -> range
                else -> "$range;q=0.${MAX_RANGES - index}"
            }
        }.joinToString(", ")
        .takeIf { it.isNotEmpty() }

/**
 * The device's language tag, then its language without a region or script, then `en`, without repeats. An
 * undetermined device locale yields only `en`.
 */
internal fun deviceLanguageRanges(): List<String> {
    val locale = Locale.getDefault()
    return listOf(locale.toLanguageTag(), locale.language, "en")
        .filter { it.isNotBlank() && it != "und" }
        .distinct()
}

/** Ranges past the tenth have no distinct quality value left below them, so they are dropped. */
private const val MAX_RANGES = 10
