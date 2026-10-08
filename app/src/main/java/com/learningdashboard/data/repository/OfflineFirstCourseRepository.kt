package com.learningdashboard.data.repository

import com.learningdashboard.data.auth.SessionStore
import com.learningdashboard.data.local.CourseDao
import com.learningdashboard.data.remote.LearningApi
import com.learningdashboard.data.remote.resultOf
import com.learningdashboard.domain.model.AppError
import com.learningdashboard.domain.model.Course
import com.learningdashboard.domain.model.CourseDetail
import com.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room is the single source of truth. The UI only ever observes the database;
 * network calls just update it. All data is scoped to the logged-in user.
 * Room and Retrofit suspend calls are main-safe.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class OfflineFirstCourseRepository @Inject constructor(
    private val api: LearningApi,
    private val dao: CourseDao,
    private val sessionStore: SessionStore,
) : CourseRepository {

    override fun observeCourses(): Flow<List<Course>> =
        sessionStore.userId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                dao.observeCoursesWithStats(userId).map { courses -> courses.map { it.toDomain() } }
            }
        }

    override fun observeCourseDetail(courseId: Int): Flow<CourseDetail?> =
        sessionStore.userId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                combine(dao.observeCourse(userId, courseId), dao.observeLessons(userId, courseId)) { course, lessons ->
                    course?.toDetail(lessons)
                }
            }
        }

    override suspend fun refreshCourses(): Result<Int> = resultOf {
        val userId = requireUserId()
        pushPendingCompletions(userId)
        val courses = api.getCourses()
        dao.replaceCourses(userId, courses.map { it.toEntity(userId) })
        // Warm the lesson cache so course details also work offline. Best effort.
        coroutineScope {
            courses.map { async { resultOf { fetchLessons(userId, it.id) } } }.awaitAll()
        }
        courses.size
    }

    override suspend fun refreshCourseDetail(courseId: Int): Result<Int> = resultOf {
        fetchLessons(requireUserId(), courseId)
    }

    override suspend fun markLessonCompleted(courseId: Int, lessonId: Int) {
        val userId = sessionStore.userId.value ?: return
        val changed = dao.markLessonCompleted(userId, lessonId) > 0
        if (!changed) return
        // If this fails the lesson stays pending and is retried on the next refresh.
        resultOf { api.markLessonCompleted(courseId, lessonId) }
            .onSuccess { dao.markLessonSynced(userId, lessonId) }
    }

    override suspend fun syncPendingCompletions(): Int {
        val userId = sessionStore.userId.value ?: return 0
        return pushPendingCompletions(userId)
    }

    /** Returns how many completions are still unsynced. */
    private suspend fun pushPendingCompletions(userId: String): Int {
        var unsynced = 0
        dao.getPendingLessons(userId).forEach { lesson ->
            resultOf { api.markLessonCompleted(lesson.courseId, lesson.id) }
                .onSuccess { dao.markLessonSynced(userId, lesson.id) }
                .onFailure { unsynced++ }
        }
        return unsynced
    }

    private suspend fun fetchLessons(userId: String, courseId: Int): Int {
        val detail = api.getCourseDetail(courseId)
        dao.mergeLessons(
            userId = userId,
            courseId = courseId,
            remote = detail.lessons.mapIndexed { index, lesson -> lesson.toEntity(userId, courseId, index) },
        )
        return detail.lessons.size
    }

    private fun requireUserId(): String = sessionStore.userId.value ?: throw AppError.Unauthorized()
}
