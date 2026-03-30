package com.example.studentcopilot.update

import com.example.studentcopilot.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AppUpdateChecker {
    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val configUrl = BuildConfig.UPDATE_CONFIG_URL
        if (configUrl.isBlank()) return@withContext null

        runCatching {
            val connection = (URL(configUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                requestMethod = "GET"
            }

            try {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseUpdateInfo(
                    configJson = body,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    fallbackDownloadUrl = BuildConfig.UPDATE_FALLBACK_DOWNLOAD_URL,
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    internal fun parseUpdateInfo(
        configJson: String,
        currentVersionCode: Int,
        fallbackDownloadUrl: String,
    ): AppUpdateInfo? {
        val json = Json.parseToJsonElement(configJson).jsonObject
        val remoteVersionCode = json["versionCode"]?.jsonPrimitive?.intOrNull ?: -1
        val remoteVersionName = json["versionName"]?.jsonPrimitive?.content.orEmpty()
            .ifBlank { remoteVersionCode.toString() }
        val remoteDownloadUrl = json["downloadUrl"]?.jsonPrimitive?.content.orEmpty()
            .ifBlank { fallbackDownloadUrl }
        val releaseNotes = json["releaseNotes"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        if (remoteVersionCode <= currentVersionCode || remoteDownloadUrl.isBlank()) {
            return null
        }

        return AppUpdateInfo(
            versionCode = remoteVersionCode,
            versionName = remoteVersionName,
            downloadUrl = remoteDownloadUrl,
            releaseNotes = releaseNotes,
        )
    }
}
