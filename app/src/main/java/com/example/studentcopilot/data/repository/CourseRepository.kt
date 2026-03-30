package com.example.studentcopilot.data.repository

import com.example.studentcopilot.data.local.dao.CourseDao
import com.example.studentcopilot.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) {

    fun getAllCourses(): Flow<List<CourseEntity>> = courseDao.getAll()

    suspend fun addCourse(name: String) {
        courseDao.insert(CourseEntity(name = name))
    }

    suspend fun deleteCourse(id: Long) {
        courseDao.deleteById(id)
    }
}
