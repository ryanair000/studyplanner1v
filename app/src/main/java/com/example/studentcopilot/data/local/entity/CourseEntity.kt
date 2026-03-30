package com.example.studentcopilot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    indices = [
        Index("ownerUserId"),
        Index(value = ["ownerUserId", "remoteId"], unique = true),
    ],
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val ownerUserId: String,
    val name: String,
    val classDayOfWeek: Int? = null,
    val classStartMinuteOfDay: Int? = null,
)
