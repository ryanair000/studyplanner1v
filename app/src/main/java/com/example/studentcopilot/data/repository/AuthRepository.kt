package com.example.studentcopilot.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.studentcopilot.BuildConfig
import com.example.studentcopilot.data.local.database.AppDatabase
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class AuthSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long?,
)

sealed interface AuthActionResult {
    data class Authenticated(val session: AuthSession) : AuthActionResult
    data class RequiresEmailConfirmation(val email: String) : AuthActionResult
    data class Failure(val message: String) : AuthActionResult
}

class AuthRepository(
    context: Context,
    private val database: AppDatabase,
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()
    }

    fun currentSessionOrNull(): AuthSession? {
        val userId = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        if (sharedPreferences.getBoolean(KEY_IS_GUEST, false) || isGuestUserId(userId)) {
            return AuthSession(
                userId = userId,
                email = null,
                accessToken = "",
                refreshToken = null,
                expiresAtEpochSeconds = null,
            )
        }

        val accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return AuthSession(
            userId = userId,
            email = sharedPreferences.getString(KEY_EMAIL, null),
            accessToken = accessToken,
            refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null),
            expiresAtEpochSeconds = sharedPreferences.takeIf { it.contains(KEY_EXPIRES_AT) }
                ?.getLong(KEY_EXPIRES_AT, 0L)
                ?.takeIf { it > 0L },
        )
    }

    fun currentUserIdOrNull(): String? = currentSessionOrNull()?.userId

    fun currentUserEmailOrNull(): String? = currentSessionOrNull()?.email

    fun isGuestSession(session: AuthSession?): Boolean = isGuestUserId(session?.userId)

    fun isGuestUserId(userId: String?): Boolean = userId == GUEST_USER_ID

    suspend fun continueAsGuest(): AuthSession = withContext(Dispatchers.IO) {
        val guestSession = AuthSession(
            userId = GUEST_USER_ID,
            email = null,
            accessToken = "",
            refreshToken = null,
            expiresAtEpochSeconds = null,
        )
        persistSession(guestSession, isGuest = true)
        claimLocalDataIfNeeded(guestSession.userId)
        guestSession
    }

    suspend fun restoreSession(): AuthSession? = withContext(Dispatchers.IO) {
        val storedSession = currentSessionOrNull() ?: return@withContext null
        if (isGuestSession(storedSession)) return@withContext storedSession
        if (!isConfigured()) return@withContext null
        if (!isSessionExpired(storedSession)) return@withContext storedSession

        val refreshToken = storedSession.refreshToken
        if (refreshToken.isNullOrBlank()) {
            clearSession()
            return@withContext null
        }

        runCatching {
            refreshSession(refreshToken)
        }.fold(
            onSuccess = { refreshedSession ->
                refreshedSession ?: run {
                    clearSession()
                    null
                }
            },
            onFailure = {
                // Keep the stored session on transient network failures so startup degrades gracefully.
                storedSession
            },
        )
    }

    suspend fun signIn(email: String, password: String): AuthActionResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AuthActionResult.Failure("Supabase is not configured.")
        }

        val response = runCatching {
            request(
                endpoint = "auth/v1/token?grant_type=password",
                method = "POST",
                body = buildJsonObject {
                    put("email", email)
                    put("password", password)
                },
            )
        }.getOrElse { throwable ->
            return@withContext AuthActionResult.Failure(throwable.toAuthFailureMessage())
        }

        if (!response.isSuccessful) {
            return@withContext AuthActionResult.Failure(parseErrorMessage(response.body))
        }

        val session = parseSession(response.body)
            ?: return@withContext AuthActionResult.Failure("Supabase returned an invalid sign-in response.")

        persistSession(session, isGuest = false)
        claimLocalDataIfNeeded(session.userId)
        AuthActionResult.Authenticated(session)
    }

    suspend fun signUp(email: String, password: String): AuthActionResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext AuthActionResult.Failure("Supabase is not configured.")
        }

        val response = runCatching {
            request(
                endpoint = "auth/v1/signup",
                method = "POST",
                body = buildJsonObject {
                    put("email", email)
                    put("password", password)
                },
            )
        }.getOrElse { throwable ->
            return@withContext AuthActionResult.Failure(throwable.toAuthFailureMessage())
        }

        if (!response.isSuccessful) {
            return@withContext AuthActionResult.Failure(parseErrorMessage(response.body))
        }

        val session = parseSession(response.body)
        if (session != null) {
            persistSession(session, isGuest = false)
            claimLocalDataIfNeeded(session.userId)
            return@withContext AuthActionResult.Authenticated(session)
        }

        clearSession()
        return@withContext AuthActionResult.RequiresEmailConfirmation(email)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val session = currentSessionOrNull()
        val accessToken = session?.accessToken
        if (!isGuestSession(session) && isConfigured() && !accessToken.isNullOrBlank()) {
            runCatching {
                request(
                    endpoint = "auth/v1/logout",
                    method = "POST",
                    body = null,
                    accessToken = accessToken,
                )
            }
        }
        clearSession()
    }

    private suspend fun refreshSession(refreshToken: String): AuthSession? {
        val response = request(
            endpoint = "auth/v1/token?grant_type=refresh_token",
            method = "POST",
            body = buildJsonObject {
                put("refresh_token", refreshToken)
            },
        )
        if (!response.isSuccessful) return null

        return parseSession(response.body)?.also { persistSession(it, isGuest = false) }
    }

    private suspend fun claimLocalDataIfNeeded(ownerUserId: String) {
        if (ownerUserId.isBlank()) return
        if (database.courseDao().countByOwner(ownerUserId) > 0) return

        if (!isGuestUserId(ownerUserId)) {
            database.courseDao().reassignOwner(GUEST_USER_ID, ownerUserId)
            database.assignmentDao().reassignOwner(GUEST_USER_ID, ownerUserId)
            database.examDao().reassignOwner(GUEST_USER_ID, ownerUserId)
        }

        database.courseDao().claimLegacyRows(ownerUserId)
        database.assignmentDao().claimLegacyRows(ownerUserId)
        database.examDao().claimLegacyRows(ownerUserId)
    }

    private fun persistSession(session: AuthSession, isGuest: Boolean) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putBoolean(KEY_IS_GUEST, isGuest)
            .apply {
                if (isGuest) {
                    remove(KEY_EMAIL)
                    remove(KEY_ACCESS_TOKEN)
                    remove(KEY_REFRESH_TOKEN)
                    remove(KEY_EXPIRES_AT)
                } else {
                    putString(KEY_EMAIL, session.email)
                    putString(KEY_ACCESS_TOKEN, session.accessToken)
                    putString(KEY_REFRESH_TOKEN, session.refreshToken)
                }
                if (!isGuest && session.expiresAtEpochSeconds != null) {
                    putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
                } else {
                    remove(KEY_EXPIRES_AT)
                }
            }
            .apply()
    }

    private fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_IS_GUEST)
            .apply()
    }

    private fun isSessionExpired(session: AuthSession): Boolean {
        val expiresAt = session.expiresAtEpochSeconds ?: return false
        return (System.currentTimeMillis() / 1000L) >= expiresAt
    }

    private suspend fun request(
        endpoint: String,
        method: String,
        body: JsonObject?,
        accessToken: String? = null,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/$endpoint").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 10_000
                doInput = true
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty(
                    "Authorization",
                    "Bearer ${accessToken ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY}",
                )
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }

        try {
            if (body != null) {
                connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            HttpResponse(
                statusCode = code,
                body = stream?.readUtf8().orEmpty(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun InputStream.readUtf8(): String = bufferedReader().use { it.readText() }

    private fun parseSession(body: String): AuthSession? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val accessToken = root["access_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val refreshToken = root["refresh_token"]?.jsonPrimitive?.contentOrNull
        val expiresAt = root["expires_at"]?.jsonPrimitive?.longOrNull
        val userObject = root["user"]?.jsonObject ?: return null
        val userId = userObject["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val email = userObject["email"]?.jsonPrimitive?.contentOrNull

        return AuthSession(
            userId = userId,
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt,
        )
    }

    private fun parseErrorMessage(body: String): String {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        return root?.stringValue("msg")
            ?: root?.stringValue("message")
            ?: root?.stringValue("error_description")
            ?: "Supabase request failed."
    }

    private fun JsonObject.stringValue(key: String): String? {
        return (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun Throwable.toAuthFailureMessage(): String {
        return when (this) {
            is SocketTimeoutException, is UnknownHostException, is ConnectException, is SSLException ->
                "Couldn't reach Supabase. Check your internet connection and try again."
            is MalformedURLException ->
                "Supabase is configured with an invalid URL."
            else -> message ?: "Supabase request failed."
        }
    }

    private data class HttpResponse(
        val statusCode: Int,
        val body: String,
    ) {
        val isSuccessful: Boolean
            get() = statusCode in 200..299
    }

    private companion object {
        const val PREFS_NAME = "auth_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_IS_GUEST = "is_guest"
        const val GUEST_USER_ID = "__pangia_guest__"
    }
}
