package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studentcopilot.data.local.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Insert
    suspend fun insert(exam: ExamEntity): Long

    @Query("SELECT * FROM exams WHERE ownerUserId = :ownerUserId ORDER BY date ASC")
    suspend fun getAllSnapshot(ownerUserId: String): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE ownerUserId = :ownerUserId ORDER BY date ASC")
    fun getAll(ownerUserId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE ownerUserId = :ownerUserId AND id = :id LIMIT 1")
    suspend fun getById(ownerUserId: String, id: Long): ExamEntity?

    @Query("UPDATE exams SET remoteId = :remoteId WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun updateRemoteId(ownerUserId: String, id: Long, remoteId: String)

    @Query("DELETE FROM exams WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun deleteById(ownerUserId: String, id: Long)

    @Query("DELETE FROM exams WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: String)

    @Query("SELECT COUNT(*) FROM exams WHERE ownerUserId = :ownerUserId AND date >= :now")
    fun countUpcoming(ownerUserId: String, now: Long): Flow<Int>

    @Query("SELECT MIN(date) FROM exams WHERE ownerUserId = :ownerUserId AND date >= :now")
    fun nearestDate(ownerUserId: String, now: Long): Flow<Long?>

    @Query("UPDATE exams SET ownerUserId = :ownerUserId WHERE ownerUserId = ''")
    suspend fun claimLegacyRows(ownerUserId: String)

    @Query("UPDATE exams SET ownerUserId = :newOwnerUserId WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(oldOwnerUserId: String, newOwnerUserId: String)
}
