package com.learningdashboard.domain.model

data class Course(
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessonCount: Int,
)

data class Lesson(
    val id: Int,
    val title: String,
    val isCompleted: Boolean,
    /** Completed locally but not yet confirmed by the server. */
    val isPendingSync: Boolean,
)

data class CourseDetail(
    val course: Course,
    val lessons: List<Lesson>,
) {
    val completedCount: Int get() = lessons.count { it.isCompleted }
}
