package com.learningdashboard.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.learningdashboard.domain.model.AppError
import com.learningdashboard.domain.model.CourseDetail
import com.learningdashboard.domain.model.asAppError
import com.learningdashboard.domain.repository.CourseRepository
import com.learningdashboard.ui.common.RefreshStatus
import com.learningdashboard.ui.navigation.CourseDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState
    data class Error(val error: AppError) : CourseDetailUiState
    data class Content(
        val detail: CourseDetail,
        val isRefreshing: Boolean,
        val refreshError: AppError?,
    ) : CourseDetailUiState
}

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val courseRepository: CourseRepository,
) : ViewModel() {

    private val courseId = savedStateHandle.toRoute<CourseDetailRoute>().courseId
    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.InProgress)
    private var refreshJob: Job? = null

    val uiState: StateFlow<CourseDetailUiState> =
        combine(courseRepository.observeCourseDetail(courseId), refreshStatus) { detail, status ->
            reduce(detail, status)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDetailUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshStatus.value = RefreshStatus.InProgress
            refreshStatus.value = courseRepository.refreshCourseDetail(courseId).fold(
                onSuccess = { RefreshStatus.Success(it) },
                onFailure = { RefreshStatus.Failed(it.asAppError()) },
            )
        }
    }

    fun markLessonCompleted(lessonId: Int) {
        viewModelScope.launch {
            courseRepository.markLessonCompleted(courseId, lessonId)
        }
    }

    private fun reduce(detail: CourseDetail?, status: RefreshStatus): CourseDetailUiState = when {
        detail == null -> when (status) {
            RefreshStatus.InProgress -> CourseDetailUiState.Loading
            is RefreshStatus.Failed -> CourseDetailUiState.Error(status.error)
            is RefreshStatus.Success -> CourseDetailUiState.Error(AppError.NotFound())
        }
        else -> CourseDetailUiState.Content(
            detail = detail,
            isRefreshing = status is RefreshStatus.InProgress ||
                // Lessons are written but the database hasn't emitted them yet.
                (status is RefreshStatus.Success && status.fetchedCount > 0 && detail.lessons.isEmpty()),
            refreshError = (status as? RefreshStatus.Failed)?.error,
        )
    }
}
