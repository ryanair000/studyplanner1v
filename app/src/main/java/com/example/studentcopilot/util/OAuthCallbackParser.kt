package com.example.studentcopilot.util

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class OAuthCallbackPayload(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long?,
    val errorMessage: String?,
)

object OAuthCallbackParser {
    fun parse(callbackUrl: String): OAuthCallbackPayload {
        val query = callbackUrl.substringAfter('?', "").substringBefore('#')
        val fragment = callbackUrl.substringAfter('#', "")
        val params = buildMap {
            putAll(parseParams(query))
            putAll(parseParams(fragment))
        }

        val expiresAt = params["expires_at"]?.toLongOrNull()
            ?: params["expires_in"]?.toLongOrNull()?.let { (System.currentTimeMillis() / 1000L) + it }

        return OAuthCallbackPayload(
            accessToken = params["access_token"],
            refreshToken = params["refresh_token"],
            expiresAtEpochSeconds = expiresAt,
            errorMessage = firstNonBlank(
                params["error_description"],
                params["error"],
                params["message"],
                params["msg"],
            ),
        )
    }

    private fun parseParams(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&')
            .mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val key = decode(part.substringBefore('=', ""))
                if (key.isBlank()) return@mapNotNull null
                val value = decode(part.substringAfter('=', ""))
                key to value
            }
            .toMap()
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }
    }
}
