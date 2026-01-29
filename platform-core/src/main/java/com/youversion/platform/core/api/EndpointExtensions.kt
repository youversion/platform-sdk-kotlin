package com.youversion.platform.core.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

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

internal fun URLBuilder.pageToken(pageToken: String?): Unit = parameter("page_token", pageToken)
