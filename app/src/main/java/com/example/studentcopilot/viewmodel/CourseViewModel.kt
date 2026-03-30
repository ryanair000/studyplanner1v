package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.repository.CourseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CourseRepository

    val courses: StateFlow<List<CourseEntity>>

    init {
        val dao = (application as StudentCopilotApp).database.courseDao()
        repository = CourseRepository(dao)
        courses = repository.getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addCourse(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCourse(name.trim())
        }
    }

    fun deleteCourse(id: Long) {
        viewModelScope.launch {
            repository.deleteCourse(id)
        }
    }
}
