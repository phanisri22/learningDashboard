package com.learningdashboard.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun `progress is the rounded share of completed lessons`() {
        assertEquals(65, ProgressCalculator.percent(completed = 13, total = 20))
        assertEquals(38, ProgressCalculator.percent(completed = 6, total = 16)) // 37.5 rounds up
        assertEquals(100, ProgressCalculator.percent(completed = 28, total = 28))
    }

    @Test
    fun `course without lessons has zero progress instead of dividing by zero`() {
        assertEquals(0, ProgressCalculator.percent(completed = 0, total = 0))
    }
}
