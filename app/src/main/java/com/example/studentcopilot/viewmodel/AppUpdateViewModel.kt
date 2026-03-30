package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.update.AppUpdateChecker
import com.example.studentcopilot.update.AppUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUpdateUiState(
    val availableUpdate: AppUpdateInfo? = null,
)

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val appUpdateChecker = AppUpdateChecker()
    private val _state = MutableStateFlow(AppUpdateUiState())
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    private var hasCheckedThisSession = false

    fun checkForUpdates() {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true

        viewModelScope.launch {
            val update = appUpdateChecker.checkForUpdate()
            _state.update { it.copy(availableUpdate = update) }
        }
    }

    fun dismissUpdate() {
        _state.update { it.copy(availableUpdate = null) }
    }
}
