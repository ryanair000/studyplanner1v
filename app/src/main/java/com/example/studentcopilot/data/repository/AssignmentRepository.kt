package com.example.studentcopilot.data.repository

import com.example.studentcopilot.data.local.dao.AssignmentDao
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

class AssignmentRepository(private val assignmentDao: AssignmentDao) {

    fun getAllAssignments(): Flow<List<AssignmentEntity>> = assignmentDao.getAll()

    suspend fun addAssignment(title: String, courseId: Long, dueDate: Long) {
        assignmentDao.insert(
            AssignmentEntity(title = title, courseId = courseId, dueDate = dueDate),
        )
    }

    suspend fun toggleCompleted(id: Long, completed: Boolean) {
        assignmentDao.setCompleted(id, completed)
    }

    suspend fun deleteAssignment(id: Long) {
        assignmentDao.deleteById(id)
    }
}
