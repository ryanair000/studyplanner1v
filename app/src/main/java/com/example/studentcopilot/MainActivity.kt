package com.example.studentcopilot

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.studentcopilot.ui.navigation.AppNavigation
import com.example.studentcopilot.ui.theme.StudentCopilotTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private val authCallbackUri = MutableStateFlow<Uri?>(null)
    val authCallbackUris: StateFlow<Uri?> = authCallbackUri.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishAuthCallback(intent)
        enableEdgeToEdge()
        setContent {
            StudentCopilotTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publishAuthCallback(intent)
    }

    fun clearAuthCallback() {
        authCallbackUri.value = null
        setIntent(intent?.let { Intent(it).apply { data = null } })
    }

    private fun publishAuthCallback(sourceIntent: Intent?) {
        val callbackUri = sourceIntent?.data ?: return
        if (
            callbackUri.scheme == BuildConfig.AUTH_REDIRECT_SCHEME &&
            callbackUri.host == BuildConfig.AUTH_REDIRECT_HOST &&
            callbackUri.path == BuildConfig.AUTH_REDIRECT_PATH
        ) {
            authCallbackUri.value = callbackUri
        }
    }
}
