package com.learningdashboard.data.local

/**
 * Lesson completion only moves forward, so a local completion wins over a server
 * copy that has not caught up yet (pending sync, or a stale response).
 */
object LessonMerger {

    fun merge(remote: List<LessonEntity>, local: Map<Int, LessonEntity>): List<LessonEntity> =
        remote.map { remoteLesson ->
            val localLesson = local[remoteLesson.id]
            if (localLesson != null && localLesson.isCompleted && !remoteLesson.isCompleted) {
                remoteLesson.copy(isCompleted = true, isPendingSync = localLesson.isPendingSync)
            } else {
                remoteLesson
            }
        }
}
