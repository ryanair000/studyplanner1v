package com.example.studentcopilot.data.repository

import com.example.studentcopilot.data.local.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

class AssignmentRepository(
    private val assignmentDao: com.example.studentcopilot.data.local.dao.AssignmentDao,
    private val syncRepository: SupabaseSyncRepository,
) {

    fun getAllAssignments(ownerUserId: String): Flow<List<AssignmentEntity>> = assignmentDao.getAll(ownerUserId)

    suspend fun addAssignment(ownerUserId: String, title: String, courseId: Long, dueDate: Long) {
        syncRepository.addAssignment(ownerUserId, title, courseId, dueDate)
    }

    suspend fun toggleCompleted(ownerUserId: String, id: Long, completed: Boolean) {
        syncRepository.toggleAssignmentCompleted(ownerUserId, id, completed)
    }

    suspend fun deleteAssignment(ownerUserId: String, id: Long) {
        syncRepository.deleteAssignment(ownerUserId, id)
    }
}
