package com.learningdashboard.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningdashboard.R
import com.learningdashboard.domain.model.Course
import com.learningdashboard.ui.common.FullScreenLoading
import com.learningdashboard.ui.common.FullScreenMessage
import com.learningdashboard.ui.common.InfoBanner
import com.learningdashboard.ui.common.toMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCourseClick: (Int) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val logoutState by viewModel.logoutState.collectAsStateWithLifecycle()

    (logoutState as? LogoutState.ConfirmUnsynced)?.let { state ->
        UnsyncedLogoutDialog(
            unsyncedCount = state.unsyncedCount,
            onConfirm = viewModel::confirmLogout,
            onDismiss = viewModel::cancelLogout,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = viewModel::logout, enabled = logoutState == LogoutState.Idle) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (isOffline) InfoBanner(stringResource(R.string.offline_banner))

            when (val s = state) {
                DashboardUiState.Loading -> FullScreenLoading()

                DashboardUiState.Empty -> FullScreenMessage(
                    title = stringResource(R.string.empty_courses_title),
                    message = stringResource(R.string.empty_courses_message),
                    actionLabel = stringResource(R.string.retry),
                    onAction = viewModel::refresh,
                )

                is DashboardUiState.Error -> FullScreenMessage(
                    title = stringResource(R.string.load_failed_title),
                    message = s.error.toMessage(),
                    actionLabel = stringResource(R.string.retry),
                    onAction = viewModel::refresh,
                )

                is DashboardUiState.Content -> PullToRefreshBox(
                    isRefreshing = s.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CourseList(
                        courses = s.courses,
                        showRefreshError = s.refreshError != null && !isOffline,
                        onCourseClick = onCourseClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnsyncedLogoutDialog(unsyncedCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.unsynced_logout_title)) },
        text = {
            Text(pluralStringResource(R.plurals.unsynced_logout_message, unsyncedCount, unsyncedCount))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.logout_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.stay_logged_in)) }
        },
    )
}

@Composable
private fun CourseList(
    courses: List<Course>,
    showRefreshError: Boolean,
    onCourseClick: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (showRefreshError) {
            item { InfoBanner(stringResource(R.string.refresh_failed_cached)) }
        }
        items(courses, key = { it.id }) { course ->
            CourseCard(course = course, onClick = { onCourseClick(course.id) })
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(course.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.instructor_by, course.instructor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { course.progress / 100f },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.progress_percent, course.progress), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.lessons_count, course.lessonCount),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onClick) { Text(stringResource(R.string.continue_course)) }
            }
        }
    }
}
