package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {

    @Insert
    suspend fun insert(assignment: AssignmentEntity): Long

    @Query("SELECT * FROM assignments WHERE ownerUserId = :ownerUserId ORDER BY dueDate ASC")
    suspend fun getAllSnapshot(ownerUserId: String): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE ownerUserId = :ownerUserId ORDER BY dueDate ASC")
    fun getAll(ownerUserId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE ownerUserId = :ownerUserId AND id = :id LIMIT 1")
    suspend fun getById(ownerUserId: String, id: Long): AssignmentEntity?

    @Query("UPDATE assignments SET remoteId = :remoteId WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun updateRemoteId(ownerUserId: String, id: Long, remoteId: String)

    @Query("UPDATE assignments SET isCompleted = :completed WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun setCompleted(ownerUserId: String, id: Long, completed: Boolean)

    @Query("DELETE FROM assignments WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun deleteById(ownerUserId: String, id: Long)

    @Query("DELETE FROM assignments WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: String)

    @Query("SELECT COUNT(*) FROM assignments WHERE ownerUserId = :ownerUserId AND isCompleted = 0 AND dueDate >= :now")
    fun countUpcoming(ownerUserId: String, now: Long): Flow<Int>

    @Query("SELECT MIN(dueDate) FROM assignments WHERE ownerUserId = :ownerUserId AND isCompleted = 0 AND dueDate >= :now")
    fun nearestDeadline(ownerUserId: String, now: Long): Flow<Long?>

    @Query("UPDATE assignments SET ownerUserId = :ownerUserId WHERE ownerUserId = ''")
    suspend fun claimLegacyRows(ownerUserId: String)

    @Query("UPDATE assignments SET ownerUserId = :newOwnerUserId WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(oldOwnerUserId: String, newOwnerUserId: String)
}
