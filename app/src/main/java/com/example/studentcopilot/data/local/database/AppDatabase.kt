package com.example.studentcopilot.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studentcopilot.data.local.dao.AssignmentDao
import com.example.studentcopilot.data.local.dao.CourseDao
import com.example.studentcopilot.data.local.dao.ExamDao
import com.example.studentcopilot.data.local.dao.TimetableEntryDao
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.data.local.entity.TimetableEntryEntity

@Database(
    entities = [CourseEntity::class, AssignmentEntity::class, ExamEntity::class, TimetableEntryEntity::class],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun examDao(): ExamDao
    abstract fun timetableEntryDao(): TimetableEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_copilot.db",
                )
                    .addMigrations(
                        MIGRATION_1_4,
                        MIGRATION_2_4,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
