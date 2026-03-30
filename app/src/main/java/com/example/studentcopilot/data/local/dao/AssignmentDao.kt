package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {

    @Insert
    suspend fun insert(assignment: AssignmentEntity)

    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAll(): Flow<List<AssignmentEntity>>

    @Query("UPDATE assignments SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM assignments WHERE isCompleted = 0 AND dueDate >= :now")
    fun countUpcoming(now: Long): Flow<Int>

    @Query("SELECT MIN(dueDate) FROM assignments WHERE isCompleted = 0 AND dueDate >= :now")
    fun nearestDeadline(now: Long): Flow<Long?>
}
