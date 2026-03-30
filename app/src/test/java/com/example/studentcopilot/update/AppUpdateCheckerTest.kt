package com.example.studentcopilot.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateCheckerTest {

    private val checker = AppUpdateChecker()

    @Test
    fun parseUpdateInfo_returnsUpdateWhenRemoteVersionIsHigher() {
        val result = checker.parseUpdateInfo(
            configJson = """
                {
                  "versionCode": 2,
                  "versionName": "1.0.1",
                  "downloadUrl": "https://example.com/Pangia-v1.0.1.apk",
                  "releaseNotes": "Bug fixes"
                }
            """.trimIndent(),
            currentVersionCode = 1,
            fallbackDownloadUrl = "",
        )

        requireNotNull(result)
        assertEquals(2, result.versionCode)
        assertEquals("1.0.1", result.versionName)
        assertEquals("https://example.com/Pangia-v1.0.1.apk", result.downloadUrl)
        assertEquals("Bug fixes", result.releaseNotes)
    }

    @Test
    fun parseUpdateInfo_returnsNullWhenRemoteVersionIsNotHigher() {
        val result = checker.parseUpdateInfo(
            configJson = """
                {
                  "versionCode": 1,
                  "versionName": "1.0.0",
                  "downloadUrl": "https://example.com/Pangia-v1.0.0.apk"
                }
            """.trimIndent(),
            currentVersionCode = 1,
            fallbackDownloadUrl = "",
        )

        assertNull(result)
    }

    @Test
    fun parseUpdateInfo_usesFallbackDownloadUrl() {
        val result = checker.parseUpdateInfo(
            configJson = """
                {
                  "versionCode": 2,
                  "versionName": "1.0.1"
                }
            """.trimIndent(),
            currentVersionCode = 1,
            fallbackDownloadUrl = "https://example.com/Pangia-v1.0.1.apk",
        )

        requireNotNull(result)
        assertEquals("https://example.com/Pangia-v1.0.1.apk", result.downloadUrl)
    }
}
