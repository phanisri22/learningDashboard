package com.learningdashboard.ui.dashboard

import com.learningdashboard.core.network.ConnectivityObserver
import com.learningdashboard.domain.model.AppError
import com.learningdashboard.domain.model.Course
import com.learningdashboard.domain.model.CourseDetail
import com.learningdashboard.domain.repository.AuthRepository
import com.learningdashboard.domain.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeCourseRepository()
    private val connectivity = FakeConnectivityObserver()
    private val authRepository = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cached courses stay visible when refresh fails offline`() = runTest(dispatcher) {
        repository.courses.value = listOf(pythonCourse)
        repository.refreshResult = Result.failure(AppError.Network())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(listOf(pythonCourse), state.courses)
        assertTrue(state.refreshError is AppError.Network)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `failed refresh with no cache shows error state`() = runTest(dispatcher) {
        repository.refreshResult = Result.failure(AppError.Network())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as DashboardUiState.Error
        assertTrue(state.error is AppError.Network)
    }

    @Test
    fun `successful refresh with no courses shows empty state`() = runTest(dispatcher) {
        repository.refreshResult = Result.success(0)

        val viewModel = createViewModel()

        assertEquals(DashboardUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `retry after failure shows courses written by the repository`() = runTest(dispatcher) {
        repository.refreshResult = Result.failure(AppError.Network())
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value is DashboardUiState.Error)

        repository.refreshResult = Result.success(1)
        repository.onRefresh = { repository.courses.value = listOf(pythonCourse) }
        viewModel.refresh()

        val state = viewModel.uiState.value as DashboardUiState.Content
        assertEquals(listOf(pythonCourse), state.courses)
        assertEquals(null, state.refreshError)
    }

    @Test
    fun `logout with unsynced completions asks for confirmation first`() = runTest(dispatcher) {
        repository.unsyncedCompletions = 2
        val viewModel = createViewModel()

        viewModel.logout()

        assertEquals(LogoutState.ConfirmUnsynced(unsyncedCount = 2), viewModel.logoutState.value)
        assertEquals(false, authRepository.loggedOut)

        viewModel.confirmLogout()

        assertEquals(LogoutState.LoggedOut, viewModel.logoutState.value)
        assertEquals(true, authRepository.loggedOut)
    }

    @Test
    fun `logout goes straight through when everything is synced`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.logout()

        assertEquals(LogoutState.LoggedOut, viewModel.logoutState.value)
        assertEquals(true, authRepository.loggedOut)
    }

    private fun TestScope.createViewModel(): DashboardViewModel {
        val viewModel = DashboardViewModel(repository, authRepository, connectivity)
        // stateIn(WhileSubscribed) only runs while collected, like the UI does.
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private val pythonCourse = Course(
        id = 1,
        title = "Python Programming",
        instructor = "John Smith",
        progress = 65,
        lessonCount = 20,
    )
}

private class FakeCourseRepository : CourseRepository {
    val courses = MutableStateFlow<List<Course>>(emptyList())
    var refreshResult: Result<Int> = Result.success(0)
    var onRefresh: () -> Unit = {}
    var unsyncedCompletions = 0

    override fun observeCourses(): Flow<List<Course>> = courses
    override fun observeCourseDetail(courseId: Int): Flow<CourseDetail?> = flowOf(null)

    override suspend fun refreshCourses(): Result<Int> {
        onRefresh()
        return refreshResult
    }

    override suspend fun refreshCourseDetail(courseId: Int): Result<Int> = Result.success(0)
    override suspend fun markLessonCompleted(courseId: Int, lessonId: Int) = Unit
    override suspend fun syncPendingCompletions(): Int = unsyncedCompletions
}

private class FakeAuthRepository : AuthRepository {
    var loggedOut = false

    override fun isLoggedIn() = !loggedOut
    override fun observeIsLoggedIn(): Flow<Boolean> = flowOf(!loggedOut)
    override fun consumeSessionExpired() = false
    override suspend fun login(email: String, password: String) = Result.success(Unit)
    override suspend fun logout() {
        loggedOut = true
    }
}

private class FakeConnectivityObserver : ConnectivityObserver {
    val online = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = online
    override fun isCurrentlyOnline() = online.value
}
