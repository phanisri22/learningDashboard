package com.learningdashboard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CourseDao {

    @Query(
        """
        SELECT c.*,
            (SELECT COUNT(*) FROM lessons l WHERE l.userId = c.userId AND l.courseId = c.id) AS cachedLessonCount,
            (SELECT COUNT(*) FROM lessons l WHERE l.userId = c.userId AND l.courseId = c.id AND l.isCompleted = 1) AS completedLessonCount
        FROM courses c
        WHERE c.userId = :userId
        ORDER BY c.id
        """,
    )
    abstract fun observeCoursesWithStats(userId: String): Flow<List<CourseWithStats>>

    @Query("SELECT * FROM courses WHERE userId = :userId AND id = :courseId")
    abstract fun observeCourse(userId: String, courseId: Int): Flow<CourseEntity?>

    @Query("SELECT * FROM lessons WHERE userId = :userId AND courseId = :courseId ORDER BY position")
    abstract fun observeLessons(userId: String, courseId: Int): Flow<List<LessonEntity>>

    @Upsert
    protected abstract suspend fun upsertCourses(courses: List<CourseEntity>)

    @Query("DELETE FROM courses WHERE userId = :userId AND id NOT IN (:ids)")
    protected abstract suspend fun deleteCoursesNotIn(userId: String, ids: List<Int>)

    /** Makes the user's cached course list match the server; lessons of removed courses cascade. */
    @Transaction
    open suspend fun replaceCourses(userId: String, courses: List<CourseEntity>) {
        deleteCoursesNotIn(userId, courses.map { it.id })
        upsertCourses(courses)
    }

    @Query("SELECT * FROM lessons WHERE userId = :userId AND courseId = :courseId")
    protected abstract suspend fun getLessons(userId: String, courseId: Int): List<LessonEntity>

    @Upsert
    protected abstract suspend fun upsertLessons(lessons: List<LessonEntity>)

    /** Writes server lessons without losing completions made on this device. */
    @Transaction
    open suspend fun mergeLessons(userId: String, courseId: Int, remote: List<LessonEntity>) {
        val local = getLessons(userId, courseId).associateBy { it.id }
        upsertLessons(LessonMerger.merge(remote, local))
    }

    /** Returns the number of rows changed: 0 if the lesson was already completed. */
    @Query(
        "UPDATE lessons SET isCompleted = 1, isPendingSync = 1 " +
            "WHERE userId = :userId AND id = :lessonId AND isCompleted = 0",
    )
    abstract suspend fun markLessonCompleted(userId: String, lessonId: Int): Int

    @Query("UPDATE lessons SET isPendingSync = 0 WHERE userId = :userId AND id = :lessonId")
    abstract suspend fun markLessonSynced(userId: String, lessonId: Int)

    @Query("SELECT * FROM lessons WHERE userId = :userId AND isPendingSync = 1")
    abstract suspend fun getPendingLessons(userId: String): List<LessonEntity>
}
