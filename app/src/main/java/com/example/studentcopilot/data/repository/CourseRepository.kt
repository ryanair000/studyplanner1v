package com.example.studentcopilot.data.repository

import com.example.studentcopilot.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

class CourseRepository(
    private val courseDao: com.example.studentcopilot.data.local.dao.CourseDao,
    private val syncRepository: SupabaseSyncRepository,
) {

    fun getAllCourses(ownerUserId: String): Flow<List<CourseEntity>> = courseDao.getAll(ownerUserId)

    suspend fun addCourse(
        ownerUserId: String,
        name: String,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
    ) {
        syncRepository.addCourse(ownerUserId, name, classDayOfWeek, classStartMinuteOfDay)
    }

    suspend fun updateCourse(
        ownerUserId: String,
        id: Long,
        name: String,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
    ) {
        syncRepository.updateCourse(ownerUserId, id, name, classDayOfWeek, classStartMinuteOfDay)
    }

    suspend fun deleteCourse(ownerUserId: String, id: Long) {
        syncRepository.deleteCourse(ownerUserId, id)
    }
}
