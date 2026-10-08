package com.learningdashboard.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// Every row is owned by a user, so several accounts can share a device without seeing each other's data.

@Entity(tableName = "courses", primaryKeys = ["userId", "id"])
data class CourseEntity(
    val userId: String,
    val id: Int,
    val title: String,
    val instructor: String,
    /** Progress reported by the course list API; used until lessons are cached. */
    val serverProgress: Int,
    val lessonCount: Int,
)

@Entity(
    tableName = "lessons",
    primaryKeys = ["userId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["userId", "id"],
            childColumns = ["userId", "courseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId", "courseId")],
)
data class LessonEntity(
    val userId: String,
    val id: Int,
    val courseId: Int,
    val position: Int,
    val title: String,
    val isCompleted: Boolean,
    val isPendingSync: Boolean = false,
)

data class CourseWithStats(
    @Embedded val course: CourseEntity,
    val cachedLessonCount: Int,
    val completedLessonCount: Int,
)
