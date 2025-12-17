package com.youversion.platform.core.api

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

internal suspend inline fun <reified T> parsePaginatedResponse(response: HttpResponse): PaginatedResponse<T> =
    parseApiBody<PaginatedResponse<T>>(response)

internal suspend inline fun <reified T> parseApiResponse(response: HttpResponse): T =
    parseApiBody<ApiResponse<T>>(response).data

internal suspend inline fun <reified T> parseApiBody(response: HttpResponse): T {
    if (response.status == HttpStatusCode.Unauthorized) {
        Logger.w { "error 401: unauthorized. Check your appKey" }
        throw notPermitted()
    }

    if (response.status == HttpStatusCode.Forbidden) {
        Logger.w { "error 403: forbidden. Check your appKey and it's entitlements" }
        throw notPermitted()
    }

    if (!response.status.isSuccess()) {
        Logger.w { "Request failed with status code ${response.status.value} " }
        throw cannotDownload()
    }

    try {
        return response.body<T>()
    } catch (e: Exception) {
        throw invalidResponse(e)
    }
}

/**
 * Fetches all pages from a paginated API endpoint and returns the combined results.
 *
 * **⚠️ USE WITH CAUTION**: This function makes multiple sequential API calls, one for each page
 * of results
 */
internal suspend fun <T> fetchAllPages(request: suspend (nextPageToken: String?) -> PaginatedResponse<T>): List<T> {
    var nextPageToken: String? = null
    var hasNextPage = true
    val allResults = mutableListOf<T>()

    while (hasNextPage) {
        val paginatedResponse = request(nextPageToken)
        allResults += paginatedResponse.data
        nextPageToken = paginatedResponse.nextPageToken
        hasNextPage = paginatedResponse.nextPageToken != null
    }

    return allResults
}
