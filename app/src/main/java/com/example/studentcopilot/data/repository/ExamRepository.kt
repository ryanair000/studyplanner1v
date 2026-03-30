package com.example.studentcopilot.data.repository

import com.example.studentcopilot.data.local.dao.ExamDao
import com.example.studentcopilot.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

class ExamRepository(private val examDao: ExamDao) {

    fun getAllExams(): Flow<List<ExamEntity>> = examDao.getAll()

    suspend fun addExam(title: String, courseId: Long, date: Long, type: String) {
        examDao.insert(
            ExamEntity(title = title, courseId = courseId, date = date, type = type),
        )
    }

    suspend fun deleteExam(id: Long) {
        examDao.deleteById(id)
    }
}
