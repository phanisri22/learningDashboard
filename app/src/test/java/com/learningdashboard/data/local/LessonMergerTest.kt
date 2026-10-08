package com.learningdashboard.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonMergerTest {

    @Test
    fun `local completion survives a stale server response`() {
        val local = mapOf(1 to lesson(id = 1, completed = true, pendingSync = true))
        val remote = listOf(lesson(id = 1, completed = false), lesson(id = 2, completed = false))

        val merged = LessonMerger.merge(remote, local)

        assertEquals(lesson(id = 1, completed = true, pendingSync = true), merged[0])
        assertEquals(lesson(id = 2, completed = false), merged[1])
    }

    @Test
    fun `server confirmation clears the pending flag`() {
        val local = mapOf(1 to lesson(id = 1, completed = true, pendingSync = true))
        val remote = listOf(lesson(id = 1, completed = true))

        val merged = LessonMerger.merge(remote, local)

        assertEquals(lesson(id = 1, completed = true, pendingSync = false), merged.single())
    }

    @Test
    fun `server title changes are applied`() {
        val local = mapOf(1 to lesson(id = 1, completed = false, title = "Old"))
        val remote = listOf(lesson(id = 1, completed = false, title = "New"))

        assertEquals("New", LessonMerger.merge(remote, local).single().title)
    }

    private fun lesson(id: Int, completed: Boolean, pendingSync: Boolean = false, title: String = "Lesson $id") =
        LessonEntity(
            userId = "u_1001",
            id = id,
            courseId = 10,
            position = id,
            title = title,
            isCompleted = completed,
            isPendingSync = pendingSync,
        )
}
