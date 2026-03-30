package com.example.studentcopilot.data.repository

import com.example.studentcopilot.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

class ExamRepository(
    private val examDao: com.example.studentcopilot.data.local.dao.ExamDao,
    private val syncRepository: SupabaseSyncRepository,
) {

    fun getAllExams(ownerUserId: String): Flow<List<ExamEntity>> = examDao.getAll(ownerUserId)

    suspend fun addExam(ownerUserId: String, title: String, courseId: Long, date: Long, type: String) {
        syncRepository.addExam(ownerUserId, title, courseId, date, type)
    }

    suspend fun deleteExam(ownerUserId: String, id: Long) {
        syncRepository.deleteExam(ownerUserId, id)
    }
}
