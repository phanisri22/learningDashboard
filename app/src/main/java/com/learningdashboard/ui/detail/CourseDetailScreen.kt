package com.learningdashboard.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learningdashboard.R
import com.learningdashboard.domain.model.CourseDetail
import com.learningdashboard.domain.model.Lesson
import com.learningdashboard.ui.common.FullScreenLoading
import com.learningdashboard.ui.common.FullScreenMessage
import com.learningdashboard.ui.common.toMessage
import com.learningdashboard.ui.theme.CompletedGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    onBack: () -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text((state as? CourseDetailUiState.Content)?.detail?.course?.title.orEmpty())
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (val s = state) {
                CourseDetailUiState.Loading -> FullScreenLoading()

                is CourseDetailUiState.Error -> FullScreenMessage(
                    title = stringResource(R.string.course_load_failed_title),
                    message = s.error.toMessage(),
                    actionLabel = stringResource(R.string.retry),
                    onAction = viewModel::refresh,
                )

                is CourseDetailUiState.Content -> CourseDetailContent(
                    state = s,
                    onMarkCompleted = viewModel::markLessonCompleted,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun CourseDetailContent(
    state: CourseDetailUiState.Content,
    onMarkCompleted: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val detail = state.detail
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { ProgressHeader(detail) }
        item {
            Text(
                stringResource(R.string.lessons_header),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (detail.lessons.isEmpty()) {
            item {
                when {
                    state.isRefreshing -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.refreshError != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.lessons_unavailable))
                        Text(
                            state.refreshError.toMessage(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                    else -> Text(stringResource(R.string.no_lessons))
                }
            }
        } else {
            items(detail.lessons, key = { it.id }) { lesson ->
                LessonRow(lesson = lesson, onMarkCompleted = { onMarkCompleted(lesson.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProgressHeader(detail: CourseDetail) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(detail.course.title, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.instructor_by, detail.course.instructor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { detail.course.progress / 100f },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.progress_percent, detail.course.progress),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (detail.lessons.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.lessons_completed, detail.completedCount, detail.lessons.size),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, onMarkCompleted: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Icon(
            imageVector = if (lesson.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (lesson.isCompleted) CompletedGreen else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (lesson.isCompleted) R.string.status_completed else R.string.status_pending),
                style = MaterialTheme.typography.bodySmall,
                color = if (lesson.isCompleted) CompletedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lesson.isPendingSync) {
                Text(
                    stringResource(R.string.pending_sync),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!lesson.isCompleted) {
            OutlinedButton(onClick = onMarkCompleted) { Text(stringResource(R.string.mark_complete)) }
        }
    }
}
