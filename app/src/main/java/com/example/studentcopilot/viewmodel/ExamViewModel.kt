package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.data.repository.CourseRepository
import com.example.studentcopilot.data.repository.ExamRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val examRepository: ExamRepository
    private val courseRepository: CourseRepository

    val exams: StateFlow<List<ExamEntity>>
    val courses: StateFlow<List<CourseEntity>>

    init {
        val db = (application as StudentCopilotApp).database
        examRepository = ExamRepository(db.examDao())
        courseRepository = CourseRepository(db.courseDao())

        exams = examRepository.getAllExams()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        courses = courseRepository.getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addExam(title: String, courseId: Long, date: Long, type: String) {
        if (title.isBlank() || courseId <= 0L || date <= 0L || type.isBlank()) return
        viewModelScope.launch {
            examRepository.addExam(title.trim(), courseId, date, type)
        }
    }

    fun deleteExam(id: Long) {
        viewModelScope.launch {
            examRepository.deleteExam(id)
        }
    }
}
