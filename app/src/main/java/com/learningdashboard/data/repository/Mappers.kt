package com.learningdashboard.data.repository

import com.learningdashboard.data.local.CourseEntity
import com.learningdashboard.data.local.CourseWithStats
import com.learningdashboard.data.local.LessonEntity
import com.learningdashboard.data.remote.dto.CourseDto
import com.learningdashboard.data.remote.dto.LessonDto
import com.learningdashboard.domain.ProgressCalculator
import com.learningdashboard.domain.model.Course
import com.learningdashboard.domain.model.CourseDetail
import com.learningdashboard.domain.model.Lesson

internal fun CourseDto.toEntity(userId: String) = CourseEntity(
    userId = userId,
    id = id,
    title = title,
    instructor = instructor,
    serverProgress = progress,
    lessonCount = lessons,
)

internal fun LessonDto.toEntity(userId: String, courseId: Int, position: Int) = LessonEntity(
    userId = userId,
    id = id,
    courseId = courseId,
    position = position,
    title = title,
    isCompleted = completed,
)

// Once lessons are cached, progress is derived from them so local completions show up immediately.
internal fun CourseWithStats.toDomain() = Course(
    id = course.id,
    title = course.title,
    instructor = course.instructor,
    progress = if (cachedLessonCount > 0) {
        ProgressCalculator.percent(completedLessonCount, cachedLessonCount)
    } else {
        course.serverProgress
    },
    lessonCount = if (cachedLessonCount > 0) cachedLessonCount else course.lessonCount,
)

internal fun CourseEntity.toDetail(lessons: List<LessonEntity>): CourseDetail {
    val stats = CourseWithStats(
        course = this,
        cachedLessonCount = lessons.size,
        completedLessonCount = lessons.count { it.isCompleted },
    )
    return CourseDetail(
        course = stats.toDomain(),
        lessons = lessons.sortedBy { it.position }
            .map { Lesson(it.id, it.title, it.isCompleted, it.isPendingSync) },
    )
}
