package com.example.studentcopilot.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.viewmodel.AuthUiState

@Composable
fun AuthScreen(
    state: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit,
    onContinueAsGuest: () -> Unit,
    onClearMessages: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isSignUpMode by rememberSaveable { mutableStateOf(false) }
    var localMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Pangia",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = if (isSignUpMode) {
                        "Create an account to keep your data tied to you on this device."
                    } else {
                        "Sign in to continue to your courses, assignments, and exams."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!state.isConfigured) {
                    Text(
                        text = state.errorMessage ?: "Supabase auth is not configured.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localMessage = null
                        onClearMessages()
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localMessage = null
                        onClearMessages()
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isSignUpMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localMessage = null
                            onClearMessages()
                        },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val message = localMessage ?: state.errorMessage ?: state.infoMessage
                if (message != null) {
                    Text(
                        text = message,
                        color = if (state.errorMessage != null || localMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }

                Button(
                    onClick = {
                        localMessage = null
                        onClearMessages()
                        if (isSignUpMode && password != confirmPassword) {
                            localMessage = "Passwords do not match."
                        } else if (isSignUpMode) {
                            onSignUp(email, password)
                        } else {
                            onSignIn(email, password)
                        }
                    },
                    enabled = state.isConfigured && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isSubmitting) {
                            if (isSignUpMode) "Creating account..." else "Signing in..."
                        } else if (isSignUpMode) {
                            "Create account"
                        } else {
                            "Sign in"
                        },
                    )
                }

                TextButton(
                    onClick = {
                        localMessage = null
                        onClearMessages()
                        onContinueAsGuest()
                    },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Continue as guest")
                }

                TextButton(
                    onClick = {
                        isSignUpMode = !isSignUpMode
                        localMessage = null
                        onClearMessages()
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        if (isSignUpMode) {
                            "Already have an account? Sign in"
                        } else {
                            "Need an account? Sign up"
                        },
                    )
                }
            }
        }
    }
}
