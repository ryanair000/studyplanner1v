package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.repository.AssignmentRepository
import com.example.studentcopilot.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val assignmentRepository: AssignmentRepository
    private val courseRepository: CourseRepository
    private val ownerUserId: String
    private val _errorMessage = MutableStateFlow<String?>(null)

    val assignments: StateFlow<List<AssignmentEntity>>
    val courses: StateFlow<List<CourseEntity>>
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val app = application as StudentCopilotApp
        val db = app.database
        assignmentRepository = AssignmentRepository(db.assignmentDao(), app.supabaseSyncRepository)
        courseRepository = CourseRepository(db.courseDao(), app.supabaseSyncRepository)
        ownerUserId = app.authRepository.currentUserIdOrNull().orEmpty()

        assignments = assignmentRepository.getAllAssignments(ownerUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        courses = courseRepository.getAllCourses(ownerUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addAssignment(title: String, courseId: Long, dueDate: Long) {
        if (title.isBlank() || ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                assignmentRepository.addAssignment(ownerUserId, title.trim(), courseId, dueDate)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't save that assignment."
            }
        }
    }

    fun toggleCompleted(id: Long, currentValue: Boolean) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                assignmentRepository.toggleCompleted(ownerUserId, id, !currentValue)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't update that assignment."
            }
        }
    }

    fun deleteAssignment(id: Long) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                assignmentRepository.deleteAssignment(ownerUserId, id)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't delete that assignment."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
