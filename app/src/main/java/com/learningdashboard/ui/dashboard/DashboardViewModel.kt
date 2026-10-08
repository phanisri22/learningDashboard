package com.learningdashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningdashboard.core.network.ConnectivityObserver
import com.learningdashboard.domain.model.AppError
import com.learningdashboard.domain.model.Course
import com.learningdashboard.domain.model.asAppError
import com.learningdashboard.domain.repository.AuthRepository
import com.learningdashboard.domain.repository.CourseRepository
import com.learningdashboard.ui.common.RefreshStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Error(val error: AppError) : DashboardUiState
    data class Content(
        val courses: List<Course>,
        val isRefreshing: Boolean,
        /** Set when the last refresh failed and cached courses are shown instead. */
        val refreshError: AppError?,
    ) : DashboardUiState
}

sealed interface LogoutState {
    data object Idle : LogoutState
    data object InProgress : LogoutState

    /** Some completions couldn't be synced; the user decides whether to log out anyway. */
    data class ConfirmUnsynced(val unsyncedCount: Int) : LogoutState
    data object LoggedOut : LogoutState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.InProgress)
    private var refreshJob: Job? = null

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    val uiState: StateFlow<DashboardUiState> =
        combine(courseRepository.observeCourses(), refreshStatus) { courses, status -> reduce(courses, status) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    val isOffline: StateFlow<Boolean> = connectivityObserver.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        refresh()
        // Pick up fresh data automatically when the connection comes back.
        viewModelScope.launch {
            connectivityObserver.isOnline.drop(1).filter { it }.collect { refresh() }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshStatus.value = RefreshStatus.InProgress
            refreshStatus.value = courseRepository.refreshCourses().fold(
                onSuccess = { RefreshStatus.Success(it) },
                onFailure = { RefreshStatus.Failed(it.asAppError()) },
            )
        }
    }

    /** Tries to sync offline completions first and asks for confirmation if some are still pending. */
    fun logout() {
        if (_logoutState.value != LogoutState.Idle) return
        _logoutState.value = LogoutState.InProgress
        viewModelScope.launch {
            val unsynced = courseRepository.syncPendingCompletions()
            if (unsynced > 0) {
                _logoutState.value = LogoutState.ConfirmUnsynced(unsynced)
            } else {
                completeLogout()
            }
        }
    }

    fun confirmLogout() {
        viewModelScope.launch { completeLogout() }
    }

    fun cancelLogout() {
        _logoutState.value = LogoutState.Idle
    }

    private suspend fun completeLogout() {
        authRepository.logout()
        _logoutState.value = LogoutState.LoggedOut
    }

    private fun reduce(courses: List<Course>, status: RefreshStatus): DashboardUiState = when {
        courses.isNotEmpty() -> DashboardUiState.Content(
            courses = courses,
            isRefreshing = status is RefreshStatus.InProgress,
            refreshError = (status as? RefreshStatus.Failed)?.error,
        )
        status is RefreshStatus.InProgress -> DashboardUiState.Loading
        status is RefreshStatus.Failed -> DashboardUiState.Error(status.error)
        // Fetched courses are written but the database hasn't emitted them yet.
        status is RefreshStatus.Success && status.fetchedCount > 0 -> DashboardUiState.Loading
        else -> DashboardUiState.Empty
    }
}
