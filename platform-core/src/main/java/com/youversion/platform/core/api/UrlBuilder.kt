package com.youversion.platform.core.api

import android.net.Uri
import androidx.core.net.toUri
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.users.api.UsersEndpoints
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

internal fun Url.toAndroidUri(): Uri = toString().toUri()

@Deprecated("Explore how to hide Url from public API")
fun authUri(
    appKey: String,
    requiredPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
    optionalPermissions: Set<SignInWithYouVersionPermission> = emptySet(),
): Uri = UsersEndpoints.authUrl(appKey, requiredPermissions, optionalPermissions).toAndroidUri()

internal fun buildYouVersionUrl(block: URLBuilder.() -> Unit): Url =
    HttpRequestBuilder()
        .apply {
            url {
                protocol = URLProtocol.HTTPS
                host = YouVersionPlatformConfiguration.apiHost

                block()
            }
        }.build()
        .url

internal fun URLBuilder.parameter(
    key: String,
    value: Any?,
): Unit = value?.let { parameters.append(key, it.toString()) } ?: Unit
