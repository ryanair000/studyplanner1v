package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CourseRepository
    private val ownerUserId: String
    private val _errorMessage = MutableStateFlow<String?>(null)

    val courses: StateFlow<List<CourseEntity>>
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val app = application as StudentCopilotApp
        val dao = app.database.courseDao()
        repository = CourseRepository(dao, app.supabaseSyncRepository)
        ownerUserId = app.authRepository.currentUserIdOrNull().orEmpty()
        courses = repository.getAllCourses(ownerUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addCourse(name: String) {
        addCourse(name, null, null)
    }

    fun addCourse(name: String, classDayOfWeek: Int?, classStartMinuteOfDay: Int?) {
        if (name.isBlank() || ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                repository.addCourse(ownerUserId, name.trim(), classDayOfWeek, classStartMinuteOfDay)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't save that course."
            }
        }
    }

    fun updateCourse(id: Long, name: String, classDayOfWeek: Int?, classStartMinuteOfDay: Int?) {
        if (name.isBlank() || ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                repository.updateCourse(ownerUserId, id, name.trim(), classDayOfWeek, classStartMinuteOfDay)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't update that course."
            }
        }
    }

    fun deleteCourse(id: Long) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                repository.deleteCourse(ownerUserId, id)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't delete that course."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
