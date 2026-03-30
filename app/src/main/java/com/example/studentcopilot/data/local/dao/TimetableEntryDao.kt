package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.studentcopilot.data.local.entity.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableEntryDao {

    @Insert
    suspend fun insert(entry: TimetableEntryEntity): Long

    @Update
    suspend fun update(entry: TimetableEntryEntity)

    @Query(
        """
        SELECT * FROM timetable_entries
        WHERE ownerUserId = :ownerUserId
        ORDER BY dayOfWeek ASC, startMinuteOfDay ASC, id ASC
        """,
    )
    suspend fun getAllSnapshot(ownerUserId: String): List<TimetableEntryEntity>

    @Query(
        """
        SELECT * FROM timetable_entries
        WHERE ownerUserId = :ownerUserId
        ORDER BY dayOfWeek ASC, startMinuteOfDay ASC, id ASC
        """,
    )
    fun getAll(ownerUserId: String): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE ownerUserId = :ownerUserId AND id = :id LIMIT 1")
    suspend fun getById(ownerUserId: String, id: Long): TimetableEntryEntity?

    @Query(
        """
        SELECT * FROM timetable_entries
        WHERE ownerUserId = :ownerUserId
            AND courseId = :courseId
            AND source = :source
        ORDER BY id ASC
        LIMIT 1
        """,
    )
    suspend fun getGeneratedForCourse(
        ownerUserId: String,
        courseId: Long,
        source: String = TimetableEntryEntity.SOURCE_COURSE_SCHEDULE,
    ): TimetableEntryEntity?

    @Query("UPDATE timetable_entries SET remoteId = :remoteId WHERE ownerUserId = :ownerUserId AND id = :id")
    suspend fun updateRemoteId(ownerUserId: String, id: Long, remoteId: String)

    @Query("DELETE FROM timetable_entries WHERE id = :id AND ownerUserId = :ownerUserId")
    suspend fun deleteById(ownerUserId: String, id: Long)

    @Query("DELETE FROM timetable_entries WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: String)

    @Query("UPDATE timetable_entries SET ownerUserId = :ownerUserId WHERE ownerUserId = ''")
    suspend fun claimLegacyRows(ownerUserId: String)

    @Query("UPDATE timetable_entries SET ownerUserId = :newOwnerUserId WHERE ownerUserId = :oldOwnerUserId")
    suspend fun reassignOwner(oldOwnerUserId: String, newOwnerUserId: String)
}
