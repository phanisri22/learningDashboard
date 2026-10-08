package com.learningdashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.learningdashboard.domain.repository.AuthRepository
import com.learningdashboard.ui.navigation.AppNavHost
import com.learningdashboard.ui.navigation.DashboardRoute
import com.learningdashboard.ui.navigation.LoginRoute
import com.learningdashboard.ui.theme.LearningDashboardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A saved session skips login, so cached courses are reachable offline after a restart.
        val startDestination: Any = if (authRepository.isLoggedIn()) DashboardRoute else LoginRoute
        val isLoggedIn = authRepository.observeIsLoggedIn()
        setContent {
            LearningDashboardTheme {
                AppNavHost(startDestination = startDestination, isLoggedIn = isLoggedIn)
            }
        }
    }
}
