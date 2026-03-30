package com.example.studentcopilot.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthCallbackParserTest {

    @Test
    fun parsesSessionFromFragment() {
        val payload = OAuthCallbackParser.parse(
            "com.example.studentcopilot://auth/callback#access_token=abc123&refresh_token=refresh456&expires_at=2000000000",
        )

        assertEquals("abc123", payload.accessToken)
        assertEquals("refresh456", payload.refreshToken)
        assertEquals(2_000_000_000L, payload.expiresAtEpochSeconds)
        assertNull(payload.errorMessage)
    }

    @Test
    fun parsesErrorFromQueryParameters() {
        val payload = OAuthCallbackParser.parse(
            "com.example.studentcopilot://auth/callback?error_description=Access%20Denied",
        )

        assertNull(payload.accessToken)
        assertEquals("Access Denied", payload.errorMessage)
    }

    @Test
    fun computesExpiryFromExpiresInWhenNeeded() {
        val beforeParse = System.currentTimeMillis() / 1000L
        val payload = OAuthCallbackParser.parse(
            "com.example.studentcopilot://auth/callback#access_token=abc123&expires_in=60",
        )
        val afterParse = System.currentTimeMillis() / 1000L

        val expiresAt = payload.expiresAtEpochSeconds
        assertTrue(expiresAt != null)
        assertTrue(expiresAt!! in (beforeParse + 60)..(afterParse + 60))
    }
}
