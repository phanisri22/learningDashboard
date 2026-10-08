package com.learningdashboard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.learningdashboard.ui.dashboard.DashboardScreen
import com.learningdashboard.ui.detail.CourseDetailScreen
import com.learningdashboard.ui.login.LoginScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

@Serializable
data object DashboardRoute

@Serializable
data class CourseDetailRoute(val courseId: Int)

@Composable
fun AppNavHost(startDestination: Any, isLoggedIn: Flow<Boolean>) {
    val navController = rememberNavController()

    // Any end of the session (logout or an expired token) returns to login with a cleared back stack.
    LaunchedEffect(navController) {
        isLoggedIn.collect { loggedIn ->
            val onLogin = navController.currentDestination?.hasRoute<LoginRoute>() == true
            if (!loggedIn && !onLogin) {
                navController.navigate(LoginRoute) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<LoginRoute> {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(DashboardRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<DashboardRoute> {
            DashboardScreen(
                onCourseClick = { courseId -> navController.navigate(CourseDetailRoute(courseId)) },
            )
        }
        composable<CourseDetailRoute> {
            CourseDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
