package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.repository.AuthActionResult
import com.example.studentcopilot.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSigningOut: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isGuestMode: Boolean = false,
    val currentUserId: String? = null,
    val currentUserEmail: String? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val syncErrorMessage: String? = null,
) {
    val canEnterApp: Boolean
        get() = isAuthenticated || isGuestMode
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudentCopilotApp
    private val authRepository: AuthRepository = app.authRepository
    private val syncRepository = app.supabaseSyncRepository
    private val _state = MutableStateFlow(
        AuthUiState(
            isConfigured = authRepository.isConfigured(),
        ),
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        refreshSession()
    }

    fun refreshSession() {
        viewModelScope.launch {
            val session = authRepository.restoreSession()
            if (session != null) {
                if (authRepository.isGuestSession(session)) {
                    _state.value = AuthUiState(
                        isConfigured = authRepository.isConfigured(),
                        isLoading = false,
                        isGuestMode = true,
                        currentUserId = session.userId,
                        infoMessage = "Guest mode keeps your data on this device only.",
                    )
                    app.reminderScheduler.rescheduleAll(session.userId)
                    return@launch
                }

                _state.value = AuthUiState(
                    isConfigured = true,
                    isLoading = false,
                    isSyncing = true,
                    isAuthenticated = true,
                    currentUserId = session.userId,
                    currentUserEmail = session.email,
                )
                finishSync()
                return@launch
            }

            if (!authRepository.isConfigured()) {
                _state.value = AuthUiState(
                    isConfigured = false,
                    isLoading = false,
                    errorMessage = "Supabase auth is not configured on this build.",
                )
                return@launch
            }

            _state.value = AuthUiState(
                isConfigured = true,
                isLoading = false,
                isGuestMode = false,
            )
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(errorMessage = "Enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    infoMessage = null,
                )
            }

            when (val result = authRepository.signIn(email.trim(), password)) {
                is AuthActionResult.Authenticated -> {
                    _state.value = AuthUiState(
                        isConfigured = true,
                        isLoading = false,
                        isSyncing = true,
                        isAuthenticated = true,
                        currentUserId = result.session.userId,
                        currentUserEmail = result.session.email,
                    )
                    finishSync()
                }
                is AuthActionResult.Failure -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message,
                        )
                    }
                }
                is AuthActionResult.RequiresEmailConfirmation -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            infoMessage = "Check your email to finish signing in.",
                        )
                    }
                }
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(errorMessage = "Enter both email and password.") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    infoMessage = null,
                )
            }

            when (val result = authRepository.signUp(email.trim(), password)) {
                is AuthActionResult.Authenticated -> {
                    _state.value = AuthUiState(
                        isConfigured = true,
                        isLoading = false,
                        isSyncing = true,
                        isAuthenticated = true,
                        currentUserId = result.session.userId,
                        currentUserEmail = result.session.email,
                    )
                    finishSync()
                }
                is AuthActionResult.RequiresEmailConfirmation -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            infoMessage = "Account created. If email confirmation is enabled in Supabase, verify $email before signing in.",
                        )
                    }
                }
                is AuthActionResult.Failure -> {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    infoMessage = null,
                )
            }

            val guestSession = authRepository.continueAsGuest()
            app.reminderScheduler.rescheduleAll(guestSession.userId)
            _state.value = AuthUiState(
                isConfigured = authRepository.isConfigured(),
                isLoading = false,
                isGuestMode = true,
                currentUserId = guestSession.userId,
                infoMessage = "Guest mode keeps your data on this device only.",
            )
        }
    }

    fun retrySync() {
        if (!_state.value.isAuthenticated || _state.value.isGuestMode || _state.value.isSyncing) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSyncing = true,
                    syncErrorMessage = null,
                )
            }
            finishSync()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isSigningOut = true, errorMessage = null, infoMessage = null) }
            val ownerUserId = _state.value.currentUserId
            authRepository.signOut()
            ownerUserId?.let { app.reminderScheduler.cancelAllForOwner(it) }
            _state.value = AuthUiState(
                isConfigured = authRepository.isConfigured(),
                isLoading = false,
            )
        }
    }

    fun clearMessages() {
        _state.update { it.copy(infoMessage = null, errorMessage = null, syncErrorMessage = null) }
    }

    private suspend fun finishSync() {
        when (val syncResult = syncRepository.syncCurrentUser()) {
            is com.example.studentcopilot.data.repository.SyncResult.Success -> {
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncErrorMessage = null,
                    )
                }
            }
            is com.example.studentcopilot.data.repository.SyncResult.Failure -> {
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncErrorMessage = syncResult.message,
                    )
                }
            }
        }
    }
}
