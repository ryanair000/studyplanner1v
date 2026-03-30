package com.example.studentcopilot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exams",
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
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val ownerUserId: String,
    val courseId: Long,
    val title: String,
    val date: Long,
    val type: String,
)
