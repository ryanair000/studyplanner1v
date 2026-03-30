package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.studentcopilot.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Query("SELECT * FROM courses WHERE ownerUserId = :ownerUserId ORDER BY name ASC")
    suspend fun getAllSnapshot(ownerUserId: String): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE ownerUserId = :ownerUserId ORDER BY name ASC")
    fun getAll(ownerUserId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE ownerUserId = :ownerUserId AND id = :id LIMIT 1")
    suspend fun getById(ownerUserId: String, id: Long): CourseEntity?

    @Update
    suspend fun update(course: CourseEntity)

    @Query("UPDATE courses SET remoteId = :remoteId WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun updateRemoteId(ownerUserId: String, id: Long, remoteId: String)

    @Query("DELETE FROM courses WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun deleteById(ownerUserId: String, id: Long)

    @Query("DELETE FROM courses WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: String)

    @Query("SELECT COUNT(*) FROM courses WHERE ownerUserId = :ownerUserId")
    suspend fun countByOwner(ownerUserId: String): Int

    @Query("UPDATE courses SET ownerUserId = :ownerUserId WHERE ownerUserId = ''")
    suspend fun claimLegacyRows(ownerUserId: String)

    @Query("UPDATE courses SET ownerUserId = :newOwnerUserId WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(oldOwnerUserId: String, newOwnerUserId: String)
}
