package com.learningdashboard.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learningdashboard.domain.model.AppError
import com.learningdashboard.domain.model.asAppError
import com.learningdashboard.domain.repository.AuthRepository
import com.learningdashboard.domain.validation.CredentialsValidator
import com.learningdashboard.domain.validation.EmailError
import com.learningdashboard.domain.validation.PasswordError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: EmailError? = null,
    val passwordError: PasswordError? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(error = if (authRepository.consumeSessionExpired()) AppError.Unauthorized() else null),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, error = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = CredentialsValidator.validateEmail(state.email)
        val passwordError = CredentialsValidator.validatePassword(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(state.email.trim(), state.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isLoggedIn = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }
}
