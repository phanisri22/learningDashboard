package com.learningdashboard.ui.common

import com.learningdashboard.domain.model.AppError

/** State of the latest network refresh; combined with cached data to build screen state. */
sealed interface RefreshStatus {
    data object InProgress : RefreshStatus
    data class Success(val fetchedCount: Int) : RefreshStatus
    data class Failed(val error: AppError) : RefreshStatus
}
