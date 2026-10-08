package com.learningdashboard.domain

import kotlin.math.roundToInt

object ProgressCalculator {

    /** Percentage of completed lessons, rounded to the nearest whole number. */
    fun percent(completed: Int, total: Int): Int {
        if (total <= 0) return 0
        return (completed.coerceIn(0, total) * 100.0 / total).roundToInt()
    }
}
