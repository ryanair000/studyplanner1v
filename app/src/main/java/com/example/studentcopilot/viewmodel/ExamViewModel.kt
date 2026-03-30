package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.data.repository.CourseRepository
import com.example.studentcopilot.data.repository.ExamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val examRepository: ExamRepository
    private val courseRepository: CourseRepository
    private val ownerUserId: String
    private val _errorMessage = MutableStateFlow<String?>(null)

    val exams: StateFlow<List<ExamEntity>>
    val courses: StateFlow<List<CourseEntity>>
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val app = application as StudentCopilotApp
        val db = app.database
        examRepository = ExamRepository(db.examDao(), app.supabaseSyncRepository)
        courseRepository = CourseRepository(db.courseDao(), app.supabaseSyncRepository)
        ownerUserId = app.authRepository.currentUserIdOrNull().orEmpty()

        exams = examRepository.getAllExams(ownerUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        courses = courseRepository.getAllCourses(ownerUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addExam(title: String, courseId: Long, date: Long, type: String) {
        if (title.isBlank() || courseId <= 0L || date <= 0L || type.isBlank() || ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                examRepository.addExam(ownerUserId, title.trim(), courseId, date, type)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't save that exam."
            }
        }
    }

    fun deleteExam(id: Long) {
        if (ownerUserId.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            runCatching {
                examRepository.deleteExam(ownerUserId, id)
            }.onFailure {
                _errorMessage.value = it.message ?: "Couldn't delete that exam."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
