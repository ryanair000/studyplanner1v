package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studentcopilot.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Insert
    suspend fun insert(exam: ExamEntity)

    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun getAll(): Flow<List<ExamEntity>>

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM exams WHERE date >= :now")
    fun countUpcoming(now: Long): Flow<Int>

    @Query("SELECT MIN(date) FROM exams WHERE date >= :now")
    fun nearestDate(now: Long): Flow<Long?>
}
