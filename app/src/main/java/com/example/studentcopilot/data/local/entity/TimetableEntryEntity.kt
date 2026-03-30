package com.example.studentcopilot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("courseId"),
        Index("ownerUserId"),
        Index(value = ["ownerUserId", "remoteId"], unique = true),
    ],
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val ownerUserId: String,
    val courseId: Long,
    val dayOfWeek: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int? = null,
    val classType: String? = null,
    val section: String? = null,
    val venue: String? = null,
    val lecturer: String? = null,
    val weekPattern: String? = null,
    val source: String = SOURCE_MANUAL,
) {
    companion object {
        const val SOURCE_COURSE_SCHEDULE = "course_schedule"
        const val SOURCE_MANUAL = "manual"
    }
}
