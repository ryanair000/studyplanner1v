package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.repository.AssignmentRepository
import com.example.studentcopilot.data.repository.CourseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssignmentViewModel(application: Application) : AndroidViewModel(application) {

    private val assignmentRepository: AssignmentRepository
    private val courseRepository: CourseRepository

    val assignments: StateFlow<List<AssignmentEntity>>
    val courses: StateFlow<List<CourseEntity>>

    init {
        val db = (application as StudentCopilotApp).database
        assignmentRepository = AssignmentRepository(db.assignmentDao())
        courseRepository = CourseRepository(db.courseDao())

        assignments = assignmentRepository.getAllAssignments()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        courses = courseRepository.getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addAssignment(title: String, courseId: Long, dueDate: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            assignmentRepository.addAssignment(title.trim(), courseId, dueDate)
        }
    }

    fun toggleCompleted(id: Long, currentValue: Boolean) {
        viewModelScope.launch {
            assignmentRepository.toggleCompleted(id, !currentValue)
        }
    }

    fun deleteAssignment(id: Long) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(id)
        }
    }
}
