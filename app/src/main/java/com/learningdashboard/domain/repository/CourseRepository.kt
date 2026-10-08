package com.learningdashboard.domain.repository

import com.learningdashboard.domain.model.Course
import com.learningdashboard.domain.model.CourseDetail
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: observe* functions read from the local cache, refresh* functions
 * fetch from the API and write into it. Everything is scoped to the logged-in user.
 */
interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>

    fun observeCourseDetail(courseId: Int): Flow<CourseDetail?>

    /** Returns the number of courses fetched. */
    suspend fun refreshCourses(): Result<Int>

    /** Returns the number of lessons fetched. */
    suspend fun refreshCourseDetail(courseId: Int): Result<Int>

    /** Saved locally first; synced to the server now or on the next refresh. */
    suspend fun markLessonCompleted(courseId: Int, lessonId: Int)

    /** Pushes completions saved offline. Returns how many are still unsynced. */
    suspend fun syncPendingCompletions(): Int
}
