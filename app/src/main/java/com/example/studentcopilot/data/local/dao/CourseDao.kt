package com.example.studentcopilot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studentcopilot.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Insert
    suspend fun insert(course: CourseEntity)

    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAll(): Flow<List<CourseEntity>>

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
